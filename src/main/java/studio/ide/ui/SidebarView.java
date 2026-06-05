package studio.ide.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import studio.ide.emulator.CPU;

public class SidebarView {

    @FXML private Label sidebarTitleLabel;
    @FXML private VBox fileExplorerPane;
    @FXML private VBox registersPane;
    @FXML private Label regALabel, regBLabel, regCLabel, regDLabel, regELabel, regHLabel, regLLabel;
    @FXML private Label regPCLabel, regSPLabel;
    @FXML private Label flagZLabel, flagSLabel, flagCYLabel, flagPLabel, flagACLabel;

    public void updateRegisters(CPU cpu) {
        if(cpu == null) return;

        /**
         * Updates all the text labels in the register pane with live data from the CPU core.
         */
        regALabel.setText(String.format("A: %02XH", cpu.a));
        regBLabel.setText(String.format("B: %02XH", cpu.b));
        regCLabel.setText(String.format("C: %02XH", cpu.c));
        regDLabel.setText(String.format("D: %02XH", cpu.d));
        regELabel.setText(String.format("E: %02XH", cpu.e));
        regHLabel.setText(String.format("H: %02XH", cpu.h));
        regLLabel.setText(String.format("L: %02XH", cpu.l));

        regPCLabel.setText(String.format("PC: %04XH", cpu.pc));
        regSPLabel.setText(String.format("SP: %04XH", cpu.sp));

        flagZLabel.setText(String.format("Z: %d", cpu.flagZ ? 1 : 0));
        flagSLabel.setText(String.format("S: %d", cpu.flagS ? 1 : 0));
        flagCYLabel.setText(String.format("CY: %d", cpu.flagCY ? 1 : 0));
        flagPLabel.setText(String.format("P: %d", cpu.flagP ? 1 : 0));
        flagACLabel.setText(String.format("AC: %d", cpu.flagAC ? 1 : 0));
    }

    /**
     * Swaps the visible view panel on the left sidebar.
     * @param showRegisters if true, shows registers; if false, shows file explorer.
     */
    public void toggleView(boolean showRegisters) {
        if (showRegisters) {
            sidebarTitleLabel.setText("Registers & Flags");
            fileExplorerPane.setVisible(false);
            fileExplorerPane.setManaged(false);
            registersPane.setVisible(true);
            registersPane.setManaged(true);
        } else {
            sidebarTitleLabel.setText("File Explorer");
            fileExplorerPane.setVisible(true);
            fileExplorerPane.setManaged(true);
            registersPane.setVisible(false);
            registersPane.setManaged(false);
        }
    }

    @FXML public void triggerShowFiles() { toggleView(false); }
    @FXML public void triggerShowRegisters() { toggleView(true); }
}
