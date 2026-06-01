package studio.ide.emulator;

public class InstructionSet {

    public final OpCode[] table = new OpCode[256];
    public final int[] bytes = new int[256];


    // remember
    // 000 B
    // 001 C
    // 010 D
    // 011 E
    // 100 H
    // 101 L
    // 110 M
    // 111 A

    // 00 BC
    // 01 DE
    // 10 HL
    // 11 SP

    public InstructionSet() {
        //ADD Reg Opcode 1 0 0 0 0 X X X; 8
        //ADC Reg Opcode 1 0 0 0 1 X X X; 8
        for (int opcode = 0x80; opcode <= 0x8F; opcode++) {

            final int currHex = opcode;

            bytes[currHex] = 1;

            table[currHex] = (cpu) -> {
                int srcReg = currHex & 0x07;
                int addend = getRegValue(cpu, srcReg);
                boolean isADC = (currHex & 0x08) != 0;
                int carryIn = (isADC && cpu.flagCY) ? 1 : 0;
                doAdd(cpu, addend, carryIn);
            };
        }

        //SUB Reg Opcode 1 0 0 1 0 X X X; 8
        //SBB Reg Opcode 1 0 0 1 1 X X X; 8
        for (int opcode = 0x90; opcode <= 0x9F; opcode++) {

            final int currHex = opcode;

            bytes[currHex] = 1;

            table[currHex] = (cpu) -> {
                int srcReg = currHex & 0x07;
                int subtrahend = getRegValue(cpu, srcReg);
                boolean isSBB = (currHex & 0x08) != 0;
                int borrowIn = (isSBB && cpu.flagCY) ? 1 : 0;
                doSub(cpu, subtrahend, borrowIn);
            };
        }

        //ADI 0xC6
        bytes[0xC6] = 2;
        table[0xC6] = (cpu) -> {
            int immediateData = cpu.readMemory(cpu.pc + 1);
            doAdd(cpu, immediateData, 0);
        };

        //ACI 0xCE
        bytes[0xCE] = 2;
        table[0xCE] = (cpu) -> {
            int immediateData = cpu.readMemory(cpu.pc + 1);
            int carryIn = cpu.flagCY ? 1 : 0;
            doAdd(cpu, immediateData, carryIn);
        };

        //SUI 0xD6
        bytes[0xD6] = 2;
        table[0xD6] = (cpu) -> {
            int immediateData = cpu.readMemory(cpu.pc + 1);
            doSub(cpu, immediateData, 0);
        };

        //SBI 0xCE
        bytes[0xDE] = 2;
        table[0xDE] = (cpu) -> {
            int immediateData = cpu.readMemory(cpu.pc + 1);
            int borrowIn = cpu.flagCY ? 1 : 0;
            doSub(cpu, immediateData, borrowIn);
        };

        //ANA 1 0 1 0 0 X X X
        for (int opcode = 0xA0; opcode <= 0xA7; opcode++) {
            final int currHex = opcode;
            bytes[currHex] = 1;

            table[currHex] = (cpu) -> {
                int srcReg = currHex & 0x07;
                int operand = getRegValue(cpu, srcReg);
                doAnd(cpu, operand);
            };
        }

        //XRA 1 0 1 0 1 X X X
        for (int opcode = 0xA8; opcode <= 0xAF; opcode++) {
            final int currHex = opcode;
            bytes[currHex] = 1;

            table[currHex] = (cpu) -> {
                int srcReg = currHex & 0x07;
                int operand = getRegValue(cpu, srcReg);
                doXor(cpu, operand);
            };
        }

        //ORA 1 0 1 1 0 X X X
        for (int opcode = 0xB0; opcode <= 0xB7; opcode++) {
            final int currHex = opcode;
            bytes[currHex] = 1;

            table[currHex] = (cpu) -> {
                int srcReg = currHex & 0x07;
                int operand = getRegValue(cpu, srcReg);
                doOr(cpu, operand);
            };
        }

        //CMP 1 0 1 1 0 X X X
        for (int opcode = 0xB8; opcode <= 0xBF; opcode++) {
            final int currHex = opcode;
            bytes[currHex] = 1;

            table[currHex] = (cpu) -> {
                int srcReg = currHex & 0x07;
                int operand = getRegValue(cpu, srcReg);
                doCmp(cpu, operand);
            };
        }

        //INR 0 0 D D D 1 0 0
        for (int opcode = 0x04; opcode <= 0x3C; opcode += 0x08) {
            final int currHex = opcode;
            bytes[currHex] = 1;

            table[currHex] = (cpu) -> {
                int targetReg = (currHex >> 3) & 0x07;
                int val = getRegValue(cpu, targetReg);
                setRegValue(cpu, targetReg, doInr(cpu, val));
            };
        }

        //DCR 0 0 D D D 1 0 1
        for (int opcode = 0x05; opcode <= 0x3D; opcode += 0x08) {
            final int currHex = opcode;
            bytes[currHex] = 1;

            table[currHex] = (cpu) -> {
                int targetReg = (currHex >> 3) & 0x07;
                int val = getRegValue(cpu, targetReg);
                setRegValue(cpu, targetReg, doDcr(cpu, val));
            };
        }

        // ANI
        bytes[0xE6] = 2;
        table[0xE6] = (cpu) -> {
            int immediateData = cpu.readMemory(cpu.pc + 1);
            doAnd(cpu, immediateData);
        };

        // XRI
        bytes[0xEe] = 2;
        table[0xEE] = (cpu) -> {
            int immediateData = cpu.readMemory(cpu.pc + 1);
            doXor(cpu, immediateData);
        };

        // ORI
        bytes[0xF6] = 2;
        table[0xF6] = (cpu) -> {
            int immediateData = cpu.readMemory(cpu.pc + 1);
            doOr(cpu, immediateData);
        };

        // CPI
        bytes[0xFE] = 2;
        table[0xFE] = (cpu) -> {
            int immediateData = cpu.readMemory(cpu.pc + 1);
            doCmp(cpu, immediateData);
        };

        //CMA
        bytes[0x2F] = 1;
        table[0x2F] = (cpu) -> {
            cpu.a = (~cpu.a) & 0xFF;
        };





        //MVI Reg Opcode 0 0 X X X 1 0 0; 8
        for (int opcode = 0x06; opcode <= 0x3E; opcode += 0x08) {

            final int currHex = opcode;

            bytes[currHex] = 2;

            table[currHex] = (cpu) -> {
                int destReg = (currHex >> 3) & 0x07;
                int immediateData = cpu.readMemory(cpu.pc + 1);
                setRegValue(cpu, destReg, immediateData);
            };
        }

        //MOV Reg, Reg Opcode 0 1 D D D S S S; 63
        for (int opcode = 0x40; opcode <= 0x7F; opcode++) {
            final int currHex = opcode;
            bytes[currHex] = 1;

            if (currHex == 0x76) continue;

            table[currHex] = (cpu) -> {
                int destReg = (currHex >> 3) & 0x07;
                int srcReg = currHex & 0x07;

                int value = getRegValue(cpu, srcReg);
                setRegValue(cpu, destReg, value);
            };
        }

        //LDA address
        bytes[0x3A] = 3;
        table[0x3A] = (cpu) -> {
            int lowerByte = cpu.readMemory(cpu.pc + 1);
            int higherByte = cpu.readMemory(cpu.pc + 2);
            int address = (higherByte << 8) | lowerByte;

            setRegValue(cpu, 0x07, cpu.readMemory(address));
        };

        //STA address
        bytes[0x32] = 3;
        table[0x32] = (cpu) -> {
            int lowerByte = cpu.readMemory(cpu.pc + 1);
            int higherByte = cpu.readMemory(cpu.pc + 2);
            int address = (higherByte << 8) | lowerByte;

            cpu.writeMemory(address, cpu.a);
        };

        //LHLD address
        bytes[0x2A] = 3;
        table[0x2A] = (cpu) -> {
            int lowerByte = cpu.readMemory(cpu.pc + 1);
            int higherByte = cpu.readMemory(cpu.pc + 2);
            int baseAdress = (higherByte << 8) | lowerByte;

            setRegValue(cpu, 0x05, cpu.readMemory(baseAdress));
            setRegValue(cpu, 0x04, cpu.readMemory(baseAdress + 1));
        };

        //SHLD address
        bytes[0x22] = 3;
        table[0x22] = (cpu) -> {
            int lowerByte = cpu.readMemory(cpu.pc + 1);
            int higherByte = cpu.readMemory(cpu.pc + 2);
            int baseAdress = (higherByte << 8) | lowerByte;

            cpu.writeMemory(baseAdress, getRegValue(cpu, 0x05));
            cpu.writeMemory(baseAdress + 1, getRegValue(cpu, 0x04));
        };

        //LDAX B
        bytes[0x0A] = 1;
        table[0x0A] = (cpu) -> {
            int address = ((getRegValue(cpu, 0x00) << 8) | getRegValue(cpu, 0x01));
            setRegValue(cpu, 0x07, cpu.readMemory(address));
        };

        //LDAX D
        bytes[0x1A] = 1;
        table[0x1A] = (cpu) -> {
            int address = ((getRegValue(cpu, 0x02) << 8) | getRegValue(cpu, 0x03));
            setRegValue(cpu, 0x07, cpu.readMemory(address));
        };

        //STAX B
        bytes[0x02] = 1;
        table[0x02] = (cpu) -> {
            int address = ((getRegValue(cpu, 0x00) << 8) | getRegValue(cpu, 0x01));
            cpu.writeMemory(address, getRegValue(cpu, 0x07));
        };

        //STAX D
        bytes[0x12] = 1;
        table[0x12] = (cpu) -> {
            int address = ((getRegValue(cpu, 0x02) << 8) | getRegValue(cpu, 0x03));
            cpu.writeMemory(address, getRegValue(cpu, 0x07));
        };

        //LXI B, data16
        bytes[0x01] = 3;
        table[0x01] = (cpu) -> {
            int lowerByte = cpu.readMemory(cpu.pc + 1);
            int higherByte = cpu.readMemory(cpu.pc + 2);

            setRegValue(cpu, 0x00, higherByte);
            setRegValue(cpu, 0x01, lowerByte);
        };

        //LXI D, data16
        bytes[0x11] = 3;
        table[0x11] = (cpu) -> {
            int lowerByte = cpu.readMemory(cpu.pc + 1);
            int higherByte = cpu.readMemory(cpu.pc + 2);

            setRegValue(cpu, 0x02, higherByte);
            setRegValue(cpu, 0x03, lowerByte);
        };

        //LXI H, data16
        bytes[0x21] = 3;
        table[0x21] = (cpu) -> {
            int lowerByte = cpu.readMemory(cpu.pc + 1);
            int higherByte = cpu.readMemory(cpu.pc + 2);

            setRegValue(cpu, 0x04, higherByte);
            setRegValue(cpu, 0x05, lowerByte);
        };

        //LXI SP, data16
        bytes[0x31] = 3;
        table[0x31] = (cpu) -> {
            int lowerByte = cpu.readMemory(cpu.pc + 1);
            int higherByte = cpu.readMemory(cpu.pc + 2);

            cpu.sp = (higherByte << 8) | lowerByte;
        };

        //JMP address
        bytes[0xC3] = 3;
        table[0xC3] = (cpu) -> {
            int lowerByte = cpu.readMemory(cpu.pc + 1);
            int higherByte = cpu.readMemory(cpu.pc + 2);

            int targetAddress = (higherByte << 8) | lowerByte;
            cpu.pc = targetAddress - 3;
        };

        //JNZ address
        bytes[0xC2] = 3;
        table[0xC2] = (cpu) -> {
            int lowerByte = cpu.readMemory(cpu.pc + 1);
            int higherByte = cpu.readMemory(cpu.pc + 2);

            int targetAddress = (higherByte << 8) | lowerByte;
            if(!cpu.flagZ) cpu.pc = targetAddress - 3; // Fixed: Jump if NOT zero
        };

        //JZ address
        bytes[0xCA] = 3;
        table[0xCA] = (cpu) -> {
            int lowerByte = cpu.readMemory(cpu.pc + 1);
            int higherByte = cpu.readMemory(cpu.pc + 2);

            int targetAddress = (higherByte << 8) | lowerByte;
            if(cpu.flagZ) cpu.pc = targetAddress - 3; // Fixed: Jump if zero
        };

        //JNC address
        bytes[0xD2] = 3;
        table[0xD2] = (cpu) -> {
            int lowerByte = cpu.readMemory(cpu.pc + 1);
            int higherByte = cpu.readMemory(cpu.pc + 2);

            int targetAddress = (higherByte << 8) | lowerByte;
            if(!cpu.flagCY) cpu.pc = targetAddress - 3; // Fixed: Jump if NO carry
        };

        //JC address
        bytes[0xDA] = 3;
        table[0xDA] = (cpu) -> {
            int lowerByte = cpu.readMemory(cpu.pc + 1);
            int higherByte = cpu.readMemory(cpu.pc + 2);

            int targetAddress = (higherByte << 8) | lowerByte;
            if(cpu.flagCY) cpu.pc = targetAddress - 3; // Fixed: Jump if carry
        };

        //JPO address
        bytes[0xE2] = 3;
        table[0xE2] = (cpu) -> {
            int lowerByte = cpu.readMemory(cpu.pc + 1);
            int higherByte = cpu.readMemory(cpu.pc + 2);

            int targetAddress = (higherByte << 8) | lowerByte;
            if(!cpu.flagP) cpu.pc = targetAddress - 3; // Fixed: Jump if parity is ODD
        };

        //JPE address
        bytes[0xEA] = 3;
        table[0xEA] = (cpu) -> {
            int lowerByte = cpu.readMemory(cpu.pc + 1);
            int higherByte = cpu.readMemory(cpu.pc + 2);

            int targetAddress = (higherByte << 8) | lowerByte;
            if(cpu.flagP) cpu.pc = targetAddress - 3; // Fixed: Jump if parity is EVEN
        };

        //JP address
        bytes[0xF2] = 3;
        table[0xF2] = (cpu) -> {
            int lowerByte = cpu.readMemory(cpu.pc + 1);
            int higherByte = cpu.readMemory(cpu.pc + 2);

            int targetAddress = (higherByte << 8) | lowerByte;
            if(!cpu.flagS) cpu.pc = targetAddress - 3; // Fixed: Jump if Positive (Sign bit 0)
        };

        //JM address
        bytes[0xFA] = 3;
        table[0xFA] = (cpu) -> {
            int lowerByte = cpu.readMemory(cpu.pc + 1);
            int higherByte = cpu.readMemory(cpu.pc + 2);

            int targetAddress = (higherByte << 8) | lowerByte;
            if(cpu.flagS) cpu.pc = targetAddress - 3; // Fixed: Jump if Minus (Sign bit 1)
        };
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

    private void setRegValue(CPU cpu, int regCode, int value) {
        switch (regCode) {
            case 0: cpu.b = value; break;
            case 1: cpu.c = value; break;
            case 2: cpu.d = value; break;
            case 3: cpu.e = value; break;
            case 4: cpu.h = value; break;
            case 5: cpu.l = value; break;
            case 6: cpu.writeMemory(getHLAddress(cpu), value); break;
            case 7: cpu.a = value; break;
        }
    }

    private void doAdd(CPU cpu, int addend, int carryIn) {
        int result = cpu.a + addend + carryIn;
        setZSP(cpu, result);
        cpu.flagCY = result > 255;
        cpu.flagAC = ((cpu.a ^ result ^ addend) & 0x10) != 0;

        cpu.a = result & 0xFF;

    }

    private void doSub(CPU cpu, int subtrahend, int borrowIn) {

        int valA = cpu.a;
        int totalSubtrahend = subtrahend + borrowIn;
        int result = valA - totalSubtrahend;
        setZSP(cpu, result);
        cpu.flagCY = valA < totalSubtrahend; //CY becomes borrow

        int twosC = (~totalSubtrahend & 0xFF) + 1; //two's complement
        int aluAddn = valA + twosC;
        cpu.flagAC = ((valA ^ twosC ^ aluAddn) & 0x10) != 0;

        cpu.a = result & 0xFF;

    }

    private void doAnd(CPU cpu, int operand) {
        int valA = cpu.a;
        int result = valA & operand;
        setZSP(cpu, result);
        cpu.flagAC = true;
        cpu.flagCY = false;
        cpu.a = result & 0xFF;
    }

    private void doOr(CPU cpu, int operand) {
        int valA = cpu.a;
        int result = valA | operand;
        setZSP(cpu, result);
        cpu.flagAC = false;
        cpu.flagCY = false;
        cpu.a = result & 0xFF;
    }

    private void doXor(CPU cpu, int operand) {
        int valA = cpu.a;
        int result = valA ^ operand;
        setZSP(cpu, result);
        cpu.flagAC = false;
        cpu.flagCY = false;
        cpu.a = result & 0xFF;
    }

    private void doCmp(CPU cpu, int operand) {
        int valA = cpu.a;
        doSub(cpu, operand, 0);
        cpu.a = valA;
    }

    private int doInr(CPU cpu, int val) {
        int result = val + 1;
        setZSP(cpu, result);
        cpu.flagAC = ((val ^ 1 ^ result) & 0x10) != 0;
        return result & 0xFF;
    }

    private int doDcr(CPU cpu, int val) {
        int result = val - 1;
        setZSP(cpu, result);
        int twosComp = 0xFF;
        int aluAddn = val + twosComp;
        cpu.flagAC = ((val ^ twosComp ^ aluAddn) * 0x10) != 0;
        return result & 0xFF;
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