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

        if (instruction != null) instruction.execute(this);
        else System.out.println("Invalid Opcode!");

    }

}