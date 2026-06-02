package studio.ide.emulator;

public class CPU {
    public int a, b, c, d, e, h, l;
    public int sp, pc;
    public boolean flagP, flagZ, flagCY, flagAC, flagS; //5 flags

    private Memory memory;
    private InstructionSet instructionSet;

    public CPU(Memory memory) {
        this.memory = memory;
        this.instructionSet = new InstructionSet();
    }

    public int getPSW() {
        int f = 0;
        if (flagS) f |= 0x80;  // S is Bit 7
        if (flagZ) f |= 0x40;  // Z is Bit 6
        //Bit 5 is always 0
        if (flagAC) f |= 0x10;  // AC is Bit 4
        //Bit 3 is always 0
        if (flagP) f |= 0x04;  // P is Bit 2
        //Bit 1 is always 1
        f |= 0x02;
        if (flagCY) f |= 0x01;  // CY is Bit 0
        return f;
    }

    public void setPSW(int f) {
        flagS = (f & 0x80) != 0;
        flagZ = (f & 0x40) != 0;
        flagAC = (f & 0x10) != 0;
        flagP = (f & 0x04) != 0;
        flagCY = (f & 0x01) != 0;
    }

    public int readMemory(int address) {
        return memory.read(address) & 0xFF;
    }

    public void writeMemory(int address, int value) {
        memory.write(address, (byte) value);
    }

    public void step() {

        int rawOpCode = readMemory(pc);
        OpCode instruction = instructionSet.table[rawOpCode];
        int bytes = instructionSet.bytes[rawOpCode];

        pc += bytes;

        if (instruction != null) {
            int oldPc = pc;
            instruction.execute(this);
            if (pc == oldPc) pc += bytes;
        }

        else System.out.println("Invalid Opcode!");

    }

}