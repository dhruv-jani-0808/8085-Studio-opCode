package studio.ide.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DefaultStringConverter;
import studio.ide.emulator.Memory;

public class MemoryTableView {

    @FXML private TableView<MemoryRow> memoryTable;
    @FXML private TableColumn<MemoryRow, String>  hexAddrCol;
    @FXML private TableColumn<MemoryRow, Integer> decAddrCol;
    @FXML private TableColumn<MemoryRow, String>  dataCol;
    @FXML private TextField searchField;

    private final ObservableList<MemoryRow> masterMemoryData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // After the scene attaches, color each column header label (Fix 7)
        memoryTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> styleColumnHeaders());
            }
        });
    }

    /** Applies distinct colours to the three column header labels. */
    private void styleColumnHeaders() {
        // JavaFX column header labels are found via CSS lookup after the skin is rendered
        for (Node header : memoryTable.lookupAll(".column-header .label")) {
            Node col = header.getParent(); // the column-header itself
            if (col != null) {
                String txt = ((javafx.scene.control.Label) header).getText();
                switch (txt) {
                    case "Address" -> header.setStyle("-fx-text-fill: #c586c0; -fx-font-weight: bold; -fx-font-size: 13px;");
                    case "Decimal" -> header.setStyle("-fx-text-fill: #9cdcfe; -fx-font-weight: bold; -fx-font-size: 13px;");
                    case "Data"    -> header.setStyle("-fx-text-fill: #ce9178; -fx-font-weight: bold; -fx-font-size: 13px;");
                    default        -> header.setStyle("-fx-text-fill: #d4d4d4; -fx-font-weight: bold; -fx-font-size: 13px;");
                }
            }
        }
    }

    /**
     * Call this inside initialize() to configure the grid columns and listeners.
     */
    public void initializeMemoryTable(Memory memory) {
        hexAddrCol.setCellValueFactory(new PropertyValueFactory<>("hexAddress"));
        decAddrCol.setCellValueFactory(new PropertyValueFactory<>("decimalAddress"));
        dataCol.setCellValueFactory(new PropertyValueFactory<>("dataValueDisplay")); // Pulls formatted string

        memoryTable.setEditable(true);
        dataCol.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));

        dataCol.setOnEditCommit(event -> {
            MemoryRow row = event.getRowValue();
            String userInput = event.getNewValue().trim().toUpperCase();

            try {
                int parsedValue;
                if (userInput.endsWith("H")) {
                    String cleanHex = userInput.substring(0, userInput.length() - 1);
                    parsedValue = Integer.parseInt(cleanHex, 16);
                } else {
                    parsedValue = Integer.parseInt(userInput);
                }

                if (parsedValue < 0 || parsedValue > 255) {
                    throw new NumberFormatException();
                }

                row.setDataValue(parsedValue);
                memoryTable.refresh();
            }
            catch (Exception e) {
                showErrorDialog("Invalid Data Value",
                        "Please enter a valid Decimal byte (0-255) or Hex byte ending in 'H' (00H-FFH).");
                memoryTable.refresh();
            }
        });

        populateCompleteGrid(memory);
        memoryTable.setItems(masterMemoryData);
    }

    /**
     * Initializes the entire virtual machine grid with 65,536 rows.
     */
    public void populateCompleteGrid(Memory memory) {
        masterMemoryData.clear();
        for (int i = 0; i < 65536; i++) {
            masterMemoryData.add(new MemoryRow(i, memory));
        }
    }

    /**
     * Live UI update hook that syncs memory values into the view grid panel.
     */
    public void refreshMemoryGrid(Memory memory) {
        if (memoryTable != null) {
            memoryTable.refresh();
        }
    }

    /**
     * Action bound to your search bar trigger. Matches radix endings automatically.
     */
    @FXML
    public void handleAddressSearch() {
        String query = searchField.getText().trim().toUpperCase();
        if (query.isEmpty()) return;

        try {
            int targetAddress;
            if (query.endsWith("H")) {
                String cleanHex = query.substring(0, query.length() - 1);
                targetAddress = Integer.parseInt(cleanHex, 16);
            } else {
                targetAddress = Integer.parseInt(query);
            }

            if (targetAddress < 0 || targetAddress > 65535) {
                throw new IndexOutOfBoundsException();
            }

            memoryTable.scrollTo(targetAddress);
            memoryTable.getSelectionModel().select(targetAddress);

        } catch (Exception e) {
            showErrorDialog("Invalid Address",
                    "Please enter a valid Decimal address (0-65535) or Hex address ending in 'H' (0000H-FFFFH).");
        }
    }

    private void showErrorDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
