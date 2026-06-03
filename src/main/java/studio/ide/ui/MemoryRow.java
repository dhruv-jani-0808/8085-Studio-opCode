package studio.ide.ui;

public class MemoryRow {

    private final String hexAddress;
    private final int decimalAddress;
    private int dataValue;

    public MemoryRow(int address, int value) {
        this.hexAddress = String.format("%04X", address);
        this.decimalAddress = address;
        this.dataValue = value;
    }

    public String getHexAddress() {
        return hexAddress;
    }

    public int getDecimalAddress() {
        return decimalAddress;
    }

    public int getDataValue() {
        return dataValue;
    }

    public String getDataValueDisplay() {
        return String.format("%02X", dataValue);
    }

    public void setDataValue(int dataValue) {
        this.dataValue = dataValue & 0xFF;
    }
}
