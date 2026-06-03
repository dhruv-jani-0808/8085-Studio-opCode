package studio.ide.ui;

import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class EditorView {
    private TextArea mainCodeEditor;
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
    public void handleOpenFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open 8085 Aseembly File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Aseembly Files (*.asm, *.txt", "*.asm", "*.txt")
        );

        File selectedFile = fileChooser.showOpenDialog(stage);
        if(selectedFile != null) {
            try {
                String content = Files.readString(selectedFile.toPath());
                mainCodeEditor.setText(content);
                currentOpenFile = selectedFile;
            }
            catch (IOException e) {
                System.err.println("Failed to read the file: " + e.getMessage());
            }
        }
    }

    /**
     * Saves the current text buffer out to the active file, or opens a "Save As" menu if new.
     */
    public void handleSaveFile(Stage stage) {
        if(currentOpenFile != null) {
            try {
                Files.writeString(currentOpenFile.toPath(), mainCodeEditor.getText());
            }
            catch (IOException e) {
                System.err.println("Failed to save file: " + e.getMessage());
            }
        }
        else {
            handleSaveAsFile(stage);
        }
    }

    public void handleSaveAsFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Assembly File");
        fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Assembly Files (*.asm)", "*.asm"));

        File file = fileChooser.showSaveDialog(stage);
        if(file != null) {
            try {
                Files.writeString(file.toPath(), mainCodeEditor.getText());
                currentOpenFile = file;
            }
            catch (IOException e) {
                System.err.println("Failed to write new file: " + e.getMessage());
            }
        }
    }
}
