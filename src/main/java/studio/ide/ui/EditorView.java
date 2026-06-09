package studio.ide.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;

public class EditorView {

    // ─── FXML fields ─────────────────────────────────────────────────────────
    @FXML private TextArea mainCodeEditor;
    @FXML private TextArea lineNumbersArea;
    @FXML private HBox     tabBar;
    @FXML private Label    statusFileLabel;
    @FXML private Label    statusLineLabel;

    // ─── Tab / multi-file state ───────────────────────────────────────────────
    /** Preserves insertion order so tabs appear in the order they were opened. */
    private final LinkedHashMap<String, String> openFilesContent = new LinkedHashMap<>();
    private final Map<String, File>   openFilesMap  = new HashMap<>();
    private final Map<String, HBox>   tabHBoxes     = new HashMap<>();
    private String  currentFilePath = null;
    private File    currentOpenFile = null;

    // ─── Callbacks ───────────────────────────────────────────────────────────
    private Consumer<File> onFileOpenedCallback = null;

    public void setOnFileOpened(Consumer<File> callback) {
        this.onFileOpenedCallback = callback;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Initialization
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        // Keep line numbers in sync with typing
        mainCodeEditor.textProperty().addListener((obs, oldVal, newVal) -> updateLineNumbers());
        updateLineNumbers();

        // Status bar: update line/col as caret moves
        mainCodeEditor.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
            int pos = newPos.intValue();
            String text = mainCodeEditor.getText();
            int line = 1, col = 1;
            for (int i = 0; i < pos && i < text.length(); i++) {
                if (text.charAt(i) == '\n') { line++; col = 1; }
                else { col++; }
            }
            int finalLine = line, finalCol = col;
            Platform.runLater(() -> statusLineLabel.setText("Ln " + finalLine + ", Col " + finalCol));
        });

        // Sync scroll bars (line numbers follow editor scroll)
        mainCodeEditor.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> {
                    ScrollBar scrollMain  = (ScrollBar) mainCodeEditor.lookup(".scroll-bar:vertical");
                    ScrollBar scrollLines = (ScrollBar) lineNumbersArea.lookup(".scroll-bar:vertical");
                    if (scrollMain != null && scrollLines != null) {
                        scrollLines.valueProperty().bind(scrollMain.valueProperty());
                        scrollLines.setPrefWidth(0);
                        scrollLines.setVisible(false);
                        scrollLines.setDisable(true);
                    }
                });
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tab management
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Opens a file in a new tab (or switches to it if already open).
     * This is the canonical entry-point used by MainController / SidebarView.
     */
    public void loadFile(File file) {
        if (file == null) return;
        String path = file.getAbsolutePath();

        if (openFilesMap.containsKey(path)) {
            // Already open → just switch to it
            switchToTab(path);
            return;
        }

        try {
            String content = Files.readString(file.toPath());
            openFilesContent.put(path, content);
            openFilesMap.put(path, file);
            addTab(file.getName(), path);
            switchToTab(path);
            if (onFileOpenedCallback != null) {
                onFileOpenedCallback.accept(file);
            }
        } catch (IOException e) {
            showError("File Error", "Failed to open file:\n" + e.getMessage());
        }
    }

    /** Creates and appends a tab button to the tab bar. */
    private void addTab(String filename, String filePath) {
        HBox tab = new HBox(0);
        tab.setStyle("-fx-background-color: #2d2d2d; -fx-padding: 0; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;");
        tab.setMinHeight(35); tab.setMaxHeight(35);

        Label nameLabel = new Label("  " + filename + "  ");
        nameLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 15px; -fx-padding: 8 0 8 0;");

        Button closeBtn = new Button("×");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #666666; "
                + "-fx-font-size: 14px; -fx-padding: 4 8 4 0; -fx-cursor: hand;");
        closeBtn.setFocusTraversable(false);
        closeBtn.setOnAction(e -> {
            closeTab(filePath);
            e.consume();
        });
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #cccccc; "
                        + "-fx-font-size: 14px; -fx-padding: 4 8 4 0; -fx-cursor: hand;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #666666; "
                        + "-fx-font-size: 14px; -fx-padding: 4 8 4 0; -fx-cursor: hand;"));

        // Left accent border (2px) – hidden until tab is active
        Pane accentBorder = new Pane();
        accentBorder.setPrefWidth(2);
        accentBorder.setStyle("-fx-background-color: transparent;");

        tab.getChildren().addAll(accentBorder, nameLabel, closeBtn);

        // Switching tabs on click
        tab.setOnMouseClicked(e -> switchToTab(filePath));

        // Hover effect
        tab.setOnMouseEntered(e -> {
            if (!filePath.equals(currentFilePath)) {
                tab.setStyle("-fx-background-color: #383838; -fx-padding: 0; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;");
            }
        });
        tab.setOnMouseExited(e -> {
            if (!filePath.equals(currentFilePath)) {
                tab.setStyle("-fx-background-color: #2d2d2d; -fx-padding: 0; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;");
            }
        });

        tabHBoxes.put(filePath, tab);
        tabBar.getChildren().add(tab);
    }

    /** Makes the given tab active – saves previous tab's content first. */
    private void switchToTab(String filePath) {
        // Persist the current editor content before leaving this tab
        if (currentFilePath != null) {
            openFilesContent.put(currentFilePath, mainCodeEditor.getText());
        }

        // Deactivate old tab styling
        if (currentFilePath != null && tabHBoxes.containsKey(currentFilePath)) {
            HBox oldTab = tabHBoxes.get(currentFilePath);
            oldTab.setStyle("-fx-background-color: #2d2d2d; -fx-padding: 0; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;");
            // Dim label and hide accent
            ((Label) oldTab.getChildren().get(1)).setStyle("-fx-text-fill: #888888; -fx-font-size: 15px; -fx-padding: 8 0 8 0;");
            ((Pane)  oldTab.getChildren().get(0)).setStyle("-fx-background-color: transparent;");
        }

        currentFilePath = filePath;
        currentOpenFile = openFilesMap.get(filePath);

        // Load content into editor
        mainCodeEditor.setText(openFilesContent.get(filePath));

        // Activate new tab styling
        HBox activeTab = tabHBoxes.get(filePath);
        if (activeTab != null) {
            activeTab.setStyle("-fx-background-color: #1e1e1e; -fx-padding: 0; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;");
            ((Label) activeTab.getChildren().get(1)).setStyle("-fx-text-fill: #cccccc; -fx-font-size: 15px; -fx-padding: 8 0 8 0;");
            ((Pane)  activeTab.getChildren().get(0)).setStyle("-fx-background-color: #007acc;");
        }

        // Update status bar
        statusFileLabel.setText(currentOpenFile != null ? currentOpenFile.getName() : "8085 Assembly");
    }

    /** Removes a tab and either switches to the next available or clears the editor. */
    private void closeTab(String filePath) {
        HBox tab = tabHBoxes.remove(filePath);
        if (tab != null) tabBar.getChildren().remove(tab);

        openFilesContent.remove(filePath);
        openFilesMap.remove(filePath);

        if (filePath.equals(currentFilePath)) {
            currentFilePath = null;
            currentOpenFile = null;
            if (!openFilesMap.isEmpty()) {
                // Switch to the last remaining tab
                String nextPath = openFilesMap.keySet().stream()
                        .reduce((first, second) -> second).orElse(null);
                if (nextPath != null) switchToTab(nextPath);
            } else {
                mainCodeEditor.clear();
                statusFileLabel.setText("8085 Assembly");
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Line highlighting (for Step execution)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Highlights the given 1-based line number in the editor by selecting it.
     * The VS Code-style blue selection colour (#264f78) is set in the FXML.
     * Called by MainController after each cpu.step().
     */
    public void highlightLine(int lineNumber) {
        String text = mainCodeEditor.getText();
        if (text == null || text.isEmpty()) return;

        String[] lines = text.split("\n", -1);
        if (lineNumber < 1 || lineNumber > lines.length) return;

        int start = 0;
        for (int i = 0; i < lineNumber - 1; i++) {
            start += lines[i].length() + 1;      // +1 for \n
        }
        int end = start + lines[lineNumber - 1].length();

        final int selStart = start;
        final int selEnd   = end;
        Platform.runLater(() -> {
            mainCodeEditor.requestFocus();
            mainCodeEditor.selectRange(selStart, selEnd);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // File menu actions
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    public void handleOpenFile() {
        Stage stage = (Stage) mainCodeEditor.getScene().getWindow();
        FileChooser fc = new FileChooser();
        fc.setTitle("Open Assembly File");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Assembly / Text files", "*.asm", "*.txt"),
                new FileChooser.ExtensionFilter("All files", "*.*")
        );
        File selected = fc.showOpenDialog(stage);
        if (selected != null) loadFile(selected);
    }

    @FXML
    public void handleSaveFile() {
        if (currentFilePath != null) {
            // Persist in-memory content map first
            openFilesContent.put(currentFilePath, mainCodeEditor.getText());
            try {
                Files.writeString(currentOpenFile.toPath(), mainCodeEditor.getText());
            } catch (IOException e) {
                showError("File Error", "Failed to save file:\n" + e.getMessage());
            }
        } else {
            handleSaveAsFile();
        }
    }

    @FXML
    public void handleSaveAsFile() {
        Stage stage = (Stage) mainCodeEditor.getScene().getWindow();
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Assembly File");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Assembly files (*.asm)", "*.asm"));
        File file = fc.showSaveDialog(stage);
        if (file != null) {
            try {
                Files.writeString(file.toPath(), mainCodeEditor.getText());
                // Register as a new tab if not already open
                if (!openFilesMap.containsKey(file.getAbsolutePath())) {
                    openFilesContent.put(file.getAbsolutePath(), mainCodeEditor.getText());
                    openFilesMap.put(file.getAbsolutePath(), file);
                    addTab(file.getName(), file.getAbsolutePath());
                }
                switchToTab(file.getAbsolutePath());
            } catch (IOException e) {
                showError("File Error", "Failed to save file:\n" + e.getMessage());
            }
        }
    }

    @FXML
    public void handleNewFile() {
        // Open an "Untitled" tab with blank content
        String uid = "untitled_" + System.currentTimeMillis();
        openFilesContent.put(uid, "");
        // We don't have a real File for untitled; use null in the map
        openFilesMap.put(uid, null);
        addTab("Untitled", uid);
        switchToTab(uid);
        currentOpenFile = null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public getters used by MainController
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns the current editor contents (synced from the active tab). */
    public String getCodeText() {
        return mainCodeEditor.getText();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void updateLineNumbers() {
        String text = mainCodeEditor.getText();
        int lineCount = text.split("\n", -1).length;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lineCount; i++) {
            sb.append(String.format("%3d", i)).append('\n');
        }
        lineNumbersArea.setText(sb.toString());
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }
}
