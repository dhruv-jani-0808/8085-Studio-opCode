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

        //UNIFIED LOOP FOR ALL ALU INR DCR MVI

        for (int regCode = 0; regCode < 8; ++regCode) {
            final int reg = regCode;

            //ADD
            bytes[0x80 | reg] = 1;
            table[0x80 | reg] = (cpu) -> doAdd(cpu, getRegValue(cpu, reg), 0);

            //ADC
            bytes[0x88 | reg] = 1;
            table[0x88 | reg] = (cpu) -> doAdd(cpu, getRegValue(cpu, reg), cpu.flagCY ? 1 : 0);

            //SUB
            bytes[0x90 | reg] = 1;
            table[0x90 | reg] = (cpu) -> doSub(cpu, getRegValue(cpu, reg), 0);

            //SBB
            bytes[0x98 | reg] = 1;
            table[0x98 | reg] = (cpu) -> doSub(cpu, getRegValue(cpu, reg), cpu.flagCY ? 1 : 0);

            //ANA
            bytes[0xA0 | reg] = 1;
            table[0xA0 | reg] = (cpu) -> doAnd(cpu, getRegValue(cpu, reg));

            //XRA
            bytes[0xA8 | reg] = 1;
            table[0xA8 | reg] = (cpu) -> doXor(cpu, getRegValue(cpu, reg));

            //ORA
            bytes[0xB0 | reg] = 1;
            table[0xB0 | reg] = (cpu) -> doOr(cpu, getRegValue(cpu, reg));

            //CMP
            bytes[0xB8 | reg] = 1;
            table[0xB8 | reg] = (cpu) -> doCmp(cpu, getRegValue(cpu, reg));

            //INR
            int inrOpC =  0x04 | (reg << 3);
            bytes[inrOpC] = 1;
            table[inrOpC] = (cpu) -> {
                int val = getRegValue(cpu, reg);
                setRegValue(cpu, reg, doInr(cpu, val));
            };

            //DCR
            int dcrOpC =  0x05 | (reg << 3);
            bytes[dcrOpC] = 1;
            table[dcrOpC] = (cpu) -> {
                int val = getRegValue(cpu, reg);
                setRegValue(cpu, reg, doDcr(cpu, val));
            };

            //MVI
            int mviOpC =  0x06 | (reg << 3);
            bytes[mviOpC] = 2;
            table[mviOpC] = (cpu) -> {
                int immediateData = cpu.readMemory(cpu.pc + 1);
                setRegValue(cpu, reg, immediateData);
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

        //SBI 0xDE
        bytes[0xDE] = 2;
        table[0xDE] = (cpu) -> {
            int immediateData = cpu.readMemory(cpu.pc + 1);
            int borrowIn = cpu.flagCY ? 1 : 0;
            doSub(cpu, immediateData, borrowIn);
        };

        // ANI
        bytes[0xE6] = 2;
        table[0xE6] = (cpu) -> {
            int immediateData = cpu.readMemory(cpu.pc + 1);
            doAnd(cpu, immediateData);
        };

        // XRI
        bytes[0xEE] = 2;
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

        //MOV Reg, Reg Opcode 0 1 D D D S S S; 63
        for (int dest = 0; dest < 8; ++dest) {
            for (int src = 0; src < 8; ++src) {

                if (dest == 6 && src == 6) continue;

                final int target = dest;
                final int source = src;

                int opcode = 0x40 | (dest << 3) | src;

                bytes[opcode] = 1;
                table[opcode] = (cpu) -> {
                    int value = getRegValue(cpu, source);
                    setRegValue(cpu, target, value);
                };
            }
        }

        //Reg Pair 28 Opcodes
        for (int regPairCode = 0; regPairCode < 4; ++regPairCode) {

            final int rp = regPairCode;

            //LXI
            bytes[0x01 | (rp << 4)] = 3;
            table[0x01 | (rp << 4)] = (cpu) -> {
                int immediateData = (cpu.readMemory(cpu.pc + 2) << 8) | cpu.readMemory(cpu.pc + 1);
                setPairValue(cpu, rp, immediateData);
            };

            //INX
            bytes[0x03 | (rp << 4)] = 1;
            table[0x03 | (rp << 4)] = (cpu) -> {
                int val = getPairValue(cpu, rp);
                setPairValue(cpu, rp, (val + 1) & 0xFFFF);
            };

            //DCX
            bytes[0x0B | (rp << 4)] = 1;
            table[0x0B | (rp << 4)] = (cpu) -> {
                int val = getPairValue(cpu, rp);
                setPairValue(cpu, rp, (val - 1) & 0xFFFF);
            };

            //DAD
            bytes[0x09 | (rp << 4)] = 1;
            table[0x09 | (rp << 4)] = (cpu) -> {
                int hl = getPairValue(cpu, 2);
                int pairVal = getPairValue(cpu, rp);
                int result = hl + pairVal;

                cpu.flagCY = (result > 0xFFFF);
                setPairValue(cpu, 2, result & 0xFFFF);
            };

            //PUSH
            bytes[0xC5 | (rp << 4)] = 1;
            table[0xC5 | (rp << 4)] = (cpu) -> {

                int valToPush = (rp == 3) ? ((cpu.a << 8) | cpu.getPSW()) : getPairValue(cpu, rp);

                cpu.sp = (cpu.sp - 1) & 0xFFFF;
                cpu.writeMemory(cpu.sp, (valToPush >> 8) & 0xFF);

                cpu.sp = (cpu.sp - 1) & 0xFFFF;
                cpu.writeMemory(cpu.sp, valToPush & 0xFF);

            };

            //POP
            bytes[0xC1 | (rp << 4)] = 1;
            table[0xC1 |(rp << 4)] = (cpu) -> {

                int low = cpu.readMemory(cpu.sp);
                cpu.sp = (cpu.sp + 1) & 0xFFFF;

                int high = cpu.readMemory(cpu.sp);
                cpu.sp = (cpu.sp + 1) & 0xFFFF;

                if (rp == 3) {
                    cpu.a = high;
                    cpu.setPSW(low);
                }

                else {
                    setPairValue(cpu, rp, (high << 8) | low);
                }
            };

            if (rp < 2) {

                //STAX
                bytes[0x02 | (rp << 4)] = 1;
                table[0x02 | (rp << 4)] = (cpu) -> {
                    int address = getPairValue(cpu, rp);
                    int accumulator = getRegValue(cpu, 7);
                    cpu.writeMemory(address, accumulator);
                };

                //LDAX
                bytes[0x0A | (rp << 4)] = 1;
                table[0x0A | (rp << 4)] = (cpu) -> {
                    int address = getPairValue(cpu, rp);
                    int value = cpu.readMemory(address);
                    setRegValue(cpu, 7, value);
                };

            }
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

        // Unconditional JMP (0xC3)
        bytes[0xC3] = 3;
        table[0xC3] = (cpu) -> {
            cpu.pc = (cpu.readMemory(cpu.pc + 1) | (cpu.readMemory(cpu.pc + 2) << 8));
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