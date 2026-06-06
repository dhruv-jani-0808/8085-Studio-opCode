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

        // =========================================================
        // 1. MOV INSTRUCTIONS (Loop: dest/src)
        // =========================================================
        // Pattern: 0 1 D D D S S S
        // Mnemonics: MOV Reg, Reg
        for (int dest = 0; dest < 8; ++dest) {
            for (int src = 0; src < 8; ++src) {

                // HLT (0x76) is effectively MOV M, M. We skip it here.
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

        // =========================================================
        // 2. UNIFIED ALU, INR, DCR, MVI (Loop: regCode)
        // =========================================================
        for (int regCode = 0; regCode < 8; ++regCode) {
            final int reg = regCode;

            // ADD Reg (Pattern: 1 0 0 0 0 S S S)
            bytes[0x80 | reg] = 1;
            table[0x80 | reg] = (cpu) -> doAdd(cpu, getRegValue(cpu, reg), 0);

            // ADC Reg (Pattern: 1 0 0 0 1 S S S)
            bytes[0x88 | reg] = 1;
            table[0x88 | reg] = (cpu) -> doAdd(cpu, getRegValue(cpu, reg), cpu.flagCY ? 1 : 0);

            // SUB Reg (Pattern: 1 0 0 1 0 S S S)
            bytes[0x90 | reg] = 1;
            table[0x90 | reg] = (cpu) -> doSub(cpu, getRegValue(cpu, reg), 0);

            // SBB Reg (Pattern: 1 0 0 1 1 S S S)
            bytes[0x98 | reg] = 1;
            table[0x98 | reg] = (cpu) -> doSub(cpu, getRegValue(cpu, reg), cpu.flagCY ? 1 : 0);

            // ANA Reg (Pattern: 1 0 1 0 0 S S S)
            bytes[0xA0 | reg] = 1;
            table[0xA0 | reg] = (cpu) -> doAnd(cpu, getRegValue(cpu, reg));

            // XRA Reg (Pattern: 1 0 1 0 1 S S S)
            bytes[0xA8 | reg] = 1;
            table[0xA8 | reg] = (cpu) -> doXor(cpu, getRegValue(cpu, reg));

            // ORA Reg (Pattern: 1 0 1 1 0 S S S)
            bytes[0xB0 | reg] = 1;
            table[0xB0 | reg] = (cpu) -> doOr(cpu, getRegValue(cpu, reg));

            // CMP Reg (Pattern: 1 0 1 1 1 S S S)
            bytes[0xB8 | reg] = 1;
            table[0xB8 | reg] = (cpu) -> doCmp(cpu, getRegValue(cpu, reg));

            // INR Reg (Pattern: 0 0 D D D 1 0 0)
            int inrOpC =  0x04 | (reg << 3);
            bytes[inrOpC] = 1;
            table[inrOpC] = (cpu) -> {
                int val = getRegValue(cpu, reg);
                setRegValue(cpu, reg, doInr(cpu, val));
            };

            // DCR Reg (Pattern: 0 0 D D D 1 0 1)
            int dcrOpC =  0x05 | (reg << 3);
            bytes[dcrOpC] = 1;
            table[dcrOpC] = (cpu) -> {
                int val = getRegValue(cpu, reg);
                setRegValue(cpu, reg, doDcr(cpu, val));
            };

            // MVI Reg, Data (Pattern: 0 0 D D D 1 1 0)
            int mviOpC =  0x06 | (reg << 3);
            bytes[mviOpC] = 2;
            table[mviOpC] = (cpu) -> {
                int immediateData = cpu.readMemory(cpu.pc + 1);
                setRegValue(cpu, reg, immediateData);
            };
        }

        // =========================================================
        // 3. REGISTER PAIR INSTRUCTIONS (Loop: regPairCode)
        // =========================================================
        for (int regPairCode = 0; regPairCode < 4; ++regPairCode) {

            final int rp = regPairCode;

            // LXI rp, Data16 (Pattern: 0 0 R P 0 0 0 1)
            bytes[0x01 | (rp << 4)] = 3;
            table[0x01 | (rp << 4)] = (cpu) -> {
                int immediateData = (cpu.readMemory(cpu.pc + 2) << 8) | cpu.readMemory(cpu.pc + 1);
                setPairValue(cpu, rp, immediateData);
            };

            // INX rp (Pattern: 0 0 R P 0 0 1 1)
            bytes[0x03 | (rp << 4)] = 1;
            table[0x03 | (rp << 4)] = (cpu) -> {
                int val = getPairValue(cpu, rp);
                setPairValue(cpu, rp, (val + 1) & 0xFFFF);
            };

            // DCX rp (Pattern: 0 0 R P 1 0 1 1)
            bytes[0x0B | (rp << 4)] = 1;
            table[0x0B | (rp << 4)] = (cpu) -> {
                int val = getPairValue(cpu, rp);
                setPairValue(cpu, rp, (val - 1) & 0xFFFF);
            };

            // DAD rp (Pattern: 0 0 R P 1 0 0 1)
            bytes[0x09 | (rp << 4)] = 1;
            table[0x09 | (rp << 4)] = (cpu) -> {
                int hl = getPairValue(cpu, 2);
                int pairVal = getPairValue(cpu, rp);
                int result = hl + pairVal;

                cpu.flagCY = (result > 0xFFFF);
                setPairValue(cpu, 2, result & 0xFFFF);
            };

            // PUSH rp/PSW (Pattern: 1 1 R P 0 1 0 1)
            bytes[0xC5 | (rp << 4)] = 1;
            table[0xC5 | (rp << 4)] = (cpu) -> {
                int valToPush = (rp == 3) ? ((cpu.a << 8) | cpu.getPSW()) : getPairValue(cpu, rp);

                cpu.sp = (cpu.sp - 1) & 0xFFFF;
                cpu.writeMemory(cpu.sp, (valToPush >> 8) & 0xFF);

                cpu.sp = (cpu.sp - 1) & 0xFFFF;
                cpu.writeMemory(cpu.sp, valToPush & 0xFF);
            };

            // POP rp/PSW (Pattern: 1 1 R P 0 0 0 1)
            bytes[0xC1 | (rp << 4)] = 1;
            table[0xC1 |(rp << 4)] = (cpu) -> {
                int low = cpu.readMemory(cpu.sp);
                cpu.sp = (cpu.sp + 1) & 0xFFFF;

                int high = cpu.readMemory(cpu.sp);
                cpu.sp = (cpu.sp + 1) & 0xFFFF;

                if (rp == 3) {
                    cpu.a = high;
                    cpu.setPSW(low);
                } else {
                    setPairValue(cpu, rp, (high << 8) | low);
                }
            };

            if (rp < 2) {
                // STAX rp (Pattern: 0 0 R P 0 0 1 0)
                bytes[0x02 | (rp << 4)] = 1;
                table[0x02 | (rp << 4)] = (cpu) -> {
                    int address = getPairValue(cpu, rp);
                    int accumulator = getRegValue(cpu, 7);
                    cpu.writeMemory(address, accumulator);
                };

                // LDAX rp (Pattern: 0 0 R P 1 0 1 0)
                bytes[0x0A | (rp << 4)] = 1;
                table[0x0A | (rp << 4)] = (cpu) -> {
                    int address = getPairValue(cpu, rp);
                    int value = cpu.readMemory(address);
                    setRegValue(cpu, 7, value);
                };
            }
        }

        // =========================================================
        // 4. CONDITIONAL BRANCHING & RST (Loop: condition flag)
        // =========================================================
        // Conditions: 0=NZ, 1=Z, 2=NC, 3=C, 4=PO, 5=PE, 6=P, 7=M
        for (int i = 0; i < 8; i++) {
            final int condition = i;

            // Conditional RET (Pattern: 1 1 C C C 0 0 0)
            int retOpcode = 0xC0 | (condition << 3);
            bytes[retOpcode] = 1;
            table[retOpcode] = (cpu) -> {
                if (evaluateCondition(cpu, condition)) {
                    int low = cpu.readMemory(cpu.sp);
                    cpu.sp = (cpu.sp + 1) & 0xFFFF;
                    int high = cpu.readMemory(cpu.sp);
                    cpu.sp = (cpu.sp + 1) & 0xFFFF;

                    cpu.pc = (high << 8) | low;
                    cpu.jumped = true;
                }
            };

            // Conditional JMP (Pattern: 1 1 C C C 0 1 0)
            int jumpOpcode = 0xC2 | (condition << 3);
            bytes[jumpOpcode] = 3;
            table[jumpOpcode] = (cpu) -> {
                if (evaluateCondition(cpu, condition)) {
                    cpu.pc = cpu.readMemory(cpu.pc + 1) | (cpu.readMemory(cpu.pc + 2) << 8);
                    cpu.jumped = true;
                }
            };

            // Conditional CALL (Pattern: 1 1 C C C 1 0 0)
            int callOpcode = 0xC4 | (condition << 3);
            bytes[callOpcode] = 3;
            table[callOpcode] = (cpu) -> {
                if (evaluateCondition(cpu, condition)) {
                    int target = cpu.readMemory(cpu.pc + 1) | (cpu.readMemory(cpu.pc + 2) << 8);
                    int returnAddress = cpu.pc + 3;

                    // PUSH return address to stack
                    cpu.sp = (cpu.sp - 1) & 0xFFFF;
                    cpu.writeMemory(cpu.sp, (returnAddress >> 8) & 0xFF);
                    cpu.sp = (cpu.sp - 1) & 0xFFFF;
                    cpu.writeMemory(cpu.sp, returnAddress & 0xFF);

                    cpu.pc = target;
                    cpu.jumped = true;
                }
            };

            // RST 0-7 Vectors (Pattern: 1 1 C C C 1 1 1)
            int rstOpcode = 0xC7 | (condition << 3);
            bytes[rstOpcode] = 1;
            table[rstOpcode] = (cpu) -> {
                int returnAddress = cpu.pc + 1;

                cpu.sp = (cpu.sp - 1) & 0xFFFF;
                cpu.writeMemory(cpu.sp, (returnAddress >> 8) & 0xFF);
                cpu.sp = (cpu.sp - 1) & 0xFFFF;
                cpu.writeMemory(cpu.sp, returnAddress & 0xFF);

                cpu.pc = condition * 8;
                cpu.jumped = true;
            };
        }

        // =========================================================
        // 5. EXCEPTIONS (Non-Looped, Hardcoded Opcodes)
        // =========================================================

        // --- Immediate Arithmetic & Logic ---

        // ADI Data (Opcode: 0xC6)
        bytes[0xC6] = 2;
        table[0xC6] = (cpu) -> {
            int immediateData = cpu.readMemory(cpu.pc + 1);
            doAdd(cpu, immediateData, 0);
        };

        // ACI Data (Opcode: 0xCE)
        bytes[0xCE] = 2;
        table[0xCE] = (cpu) -> {
            int immediateData = cpu.readMemory(cpu.pc + 1);
            int carryIn = cpu.flagCY ? 1 : 0;
            doAdd(cpu, immediateData, carryIn);
        };

        // SUI Data (Opcode: 0xD6)
        bytes[0xD6] = 2;
        table[0xD6] = (cpu) -> {
            int immediateData = cpu.readMemory(cpu.pc + 1);
            doSub(cpu, immediateData, 0);
        };

        // SBI Data (Opcode: 0xDE)
        bytes[0xDE] = 2;
        table[0xDE] = (cpu) -> {
            int immediateData = cpu.readMemory(cpu.pc + 1);
            int borrowIn = cpu.flagCY ? 1 : 0;
            doSub(cpu, immediateData, borrowIn);
        };

        // ANI Data (Opcode: 0xE6)
        bytes[0xE6] = 2;
        table[0xE6] = (cpu) -> {
            int immediateData = cpu.readMemory(cpu.pc + 1);
            doAnd(cpu, immediateData);
        };

        // XRI Data (Opcode: 0xEE)
        bytes[0xEE] = 2;
        table[0xEE] = (cpu) -> {
            int immediateData = cpu.readMemory(cpu.pc + 1);
            doXor(cpu, immediateData);
        };

        // ORI Data (Opcode: 0xF6)
        bytes[0xF6] = 2;
        table[0xF6] = (cpu) -> {
            int immediateData = cpu.readMemory(cpu.pc + 1);
            doOr(cpu, immediateData);
        };

        // CPI Data (Opcode: 0xFE)
        bytes[0xFE] = 2;
        table[0xFE] = (cpu) -> {
            int immediateData = cpu.readMemory(cpu.pc + 1);
            doCmp(cpu, immediateData);
        };

        // --- Accumulator & Flag Controls ---

        // CMA - Complement Accumulator (Opcode: 0x2F)
        bytes[0x2F] = 1;
        table[0x2F] = (cpu) -> {
            cpu.a = (~cpu.a) & 0xFF;
        };

        // DAA - Decimal Adjust Accumulator (Opcode: 0x27)
        bytes[0x27] = 1;
        table[0x27] = (cpu) -> {
            int res = cpu.a;
            if ((res & 0x0F) > 9 || cpu.flagAC) {
                res += 6;
                cpu.flagAC = ((cpu.a ^ res) & 0x10) != 0;
            }
            if ((res > 0x9F) || cpu.flagCY) {
                res += 0x60;
                cpu.flagCY = true;
            }
            setZSP(cpu, res & 0xFF);
            cpu.a = res & 0xFF;
        };

        // STC - Set Carry (Opcode: 0x37)
        bytes[0x37] = 1;
        table[0x37] = (cpu) -> {
            cpu.flagCY = true;
        };

        // CMC - Complement Carry (Opcode: 0x3F)
        bytes[0x3F] = 1;
        table[0x3F] = (cpu) -> {
            cpu.flagCY = !cpu.flagCY;
        };

        // --- Rotates ---

        // RLC - Rotate Left Circular (Opcode: 0x07)
        bytes[0x07] = 1;
        table[0x07] = (cpu) -> {
            int bit = (cpu.a >> 7) & 1;
            cpu.a = ((cpu.a << 1) | bit) & 0xFF;
            cpu.flagCY = (bit == 1);
        };

        // RRC - Rotate Right Circular (Opcode: 0x0F)
        bytes[0x0F] = 1;
        table[0x0F] = (cpu) -> {
            int bit = cpu.a & 1;
            cpu.a = ((cpu.a >> 1) | (bit << 7)) & 0xFF;
            cpu.flagCY = (bit == 1);
        };

        // RAL - Rotate Left through Carry (Opcode: 0x17)
        bytes[0x17] = 1;
        table[0x17] = (cpu) -> {
            int bit = (cpu.a >> 7) & 1;
            cpu.a = ((cpu.a << 1) | (cpu.flagCY ? 1 : 0)) & 0xFF;
            cpu.flagCY = (bit == 1);
        };

        // RAR - Rotate Right through Carry (Opcode: 0x1F)
        bytes[0x1F] = 1;
        table[0x1F] = (cpu) -> {
            int bit = cpu.a & 1;
            cpu.a = ((cpu.a >> 1) | ((cpu.flagCY ? 1 : 0) << 7)) & 0xFF;
            cpu.flagCY = (bit == 1);
        };

        // --- Unconditional Branching ---

        // JMP (Opcode: 0xC3)
        bytes[0xC3] = 3;
        table[0xC3] = (cpu) -> {
            cpu.pc = (cpu.readMemory(cpu.pc + 1) | (cpu.readMemory(cpu.pc + 2) << 8));
            cpu.jumped = true;
        };

        // RET (Opcode: 0xC9)
        bytes[0xC9] = 1;
        table[0xC9] = (cpu) -> {
            int low = cpu.readMemory(cpu.sp);
            cpu.sp = (cpu.sp + 1) & 0xFFFF;
            int high = cpu.readMemory(cpu.sp);
            cpu.sp = (cpu.sp + 1) & 0xFFFF;

            cpu.pc = (high << 8) | low;
            cpu.jumped = true;
        };

        // CALL (Opcode: 0xCD)
        bytes[0xCD] = 3;
        table[0xCD] = (cpu) -> {
            int target = cpu.readMemory(cpu.pc + 1) | (cpu.readMemory(cpu.pc + 2) << 8);
            int returnAddress = cpu.pc + 3;

            cpu.sp = (cpu.sp - 1) & 0xFFFF;
            cpu.writeMemory(cpu.sp, (returnAddress >> 8) & 0xFF);
            cpu.sp = (cpu.sp - 1) & 0xFFFF;
            cpu.writeMemory(cpu.sp, returnAddress & 0xFF);

            cpu.pc = target;
            cpu.jumped = true;
        };

        // PCHL - Jump to HL (Opcode: 0xE9)
        bytes[0xE9] = 1;
        table[0xE9] = (cpu) -> {
            cpu.pc = getPairValue(cpu, 2);
            cpu.jumped = true;
        };

        // --- Memory & Register Pair Swaps ---

        // LDA - Load Accumulator Direct (Opcode: 0x3A)
        bytes[0x3A] = 3;
        table[0x3A] = (cpu) -> {
            int address = cpu.readMemory(cpu.pc + 1) | (cpu.readMemory(cpu.pc + 2) << 8);
            setRegValue(cpu, 7, cpu.readMemory(address)); // 7 is Reg A
        };

        // STA - Store Accumulator Direct (Opcode: 0x32)
        bytes[0x32] = 3;
        table[0x32] = (cpu) -> {
            int address = cpu.readMemory(cpu.pc + 1) | (cpu.readMemory(cpu.pc + 2) << 8);
            cpu.writeMemory(address, cpu.a);
        };

        // LHLD - Load HL Direct (Opcode: 0x2A)
        bytes[0x2A] = 3;
        table[0x2A] = (cpu) -> {
            int address = cpu.readMemory(cpu.pc + 1) | (cpu.readMemory(cpu.pc + 2) << 8);
            setRegValue(cpu, 5, cpu.readMemory(address));     // Reg L
            setRegValue(cpu, 4, cpu.readMemory(address + 1)); // Reg H
        };

        // SHLD - Store HL Direct (Opcode: 0x22)
        bytes[0x22] = 3;
        table[0x22] = (cpu) -> {
            int address = cpu.readMemory(cpu.pc + 1) | (cpu.readMemory(cpu.pc + 2) << 8);
            cpu.writeMemory(address, getRegValue(cpu, 5));     // Store L
            cpu.writeMemory(address + 1, getRegValue(cpu, 4)); // Store H
        };

        // XCHG - Exchange DE and HL (Opcode: 0xEB)
        bytes[0xEB] = 1;
        table[0xEB] = (cpu) -> {
            int tempH = cpu.h;
            int tempL = cpu.l;
            cpu.h = cpu.d;
            cpu.l = cpu.e;
            cpu.d = tempH;
            cpu.e = tempL;
        };

        // XTHL - Exchange Stack Top with HL (Opcode: 0xE3)
        bytes[0xE3] = 1;
        table[0xE3] = (cpu) -> {
            int low = cpu.readMemory(cpu.sp);
            int high = cpu.readMemory((cpu.sp + 1) & 0xFFFF);
            cpu.writeMemory(cpu.sp, cpu.l);
            cpu.writeMemory((cpu.sp + 1) & 0xFFFF, cpu.h);
            cpu.l = low;
            cpu.h = high;
        };

        // SPHL - Move HL to SP (Opcode: 0xF9)
        bytes[0xF9] = 1;
        table[0xF9] = (cpu) -> {
            cpu.sp = getPairValue(cpu, 2);
        };

        // --- Hardware I/O Group ---

        // IN - Input Port (Opcode: 0xDB)
        bytes[0xDB] = 2;
        table[0xDB] = (cpu) -> {
            int port = cpu.readMemory(cpu.pc + 1);
            // IN port implementation placeholder (returns 0 or hooks to your I/O subsystem)
            setRegValue(cpu, 7, 0x00);
        };

        // OUT - Output Port (Opcode: 0xD3)
        bytes[0xD3] = 2;
        table[0xD3] = (cpu) -> {
            int port = cpu.readMemory(cpu.pc + 1);
            // OUT port implementation placeholder (sends cpu.a value to your I/O subsystem)
        };

        // --- Interrupts ---

        // DI - Disable Interrupts (Opcode: 0xF3)
        bytes[0xF3] = 1;
        table[0xF3] = (cpu) -> {
            cpu.interruptEnabled = false;
        };

        // EI - Enable Interrupts (Opcode: 0xFB)
        bytes[0xFB] = 1;
        table[0xFB] = (cpu) -> {
            cpu.interruptEnabled = true;
        };

        // RIM - Read Interrupt Mask (Opcode: 0x20)
        bytes[0x20] = 1;
        table[0x20] = (cpu) -> {
            int rimByte = 0x00;
            // Bits 0-2: Current restart interrupt masks
            rimByte |= (cpu.interruptMasks & 0x07);
            // Bit 3: Interrupt Enable status
            if (cpu.interruptEnabled) rimByte |= 0x08;

            setRegValue(cpu, 7, rimByte); // Load into Reg A
        };

        // SIM - Set Interrupt Mask (Opcode: 0x30)
        bytes[0x30] = 1;
        table[0x30] = (cpu) -> {
            int regA = getRegValue(cpu, 7);
            if ((regA & 0x08) != 0) {
                cpu.interruptMasks = regA & 0x07;
            }
        };

        // --- Core Machine States ---

        // NOP - No Operation (Opcode: 0x00)
        bytes[0x00] = 1;
        table[0x00] = (cpu) -> {
            // NOP
        };

        // HLT - Halt (Opcode: 0x76)
        bytes[0x76] = 1;
        table[0x76] = (cpu) -> {
            cpu.isHalted = true;
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
        cpu.flagAC = ((val ^ twosComp ^ aluAddn) & 0x10) != 0;
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

    private boolean evaluateCondition(CPU cpu, int conditionCode) {
        switch (conditionCode) {
            case 0: return !cpu.flagZ;   // NZ
            case 1: return cpu.flagZ;    // Z
            case 2: return !cpu.flagCY;  // NC
            case 3: return cpu.flagCY;   // C
            case 4: return !cpu.flagP;   // PO
            case 5: return cpu.flagP;    // PE
            case 6: return !cpu.flagS;   // P (Positive)
            case 7: return cpu.flagS;    // M (Minus)
            default: return false;
        }
    }
}