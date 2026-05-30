module studio.ide {
    requires javafx.controls;
    requires javafx.fxml;


    opens studio.ide to javafx.fxml;
    exports studio.ide;
}