package studio.ide.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import studio.ide.assembler.Assembler;
import studio.ide.emulator.CPU;
import studio.ide.emulator.Memory;

public class MainController {

    // ─── Toolbar buttons ──────────────────────────────────────────────────────
    @FXML private Button executeBtn;
    @FXML private Button stepBtn;

    // ─── File menu ────────────────────────────────────────────────────────────
    @FXML private Button      fileButton;
    @FXML private ContextMenu fileContextMenu;
    @FXML private MenuItem    newFileMenuItem;
    @FXML private MenuItem    openFileMenuItem;
    @FXML private MenuItem    saveMenuItem;
    @FXML private MenuItem    saveAsMenuItem;

    // ─── Activity Bar (in v2_main_layout) ────────────────────────────────────
    @FXML private VBox   activityBarBox;
    @FXML private Button activityFilesBtn;
    @FXML private Button activityRegsBtn;

    // ─── Sub-controllers (injected by fx:include) ────────────────────────────
    @FXML private SidebarView     sidebarController;
    @FXML private EditorView      editorController;
    @FXML private MemoryTableView memoryTableController;

    // ─── Back-end state ───────────────────────────────────────────────────────
    private final Memory   memory   = new Memory();
    private final CPU      cpu      = new CPU(memory);
    private final Assembler assembler = new Assembler();
    private Thread  runThread = null;
    private volatile boolean isRunning = false;
    private boolean isLoaded = false;

    /** Address → 1-based source line number, refreshed on every compile. */
    private java.util.Map<Integer, Integer> addressToLine = new java.util.HashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    // Initialization
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        memoryTableController.initializeMemoryTable(memory);
        sidebarController.updateRegisters(cpu);

        // ── Activity bar ──────────────────────────────────────────────────────
        setActivityActive(activityFilesBtn);    // Files tab active by default

        activityFilesBtn.setOnAction(e -> {
            sidebarController.toggleView(false);
            setActivityActive(activityFilesBtn);
        });
        activityRegsBtn.setOnAction(e -> {
            sidebarController.toggleView(true);
            setActivityActive(activityRegsBtn);
        });

        // Default sidebar view: show files panel (no files loaded yet)
        sidebarController.toggleView(false);

        // ── File menu (hover to open) ─────────────────────────────────────────
        fileButton.setOnMouseEntered(event ->
                fileContextMenu.show(fileButton, javafx.geometry.Side.BOTTOM, 0, 0));

        newFileMenuItem.setOnAction(e  -> editorController.handleNewFile());
        openFileMenuItem.setOnAction(e -> editorController.handleOpenFile());
        saveMenuItem.setOnAction(e     -> editorController.handleSaveFile());
        saveAsMenuItem.setOnAction(e   -> editorController.handleSaveAsFile());

        // ── File open / sidebar sync ──────────────────────────────────────────
        editorController.setOnFileOpened(file -> {
            if (file != null && file.getParentFile() != null) {
                sidebarController.openDirectory(file.getParentFile());
                sidebarController.toggleView(false);
                setActivityActive(activityFilesBtn);
            }
        });

        // Double-clicking a file in the tree loads it in the editor
        sidebarController.setOnFileSelected(editorController::loadFile);

        // ── Run / Step ────────────────────────────────────────────────────────
        executeBtn.setOnAction(e -> handleExecute());
        stepBtn.setOnAction(e    -> handleStep());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Activity bar visual feedback
    // ─────────────────────────────────────────────────────────────────────────

    /** Marks one activity-bar button as active (white + left blue border), others as dim. */
    private void setActivityActive(Button active) {
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: #858585; "
                + "-fx-font-size: 20px; -fx-cursor: hand; "
                + "-fx-min-width: 48; -fx-min-height: 48; "
                + "-fx-border-color: transparent; -fx-border-width: 0 0 0 2; -fx-background-radius: 0;";
        String activeStyle = "-fx-background-color: transparent; -fx-text-fill: #cccccc; "
                + "-fx-font-size: 20px; -fx-cursor: hand; "
                + "-fx-min-width: 48; -fx-min-height: 48; "
                + "-fx-border-color: #007acc; -fx-border-width: 0 0 0 2; -fx-background-radius: 0;";

        activityFilesBtn.setStyle(activityFilesBtn == active ? activeStyle : inactiveStyle);
        activityRegsBtn.setStyle(activityRegsBtn  == active ? activeStyle : inactiveStyle);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Execution logic (unchanged from previous version)
    // ─────────────────────────────────────────────────────────────────────────

    private void handleExecute() {
        if (isRunning) { stopExecution(); return; }
        if (!compileAndLoad()) return;

        isRunning = true;
        executeBtn.setText("⏹  Stop");
        stepBtn.setDisable(true);

        runThread = new Thread(() -> {
            int safetyCounter = 0;
            while (isRunning) {
                int opcode = cpu.readMemory(cpu.pc);
                if (opcode == 0x76) {
                    Platform.runLater(() -> showInfo("Execution Complete", "CPU reached HALT (0x76)."));
                    break;
                }
                cpu.step();
                if (++safetyCounter > 2000) {
                    Platform.runLater(() -> showInfo("Safety Limit", "Aborted: exceeded 2 000 instructions."));
                    break;
                }
            }
            Platform.runLater(() -> {
                isRunning = false;
                isLoaded = false;
                executeBtn.setText("▶  Run (F5)");
                stepBtn.setDisable(false);
                sidebarController.updateRegisters(cpu);
                memoryTableController.refreshMemoryGrid(memory);
            });
        });
        runThread.setDaemon(true);
        runThread.start();
    }

    private void stopExecution() {
        isRunning = false;
        if (runThread != null) runThread.interrupt();
    }

    private void handleStep() {
        if (isRunning) return;

        // Compile first if not loaded yet (e.g. first step, or called after Run completed)
        if (!isLoaded && !compileAndLoad()) return;

        // Now read from the fresh / current PC
        int opcode = cpu.readMemory(cpu.pc);
        if (opcode == 0x76) {
            showInfo("Halted", "CPU is in HALT state. Press Run or Step again to recompile.");
            isLoaded = false; // allow next Step to recompile
            return;
        }

        // Highlight the source line that is about to execute
        Integer lineNum = addressToLine.get(cpu.pc);
        if (lineNum != null) {
            editorController.highlightLine(lineNum);
        }

        cpu.step();

        sidebarController.updateRegisters(cpu);
        memoryTableController.refreshMemoryGrid(memory);

        // Switch activity bar to registers panel so user can see updated values
        sidebarController.toggleView(true);
        setActivityActive(activityRegsBtn);

        if (cpu.readMemory(cpu.pc) == 0x76) {
            showInfo("Halted", "CPU executed HLT instruction.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Compile & load
    // ─────────────────────────────────────────────────────────────────────────

    private boolean compileAndLoad() {
        String source = editorController.getCodeText();
        if (source == null || source.isBlank()) {
            showError("Compilation Error", "No source code found in the editor.");
            return false;
        }
        try {
            byte[] machineCode = assembler.assemble(source);
            if (machineCode == null || machineCode.length == 0) {
                showError("Compilation Error", "Compilation produced 0 bytes.");
                return false;
            }
            for (int i = 0; i < machineCode.length; i++) memory.write(i, machineCode[i]);
            for (int i = machineCode.length; i < 65536; i++) memory.write(i, (byte) 0x00);

            cpu.a = 0; cpu.b = 0; cpu.c = 0; cpu.d = 0;
            cpu.e = 0; cpu.h = 0; cpu.l = 0;
            cpu.pc = 0x0000; cpu.sp = 0xFFFF;
            cpu.flagZ = false; cpu.flagS = false; cpu.flagCY = false;
            cpu.flagP = false; cpu.flagAC = false;

            sidebarController.updateRegisters(cpu);
            memoryTableController.refreshMemoryGrid(memory);
            isLoaded = true;

            // Capture the address → source line map for the step highlighter
            addressToLine = new java.util.HashMap<>(assembler.getAddressToLine());

            return true;
        } catch (Exception e) {
            showError("Compilation Error", "Failed to compile:\n" + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dialogs
    // ─────────────────────────────────────────────────────────────────────────

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}