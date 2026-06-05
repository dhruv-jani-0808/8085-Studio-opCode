package studio.ide.ui;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class EditorView {
    @FXML private TextArea mainCodeEditor;
    private File currentOpenFile = null;

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
