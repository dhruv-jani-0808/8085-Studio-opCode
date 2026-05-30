package studio.ide.emulator;

public class CPU {
    private int registerA = 0x00;
    private int pc = 0x0000;

    private Memory memory;

    public CPU(Memory memory) {
        this.memory = memory;
    }

    public void step() {
        int opcode = memory.read(pc);

        switch (opcode) {
            case 0x3E:
                pc++;
                int immediateData = memory.read(pc);

                this.registerA = immediateData;

                pc++;
                break;

            default:
                System.out.println("Unknown or unimplemented opcode: " + String.format("0x%02X", opcode));
                break;
        }
    }

    public int getRegisterA() { return registerA; }
    public void setRegisterA(int value) { this.registerA = value; }
    public int getPc() { return pc; }
    public void setPc(int pc) { this.pc = pc; }
}