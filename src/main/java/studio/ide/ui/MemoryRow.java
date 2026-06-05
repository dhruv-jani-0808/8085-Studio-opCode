package studio.ide.ui;

import studio.ide.emulator.Memory;

public class MemoryRow {

    private final String hexAddress;
    private final int decimalAddress;
    private final Memory memory;

    public MemoryRow(int address, Memory memory) {
        this.hexAddress = String.format("%04XH", address);
        this.decimalAddress = address;
        this.memory = memory;
    }

    public String getHexAddress() {
        return hexAddress;
    }

    public int getDecimalAddress() {
        return decimalAddress;
    }

    public String getDataValueDisplay() {
        int liveValue = memory.read(decimalAddress) & 0xFF;
        return String.format("%02X", liveValue);
    }

    public void setDataValue(int dataValue) {
        memory.write(decimalAddress, (byte) dataValue);
    }
}
