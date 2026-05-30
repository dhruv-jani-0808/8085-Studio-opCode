package studio.ide.emulator;

public class InstructionSet {

    public final OpCode[] table = new OpCode[256];
    public final int[] bytes = new int[256];

    public InstructionSet() {

        //ADD Reg Opcode 1 0 0 0 0 X X X
        for (int opcode = 0x80; opcode <= 0x87; opcode++) {

            final int currHex = opcode;

            bytes[currHex] = 1;

            table[currHex] = (cpu) -> {
                int srcReg = currHex & 0x07;
                int addend = getRegValue(cpu, srcReg);
                doAdd(cpu, addend);
            };
        }

        //SUB Reg Opcode 1 0 0 1 0 X X X
        for (int opcode = 0x90; opcode <= 0x97; opcode++) {

            final int currHex = opcode;

            bytes[currHex] = 1;

            table[currHex] = (cpu) -> {
                int srcReg = currHex & 0x07;
                int subtrahend = getRegValue(cpu, srcReg);
                doSub(cpu, subtrahend);
            };
        }
    }

    private int getRegValue(CPU cpu, int regCode) {
        switch (regCode) {
            case 0: return cpu.b;
            case 1: return cpu.c;
            case 2: return cpu.d;
            case 3: return cpu.e;
            case 4: return cpu.h;
            case 5: return cpu.l;
            case 6: return cpu.readMemory(getHLAddress(cpu));
            case 7: return cpu.a;
            default: return 0;
        }
    }

    private void doAdd(CPU cpu, int addend) {
        int result = cpu.a + addend;
        setZSP(cpu, result);
        cpu.flagCY = result > 255;
        cpu.flagAC = ((cpu.a ^ result ^ addend) & 0x10) != 0;

        cpu.a = result & 0xFF;

    }

    private void doSub(CPU cpu, int subtrahend) {

        int valA = cpu.a;
        int result = valA - subtrahend;
        setZSP(cpu, result);
        cpu.flagCY = valA < subtrahend; //CY becomes borrow

        int twosC = (~subtrahend & 0xFF) + 1; //two's complement
        int aluAddn = valA + twosC;
        cpu.flagAC = ((valA ^ twosC ^ aluAddn) & 0x10) != 0;

        cpu.a = result & 0xFF;

    }

    private int getHLAddress(CPU cpu) {
        return (cpu.h << 8) | (cpu.l);
    }

    private void setZSP(CPU cpu, int result) {
        cpu.flagZ = (result & 0xFF) == 0;
        cpu.flagS = (result & 0x80) != 0;
        cpu.flagP = ((Integer.bitCount(result & 0xFF) & 1) == 0);
    }
}