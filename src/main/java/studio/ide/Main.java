package studio.ide;

import studio.ide.assembler.Assembler;
import studio.ide.emulator.CPU;
import studio.ide.emulator.Memory;

import java.sql.SQLOutput;

public class Main {
    public static void main(String[] args) {
        String program =
                "        MVI B, 05H    ; Load counter register B with 5\n"
                + "        MVI A, 00H    ; Clear accumulator A to 0\n"
                + "LOOP:   ADI 02H       ; Add 2 to Accumulator A\n"
                + "        DCR B         ; Decrement counter B\n"
                + "        JNZ LOOP      ; If B is not zero, jump back to LOOP\n"
                + "        HLT           ; Halt execution when done\n";

        System.out.println("==========================================");
        System.out.println("   Starting 8085 Assembler & Emulator     ");
        System.out.println("==========================================\n");

        Assembler assembler = new Assembler();
        System.out.println("[Assembler] compiling source code...");
        byte[] machineCode = assembler.assemble(program);
        System.out.println("[Aseembler] Compilation successful! generated " + machineCode.length + " bytes. \n");

        Memory memory = new Memory();
        CPU cpu = new CPU(memory);

        System.out.println("[Memory] loadinng binary into Ram at 0x0000");
        for(int i = 0; i < machineCode.length; i++) {
            int unsignByte = machineCode[i] & 0xFF;
            memory.write(i, (byte) unsignByte);
        }

        System.out.println("[CPU] Booting execution engine...\n");
        System.out.println("Tracing Execution Matrix:");
        System.out.println("----------------------------------------------------------------");
        System.out.printf("%-6s | %-6s | %-6s | %-6s | %-5s | %-5s | %-5s\n",
                "PC", "Opcode", "Reg A", "Reg B", "FlagZ", "FlagS", "FlagCY");
        System.out.println("----------------------------------------------------------------");

        boolean running = true;
        int safetyCounter = 0;

        while(running) {
            int currentPC = cpu.pc;
            int opcode = cpu.readMemory(currentPC);

            if(opcode == 0x76) {
                System.out.printf("0x%04X | 0x%02X   | [HALT EXECUTED]\n", currentPC, opcode);
                running = false;
                break;
            }

            cpu.step();

            System.out.printf("0x%04X | 0x%02X   | 0x%02X   | 0x%02X   | %-5b | %-5b | %-5b\n",
                    currentPC, opcode, cpu.a, cpu.b, cpu.flagZ, cpu.flagS, cpu.flagCY);

            safetyCounter++;
            if (safetyCounter > 1000) {
                System.out.println("\n[ERROR] execution safety threshold exceeded. Potential infinite loop!");
                break;
            }
        }

        System.out.println("----------------------------------------------------------------");
        System.out.println("\n==========================================");
        System.out.println("          Execution Verification          ");
        System.out.println("==========================================");
        System.out.printf("Final Accumulator (Register A): 0x%02X (%d)\n", cpu.a, cpu.a);
        System.out.printf("Final Counter     (Register B): 0x%02X (%d)\n", cpu.b, cpu.b);

        if (cpu.a == 10) {
            System.out.println(">>> SUCCESS: 5 iterations x 2 = 10. Your entire system works flawlessly! <<<");
        } else {
            System.out.println(">>> FAILURE: Output value incorrect. Check Pass 2 or CPU loop logic. <<<");
        }
        System.out.println("==========================================");
    }
}
