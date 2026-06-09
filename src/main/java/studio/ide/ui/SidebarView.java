package studio.ide.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import studio.ide.emulator.CPU;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;

public class SidebarView {

    // ─── FXML labels (existing – must keep the same fx:id) ───────────────────
    @FXML private Label sidebarTitleLabel;
    @FXML private VBox fileExplorerPane;
    @FXML private ScrollPane registersScrollPane;      // wraps registersPane
    @FXML private VBox registersPane;
    @FXML private Label regALabel, regBLabel, regCLabel;
    @FXML private Label regDLabel, regELabel, regHLabel, regLLabel;
    @FXML private Label regPCLabel, regSPLabel;
    @FXML private Label flagZLabel, flagSLabel, flagCYLabel, flagPLabel, flagACLabel;

    // ─── File explorer FXML fields ────────────────────────────────────────────
    @FXML private Label   workingDirLabel;
    @FXML private TreeView<String> fileTreeView;

    // ─── Action-icon buttons in the header bar ────────────────────────────────
    @FXML private HBox   fileActionsBar;
    @FXML private Button newFileIconBtn;
    @FXML private Button newFolderIconBtn;
    @FXML private Button refreshIconBtn;

    // ─── Internal state ───────────────────────────────────────────────────────
    private File currentDirectory = null;
    /** Maps every TreeItem to its real File so we know what was clicked. */
    private final Map<TreeItem<String>, File> treeItemToFile = new HashMap<>();
    private Consumer<File> onFileSelectedCallback = null;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /** Called by MainController so clicking a file in the tree loads it in the editor. */
    public void setOnFileSelected(Consumer<File> callback) {
        this.onFileSelectedCallback = callback;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // File explorer – directory loading
    // ─────────────────────────────────────────────────────────────────────────

    /** Populates the tree view with the full file/folder hierarchy of the given directory. */
    public void openDirectory(File directory) {
        if (directory == null || !directory.isDirectory()) return;
        currentDirectory = directory;
        workingDirLabel.setText(directory.getName());

        treeItemToFile.clear();

        TreeItem<String> root = buildTreeItem(directory);
        root.setExpanded(true);

        fileTreeView.setRoot(root);
        fileTreeView.setShowRoot(true);

        // Double-click handler wired once here
        fileTreeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<String> selected = fileTreeView.getSelectionModel().getSelectedItem();
                if (selected == null) return;
                File clickedFile = treeItemToFile.get(selected);
                if (clickedFile == null || !clickedFile.isFile()) return;
                handleFileClicked(clickedFile);
            }
        });
    }

    /**
     * Recursively builds a TreeItem for a directory entry.
     * All files and folders are shown; only .asm and .txt can be opened.
     */
    private TreeItem<String> buildTreeItem(File file) {
        // Prefix icon: 📁 for dir, nothing for files (plain name)
        String label = file.isDirectory() ? "📁 " + file.getName() : file.getName();
        TreeItem<String> item = new TreeItem<>(label);
        treeItemToFile.put(item, file);

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                Arrays.sort(children, (a, b) -> {
                    // Folders first, then alphabetical
                    if (a.isDirectory() && !b.isDirectory()) return -1;
                    if (!a.isDirectory() && b.isDirectory()) return 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                });
                for (File child : children) {
                    item.getChildren().add(buildTreeItem(child));
                }
            }
        }
        return item;
    }

    /** Opens supported files; shows a friendly dialog for unsupported ones. */
    private void handleFileClicked(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".asm") || name.endsWith(".txt")) {
            if (onFileSelectedCallback != null) {
                onFileSelectedCallback.accept(file);
            }
        } else {
            showUnsupportedFileDialog(file.getName());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // File action icon handlers (New File, New Folder, Refresh)
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    public void handleNewFileInDir() {
        if (currentDirectory == null) {
            showInfo("No folder open", "Open a file first so the explorer knows which folder to use.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog("untitled.asm");
        dialog.setTitle("New File");
        dialog.setHeaderText("Create new file in: " + currentDirectory.getName());
        dialog.setContentText("File name:");
        styleDialog(dialog);
        dialog.showAndWait().ifPresent(name -> {
            if (name.isBlank()) return;
            File newFile = new File(currentDirectory, name.trim());
            try {
                if (!newFile.createNewFile()) {
                    showError("File already exists", name.trim() + " already exists.");
                } else {
                    refreshTree();
                }
            } catch (IOException e) {
                showError("Error", "Could not create file:\n" + e.getMessage());
            }
        });
    }

    @FXML
    public void handleNewFolderInDir() {
        if (currentDirectory == null) {
            showInfo("No folder open", "Open a file first so the explorer knows which folder to use.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog("new_folder");
        dialog.setTitle("New Folder");
        dialog.setHeaderText("Create folder in: " + currentDirectory.getName());
        dialog.setContentText("Folder name:");
        styleDialog(dialog);
        dialog.showAndWait().ifPresent(name -> {
            if (name.isBlank()) return;
            File newDir = new File(currentDirectory, name.trim());
            if (!newDir.mkdir()) {
                showError("Error", "Could not create folder.");
            } else {
                refreshTree();
            }
        });
    }

    @FXML
    public void handleRefreshExplorer() {
        refreshTree();
    }

    /** Re-reads the current directory from disk and rebuilds the tree. */
    private void refreshTree() {
        if (currentDirectory != null) {
            openDirectory(currentDirectory);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Registers update
    // ─────────────────────────────────────────────────────────────────────────

    /** Syncs all register / flag labels with live CPU state. */
    public void updateRegisters(CPU cpu) {
        if (cpu == null) return;
        regALabel.setText(String.format("%02XH", cpu.a));
        regBLabel.setText(String.format("%02XH", cpu.b));
        regCLabel.setText(String.format("%02XH", cpu.c));
        regDLabel.setText(String.format("%02XH", cpu.d));
        regELabel.setText(String.format("%02XH", cpu.e));
        regHLabel.setText(String.format("%02XH", cpu.h));
        regLLabel.setText(String.format("%02XH", cpu.l));
        regPCLabel.setText(String.format("%04XH", cpu.pc));
        regSPLabel.setText(String.format("%04XH", cpu.sp));
        flagZLabel.setText(String.valueOf(cpu.flagZ  ? 1 : 0));
        flagSLabel.setText(String.valueOf(cpu.flagS  ? 1 : 0));
        flagCYLabel.setText(String.valueOf(cpu.flagCY ? 1 : 0));
        flagPLabel.setText(String.valueOf(cpu.flagP  ? 1 : 0));
        flagACLabel.setText(String.valueOf(cpu.flagAC ? 1 : 0));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Panel toggle  (called by activity bar buttons in MainController)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Swaps the visible panel between "File Explorer" and "Registers & Flags".
     * @param showRegisters true → registers view; false → file explorer view.
     */
    public void toggleView(boolean showRegisters) {
        if (showRegisters) {
            sidebarTitleLabel.setText("REGISTERS & FLAGS");
            fileExplorerPane.setVisible(false);
            fileExplorerPane.setManaged(false);
            registersScrollPane.setVisible(true);
            registersScrollPane.setManaged(true);
            fileActionsBar.setVisible(false);
            fileActionsBar.setManaged(false);
        } else {
            sidebarTitleLabel.setText("EXPLORER");
            fileExplorerPane.setVisible(true);
            fileExplorerPane.setManaged(true);
            registersScrollPane.setVisible(false);
            registersScrollPane.setManaged(false);
            fileActionsBar.setVisible(true);
            fileActionsBar.setManaged(true);
        }
    }

    /** Called by activity-bar Files button via MainController. */
    @FXML public void triggerShowFiles()     { toggleView(false); }

    /** Called by activity-bar Registers button via MainController. */
    @FXML public void triggerShowRegisters() { toggleView(true); }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void showUnsupportedFileDialog(String fileName) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Cannot open file");
        alert.setHeaderText("Unsupported file type");
        alert.setContentText("\"" + fileName + "\" cannot be opened in 8085 Studio.\n\n"
                + "Only .asm and .txt files are supported.");
        alert.showAndWait();
    }

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

    /** Minimal dark styling for input dialogs. */
    private void styleDialog(Dialog<?> dialog) {
        dialog.getDialogPane().setStyle("-fx-background-color: #252526; -fx-text-fill: #d4d4d4;");
    }
}
