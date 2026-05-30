package studio.ide;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import studio.ide.emulator.CPU;
import studio.ide.emulator.Memory;

public class HelloController {

    @FXML
    private Button navFilesBtn;
    @FXML
    private Button navRegsBtn;
    @FXML
    private Label sidebarTitleLabel;
    @FXML
    private Label regALabel;
    @FXML
    private Label regPCLabel;
    @FXML
    private Button executeBtn;

    private Memory memory = new Memory();
    private CPU cpu = new CPU(memory);

    @FXML
    public void initialize() {
        navFilesBtn.setOnAction(event -> {
            sidebarTitleLabel.setText("File Explorer");
        });

        navRegsBtn.setOnAction(event -> {
            sidebarTitleLabel.setText("Registers & Flags");
            updateRegisterUi();
        });

        executeBtn.setOnAction(event -> {
            memory.write(0x0000, (byte)0x3E);
            memory.write(0x0001, (byte)0x05);

            cpu.setPc(0x0000);

            cpu.step();

            updateRegisterUi();
        });
    }

    private void updateRegisterUi() {
        regALabel.setText(String.format("%02XH", cpu.getRegisterA()));
        regPCLabel.setText(String.format("%04XH", cpu.getPc()));
    }
}