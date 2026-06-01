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

        // LDA address (0x3A)
        bytes[0x3A] = 3;
        table[0x3A] = (cpu) -> {
            int address = cpu.readMemory(cpu.pc + 1) | (cpu.readMemory(cpu.pc + 2) << 8);
            setRegValue(cpu, 7, cpu.readMemory(address)); // 7 is Reg A
        };

        // STA address (0x32)
        bytes[0x32] = 3;
        table[0x32] = (cpu) -> {
            int address = cpu.readMemory(cpu.pc + 1) | (cpu.readMemory(cpu.pc + 2) << 8);
            cpu.writeMemory(address, cpu.a);
        };

        // LHLD address (0x2A)
        bytes[0x2A] = 3;
        table[0x2A] = (cpu) -> {
            int address = cpu.readMemory(cpu.pc + 1) | (cpu.readMemory(cpu.pc + 2) << 8);
            setRegValue(cpu, 5, cpu.readMemory(address));     // Reg L
            setRegValue(cpu, 4, cpu.readMemory(address + 1)); // Reg H
        };

        // SHLD address (0x22)
        bytes[0x22] = 3;
        table[0x22] = (cpu) -> {
            int address = cpu.readMemory(cpu.pc + 1) | (cpu.readMemory(cpu.pc + 2) << 8);
            cpu.writeMemory(address, getRegValue(cpu, 5));     // Store L
            cpu.writeMemory(address + 1, getRegValue(cpu, 4)); // Store H
        };

        // LDAX Pair (0x0A, 0x1A) and STAX Pair (0x02, 0x12)
        // Format: 00XX0010 (STAX) and 00XX1010 (LDAX) where XX is pair 0 (BC) or 1 (DE)
        for (int pair = 0; pair <= 1; pair++) {
            int staxOpcode = 0x02 | (pair << 4);
            int ldaxOpcode = 0x0A | (pair << 4);

            // STAX pair
            bytes[staxOpcode] = 1;
            table[staxOpcode] = (cpu) -> {
                int currentPair = (staxOpcode >> 4) & 0x03;
                cpu.writeMemory(getPairValue(cpu, currentPair), cpu.a);
            };

            // LDAX pair
            bytes[ldaxOpcode] = 1;
            table[ldaxOpcode] = (cpu) -> {
                int currentPair = (ldaxOpcode >> 4) & 0x03;
                setRegValue(cpu, 7, cpu.readMemory(getPairValue(cpu, currentPair)));
            };
        }

        // LXI Pair, data16 (0x01, 0x11, 0x21, 0x31)
        // Format: 00XX0001 where XX ranges from 0 to 3 (BC, DE, HL, SP)
        for (int opcode = 0x01; opcode <= 0x31; opcode += 0x10) {
            final int currHex = opcode;
            bytes[currHex] = 3;
            table[currHex] = (cpu) -> {
                int targetPair = (currHex >> 4) & 0x03;
                int immediate16 = cpu.readMemory(cpu.pc + 1) | (cpu.readMemory(cpu.pc + 2) << 8);
                setPairValue(cpu, targetPair, immediate16);
            };
        }

        // Unconditional JMP (0xC3)
        bytes[0xC3] = 3;
        table[0xC3] = (cpu) -> {
            cpu.pc = (cpu.readMemory(cpu.pc + 1) | (cpu.readMemory(cpu.pc + 2) << 8)) - 3;
        };

        // Conditional Jumps Array Mapping
        // Index mapping: 0=NZ (0xC2), 1=Z (0xCA), 2=NC (0xD2), 3=C (0xDA),
        //                4=PO (0xE2), 5=PE (0xEA), 6=P (0xF2), 7=M (0xFA)
        for (int i = 0; i < 8; i++) {
            int jumpOpcode = 0xC2 | (i << 3);
            bytes[jumpOpcode] = 3;

            table[jumpOpcode] = (cpu) -> {
                int conditionCode = (jumpOpcode >> 3) & 0x07;
                boolean shouldJump = false;

                switch (conditionCode) {
                    case 0:
                        shouldJump = !cpu.flagZ;
                        break; // JNZ
                    case 1:
                        shouldJump = cpu.flagZ;
                        break; // JZ
                    case 2:
                        shouldJump = !cpu.flagCY;
                        break; // JNC
                    case 3:
                        shouldJump = cpu.flagCY;
                        break; // JC
                    case 4:
                        shouldJump = !cpu.flagP;
                        break; // JPO
                    case 5:
                        shouldJump = cpu.flagP;
                        break; // JPE
                    case 6:
                        shouldJump = !cpu.flagS;
                        break; // JP
                    case 7:
                        shouldJump = cpu.flagS;
                        break; // JM
                }

                if (shouldJump) {
                    cpu.pc = (cpu.readMemory(cpu.pc + 1) | (cpu.readMemory(cpu.pc + 2) << 8)) - 3;
                }
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

    private int getPairValue(CPU cpu, int pairCode) {
        switch(pairCode) {
            case 0: return (cpu.b << 8) | cpu.c;       // 00 -> BC
            case 1: return (cpu.d << 8) | cpu.e;       // 01 -> DE
            case 2: return (cpu.h << 8) | cpu.l;       // 10 -> HL
            case 3: return cpu.sp;                     // 11 -> SP
            default: return 0;
        }
    }

    private void setPairValue(CPU cpu, int pairCode, int value) {
        switch(pairCode) {
            case 0: // BC
                cpu.b = (value >> 8) & 0xFF;
                cpu.c = value & 0xFF;
                break;
            case 1: // DE
                cpu.d = (value >> 8) & 0xFF;
                cpu.e = value & 0xFF;
                break;
            case 2: // HL
                cpu.h = (value >> 8) & 0xFF;
                cpu.l = value & 0xFF;
                break;
            case 3: // SP
                cpu.sp = value & 0xFFFF;
                break;
        }
    }
}