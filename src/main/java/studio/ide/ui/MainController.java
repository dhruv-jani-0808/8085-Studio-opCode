package studio.ide.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import studio.ide.assembler.Assembler;
import studio.ide.emulator.CPU;
import studio.ide.emulator.Memory;
import javafx.scene.control.Alert;

import javax.xml.transform.Templates;

public class MainController {

    @FXML private Button executeBtn;
    @FXML private Button stepBtn;

    @FXML private Button fileButton;
    @FXML private javafx.scene.control.ContextMenu fileContextMenu;
    @FXML private javafx.scene.control.MenuItem newFileMenuItem;
    @FXML private javafx.scene.control.MenuItem openFileMenuItem;
    @FXML private javafx.scene.control.MenuItem saveMenuItem;
    @FXML private javafx.scene.control.MenuItem saveAsMenuItem;

    @FXML private SidebarView sidebarController;
    @FXML private EditorView editorController;
    @FXML private MemoryTableView memoryTableController;

    private final Memory memory = new Memory();
    private final CPU cpu = new CPU(memory);
    private final Assembler assembler = new Assembler();
    private Thread runThread = null;
    private volatile boolean isRunning = false;
    private boolean isLoaded = false;

    @FXML
    public void initialize() {
        memoryTableController.initializeMemoryTable(memory);

        sidebarController.updateRegisters(cpu);

        // Open the dropdown ContextMenu on hover
        fileButton.setOnMouseEntered(event -> {
            fileContextMenu.show(fileButton, javafx.geometry.Side.BOTTOM, 0, 0);
        });

        newFileMenuItem.setOnAction(event -> editorController.handleNewFile());
        openFileMenuItem.setOnAction(event -> editorController.handleOpenFile());
        saveMenuItem.setOnAction(event -> editorController.handleSaveFile());
        saveAsMenuItem.setOnAction(event -> editorController.handleSaveAsFile());

        // Sync editor opened file with sidebar folder view
        editorController.setOnFileOpened(file -> {
            sidebarController.openDirectory(file.getParentFile());
            sidebarController.toggleView(false); // Switch sidebar to Files list
        });

        // Double-click file in sidebar to load it in editor
        sidebarController.setOnFileSelected(file -> {
            editorController.loadFile(file);
        });

        executeBtn.setOnAction(event -> handleExecute());
        stepBtn.setOnAction(event -> handleStep());
    }

    private void handleExecute() {
        if(isRunning) {
            stopExecution();
            return;
        }

        if(!compileAndLoad()) return;

        isRunning = true;
        executeBtn.setText("Stop Code");
        stepBtn.setDisable(true);

        runThread = new Thread(() -> {
            int safetyCounter = 0;

            while(isRunning) {
                int currentPC = cpu.pc;
                int opcode = cpu.readMemory(currentPC);

                if(opcode == 0x76) {
                    Platform.runLater(() -> showInfo("Execution Complete", "CPU reached HALT (0x76)."));
                    break;
                }

                cpu.step();

                safetyCounter++;
                if(safetyCounter > 2000) {
                    Platform.runLater(() -> showInfo("Safety Limit Reached", "Execution aborted: exceeded threshold of 2000 instructions."));
                    break;
                }
            }

            Platform.runLater(() -> {
                isRunning = false;
                executeBtn.setText("Run Code (F5);");
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
        if(runThread != null) {
            runThread.interrupt();
        }
    }

    private void handleStep() {
        if(isRunning) return;

        int currentPC = cpu.pc;
        int opcode = cpu.readMemory(currentPC);

        if(opcode == 0x76) {
            showInfo("Halted", "CPU is in HALT state(0x76). Write new code or run/step from the beginning to reset.");
            return;
        }

        if(!isLoaded) {
            if(!compileAndLoad()) return;
        }

        cpu.step();

        sidebarController.updateRegisters(cpu);
        memoryTableController.refreshMemoryGrid(memory);

        if(cpu.readMemory(cpu.pc) == 0x76) {
            showInfo("Halted", "CPU executed HLT instruction.");
        }
    }

    private boolean compileAndLoad() {
        String sourceCode = editorController.getCodeText();
        if(sourceCode == null || sourceCode.trim().isEmpty()) {
            showError("Compilation Error", "No source code found in the editor to assemble.");
            return false;
        }

        try {
            byte[] machineCode = assembler.assemble(sourceCode);
            if(machineCode == null || machineCode.length == 0) {
                showError("Compilation Error", "Compilation produced 0 bytes.");
                return false;
            }

            for(int i = 0; i < machineCode.length; i++) {
                memory.write(i, machineCode[i]);
            }

            for(int i = machineCode.length; i < 65536; i++) {
                memory.write(i, (byte) 0x00);
            }

            cpu.a = 0; cpu.b = 0; cpu.c = 0; cpu.d = 0; cpu.e = 0; cpu.h = 0; cpu.l = 0;
            cpu.pc = 0x0000;
            cpu.sp = 0xFFFF;
            cpu.flagZ = false;
            cpu.flagS = false;
            cpu.flagCY = false;
            cpu.flagP = false;
            cpu.flagAC = false;

            sidebarController.updateRegisters(cpu);
            memoryTableController.refreshMemoryGrid(memory);

            isLoaded = true;

            return true;
        }
        catch(Exception e) {
            showError("Compilation Error", "Failed to compile assembler source:\n" + e.getMessage());
            return false;
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}