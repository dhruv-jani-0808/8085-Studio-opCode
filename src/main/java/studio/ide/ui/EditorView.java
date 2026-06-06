package studio.ide.ui;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javafx.application.Platform;
import javafx.scene.control.ScrollBar;


public class EditorView {
    @FXML private TextArea mainCodeEditor;
    @FXML private TextArea lineNumbersArea; // Add this line
    private File currentOpenFile = null;
    @FXML
    public void initialize() {
        // Update line numbers dynamically as the user types
        mainCodeEditor.textProperty().addListener((observable, oldValue, newValue) -> {
            updateLineNumbers();
        });
        updateLineNumbers();
        // Synchronize line-number scrolling with the main code editor
        mainCodeEditor.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> {
                    ScrollBar scrollMain = (ScrollBar) mainCodeEditor.lookup(".scroll-bar:vertical");
                    ScrollBar scrollLines = (ScrollBar) lineNumbersArea.lookup(".scroll-bar:vertical");
                    if (scrollMain != null && scrollLines != null) {
                        scrollLines.valueProperty().bind(scrollMain.valueProperty());

                        // Hide the scrollbar of the line numbers panel entirely
                        scrollLines.setPrefWidth(0);
                        scrollLines.setVisible(false);
                        scrollLines.setDisable(true);
                    }
                });
            }
        });
    }
    private void updateLineNumbers() {
        String text = mainCodeEditor.getText();
        int lineCount = text.split("\n", -1).length;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lineCount; i++) {
            sb.append(i).append("\n");
        }
        lineNumbersArea.setText(sb.toString());
    }

    /**
     * Simple getter so MainController can grab the source string to feed into the compiler.
     */
    public String getCodeText() {
        return mainCodeEditor.getText();
    }

    /**
     * Opens a native OS dialog file browser to read an assembly text document into the pane.
     */
    @FXML
    public void handleOpenFile() {

        Stage stage = (Stage) mainCodeEditor.getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open 8085 Assembly File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Assembly Files (*.asm, *.txt)", "*.asm", "*.txt")
        );

        File selectedFile = fileChooser.showOpenDialog(stage);
        if(selectedFile != null) {
            try {
                String content = Files.readString(selectedFile.toPath());
                mainCodeEditor.setText(content);
                currentOpenFile = selectedFile;
            }
            catch (IOException e) {
                showError("File Error", "Failed to access file:\n" + e.getMessage());
            }
        }
    }

    /**
     * Saves the current text buffer out to the active file, or opens a "Save As" menu if new.
     */
    @FXML
    public void handleSaveFile() {
        if(currentOpenFile != null) {
            try {
                Files.writeString(currentOpenFile.toPath(), mainCodeEditor.getText());
            }
            catch (IOException e) {
                showError("File Error", "Failed to access file:\n" + e.getMessage());
            }
        }
        else {
            handleSaveAsFile();
        }
    }

    @FXML
    public void handleSaveAsFile() {

        Stage stage = (Stage) mainCodeEditor.getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Assembly File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Assembly Files (*.asm)", "*.asm"));

        File file = fileChooser.showSaveDialog(stage);
        if(file != null) {
            try {
                Files.writeString(file.toPath(), mainCodeEditor.getText());
                currentOpenFile = file;
            }
            catch (IOException e) {
                showError("File Error", "Failed to access file:\n" + e.getMessage());
            }
        }
    }

    // A quick helper method to display GUI error popups
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
