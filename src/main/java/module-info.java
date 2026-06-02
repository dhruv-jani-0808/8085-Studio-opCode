module studio.ide {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    // Open packages containing FXML files or Controllers to the FXML engine
    opens studio.ide to javafx.fxml;
    opens studio.ide.ui to javafx.fxml;
    opens studio.ide.emulator to javafx.fxml;
    opens studio.ide.assembler to javafx.fxml;

    // Export packages so Java can run them
    exports studio.ide;
    exports studio.ide.ui;
    exports studio.ide.emulator;
    exports studio.ide.assembler;
}