package studio.ide.assembler;
import java.util.Map;
import java.util.HashMap;


public class InstructionMap {

    private static final Map<String, int[]> map = new HashMap<>();

    static  {

        String reg[] = {
                "B", "C", "D", "E", "H", "L", "M", "A"
        };

        String regPair[] = {
                "B", "D", "H", "SP"
        };

        String flags[] = {
                "NZ", "Z", "NC", "C", "PO", "PE", "P", "M"
        };

        //MOV and HLT 64
       for (int dest = 0; dest <= 7; ++dest) {
           for (int src = 0; src <= 7; ++src) {
               if (dest == 6 && src == 6) map.put("HLT", new int[]{0x76, 1});
               int opcode = 0x40 | (dest << 3) | src;
               map.put("MOV " + reg[dest] + ", " + reg[src], new int[]{opcode, 1});
           }
       }

       //ALL ARITHMETIC : ADD, ADI, SUB, SBB, ANA, ORA, XRA, CMP ; 64
       //INR DCR 16
       //MVI 8
       //Total 88
       for (int regCode = 0; regCode < 8; ++regCode) {
           map.put("ADD " + reg[regCode], new int[]{0x80 | regCode, 1});
           map.put("ADC " + reg[regCode], new int[]{0x88 | regCode, 1});
           map.put("SUB " + reg[regCode], new int[]{0x90 | regCode, 1});
           map.put("SBB " + reg[regCode], new int[]{0x98 | regCode, 1});
           map.put("ANA " + reg[regCode], new int[]{0xA0 | regCode, 1});
           map.put("XRA " + reg[regCode], new int[]{0xA8 | regCode, 1});
           map.put("ORA " + reg[regCode], new int[]{0xB0 | regCode, 1});
           map.put("CMP " + reg[regCode], new int[]{0xB8 | regCode, 1});
           map.put("INR " + reg[regCode], new int[]{0x04 | (regCode << 3), 1});
           map.put("DCR " + reg[regCode], new int[]{0x05 | (regCode << 3), 1});
           map.put("MVI " + reg[regCode], new int[]{0x06 | (regCode << 3), 2});
       }


       //10
       map.put("CMA", new int[]{0x2F, 1});
       map.put("ADI", new int[]{0xC6, 2});
       map.put("ACI", new int[]{0xCE, 2});
       map.put("SUI", new int[]{0xD6, 2});
       map.put("SBI", new int[]{0xDE, 2});
       map.put("ANI", new int[]{0xE6, 2});
       map.put("XRI", new int[]{0xEE, 2});
       map.put("ORI", new int[]{0xF6, 2});
       map.put("CPI", new int[]{0xFE, 2});
       map.put("JMP", new int[]{0xC3, 3});


       // Register Pair Instructions: LXI, INX, DCX, STAX, LDAX, PUSH, POP 28
        for (int rp = 0; rp < 4; rp++) {

            // LXI rp, data16 (Pattern: 00 RP 0001) -> 3 bytes
            map.put("LXI " + regPair[rp], new int[]{(rp << 4) | 0x01, 3});

            // INX rp (Pattern: 00 RP 0011) -> 1 byte
            map.put("INX " + regPair[rp], new int[]{(rp << 4) | 0x03, 1});

            // DCX rp (Pattern: 00 RP 1011) -> 1 byte
            map.put("DCX " + regPair[rp], new int[]{(rp << 4) | 0x0B, 1});

            // DAD rp (Pattern: 00 RP 1001) -> 1 byte
            map.put("DAD " + regPair[rp], new int[]{(rp << 4) | 0x09, 1});

            // STAX and LDAX only exist for B (rp=0) and D (rp=1)
            if (rp < 2) {
                // STAX rp (Pattern: 00 RP 0010) -> 1 byte
                map.put("STAX " + regPair[rp], new int[]{(rp << 4) | 0x02, 1});

                // LDAX rp (Pattern: 00 RP 1010) -> 1 byte
                map.put("LDAX " + regPair[rp], new int[]{(rp << 4) | 0x0A, 1});
            }

            // PUSH and POP use "PSW" instead of "SP" for the 4th pair position (rp=3)
            String pushPopReg = (rp == 3) ? "PSW" : regPair[rp];

            // PUSH rp (Pattern: 11 RP 0101) -> 1 byte
            map.put("PUSH " + pushPopReg, new int[]{0xC0 | (rp << 4) | 0x05, 1});

            // POP rp (Pattern: 11 RP 0001) -> 1 byte
            map.put("POP " + pushPopReg, new int[]{0xC0 | (rp << 4) | 0x01, 1});
        }

        // Conditional Branching: RET, JMP, CALL 24
        for (int ccc = 0; ccc < 8; ccc++) {

            // Conditional RET (Pattern: 11 CCC 000) -> 1 byte
            map.put("R" + flags[ccc], new int[]{0xC0 | (ccc << 3), 1});

            // Conditional JMP (Pattern: 11 CCC 010) -> 3 bytes
            map.put("J" + flags[ccc], new int[]{0xC2 | (ccc << 3), 3});

            // Conditional CALL (Pattern: 11 CCC 100) -> 3 bytes
            map.put("C" + flags[ccc], new int[]{0xC4 | (ccc << 3), 3});
        }

        //RST 0-7 Opcode 1 1 X X X 1 1 1
        for (int n = 0; n < 8; n++) {
            map.put("RST " + n, new int[]{0xC7 | (n << 3), 1});
        }

        // --- Absolute Branching ---
        map.put("CALL", new int[]{0xCD, 3});
        map.put("RET", new int[]{0xC9, 1});
        // (Note: JMP 0xC3 is already in your code!)

        // --- 16-bit Direct Addressing & Data Transfer ---
        map.put("STA", new int[]{0x32, 3});
        map.put("LDA", new int[]{0x3A, 3});
        map.put("SHLD", new int[]{0x22, 3});
        map.put("LHLD", new int[]{0x2A, 3});

        // --- Register & Stack Pointer Swaps ---
        map.put("XCHG", new int[]{0xEB, 1});
        map.put("XTHL", new int[]{0xE3, 1});
        map.put("SPHL", new int[]{0xF9, 1});
        map.put("PCHL", new int[]{0xE9, 1});

        // --- Carry Flag & Interrupt State Control ---
        map.put("STC", new int[]{0x37, 1});
        map.put("CMC", new int[]{0x3F, 1});
        map.put("EI", new int[]{0xFB, 1});
        map.put("DI", new int[]{0xF3, 1});
        map.put("SIM", new int[]{0x20, 1});
        map.put("RIM", new int[]{0x28, 1});

        // --- External I/O Ports ---
        map.put("IN", new int[]{0xDB, 2});
        map.put("OUT", new int[]{0xD3, 2});

        // --- No Operation ---
        map.put("NOP", new int[]{0x00, 1});

        // --- Accumulator Rotates & Decimal Adjust (Pattern: 00 NNN 111) ---
        map.put("RLC", new int[]{0x07, 1});
        map.put("RRC", new int[]{0x0F, 1});
        map.put("RAL", new int[]{0x17, 1});
        map.put("RAR", new int[]{0x1F, 1});
        map.put("DAA", new int[]{0x27, 1});
    }

    public static int[] getInstructionData(String instructionStr) {
        return map.get(instructionStr);
    }



}
