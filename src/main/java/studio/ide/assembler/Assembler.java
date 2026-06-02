package studio.ide.assembler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Assembler {

    // --- State Variables ---

    // The Symbol Table: Maps a String label (like "LOOP:") to its 16-bit memory address (like 0x2050).
    // Populated during Pass 1, consumed during Pass 2 to resolve jumps/calls.
    private final Map<String, Integer> symbolTable;

    // Caches the clean, parsed instructions (comments and whitespace removed)
    // so Pass 2 doesn't have to re-read and re-parse strings.
    private final List<ParsedLine> parsedLines;

    // Holds the final raw hex data before converting to a byte[].
    // We use Integer instead of byte to avoid Java's signed-byte precision errors during math.
    private final List<Integer> machineCode;

    // Tracks our simulated memory address as we count instruction sizes to calculate where labels live.
    private int currentAddress;

    public Assembler() {
        symbolTable = new HashMap<>();
        parsedLines = new ArrayList<>();
        machineCode = new ArrayList<>();
        currentAddress = 0x0000; // Default starting address
    }

    // --- The Main Public API ---
    // Your IDE UI will call this method when the user clicks "Run" or "Build"
    public byte[] assemble(String sourceCode) {

        // 1. Clear out old data from the last time the user clicked "Run"
        symbolTable.clear();
        parsedLines.clear();
        machineCode.clear();
        currentAddress = 0x0000;

        // 2. Break the giant text editor string into a manageable array of individual lines
        String[] lines = sourceCode.split("\n");

        // 3. Pass 1: The Scout
        // Parse the strings, count the bytes, and map the label addresses into the symbol table.
        passOne(lines);

        // 4. Pass 2: The Translator (Colleague Implementation)
        // Uses the cached data from Pass 1 to translate the clean lines into hex opcodes.
        passTwo();

        // 5. Compress the Integer list into the final, raw binary byte[] payload for your CPU memory.
        return generateByteArray();
    }

    // --- The Pipeline Methods ---

    private void passOne(String[] rawLines) {
        for (String rawLine : rawLines) {

            // Send the messy string to our Lexer funnel to get a clean object
            ParsedLine line = parseLine(rawLine);

            // If it was just an empty line or purely a comment, skip it completely
            if (line == null) continue;

            // Did the Lexer find a label? Save it to the symbol table with the current address
            if (line.label != null && !line.label.isEmpty()) {
                symbolTable.put(line.label, currentAddress);
            }

            // Is there an actual instruction on this line? (Not just a floating label)
            if (line.mnemonic != null) {
                // Save it for Pass 2 so we never have to parse this string again
                parsedLines.add(line);

                // Reconstruct the string (e.g., "MOV A, B") to look it up in the Dictionary
                String lookupString = buildLookupString(line);
                int[] data = InstructionMap.getInstructionData(lookupString);

                if (data != null) {
                    // data[1] holds the byte count (e.g., MVI = 2 bytes, MOV = 1 byte).
                    // Advance our address counter so the next instruction gets the right memory slot!
                    currentAddress += data[1];
                } else {
                    // In a polished IDE, you would throw a custom exception here to highlight the text red
                    System.err.println("Syntax Error on Pass 1: Unknown instruction -> " + lookupString);
                }
            }
        }
    }

    private void passTwo() {
        // =====================================================================
        // --- PASS 2: COLLEAGUE HANDOFF POINT ---
        //
        // Objective: Iterate through the 'parsedLines' list and populate the 'machineCode' list.
        //
        // Available Resources:
        // 1. List<ParsedLine> parsedLines:
        //    - Contains clean objects (line.mnemonic, line.operand1, line.operand2).
        // 2. Map<String, Integer> symbolTable:
        //    - Use this to resolve jump addresses (e.g., symbolTable.get("LOOP") returns 0x2050).
        // 3. InstructionMap.getInstructionData(buildLookupString(line)):
        //    - Returns an int[] array -> [0] is the Hex Opcode, [1] is the total Byte Count.
        //
        // Important: For 16-bit addresses, remember that the 8085 is Little-Endian!
        // The lower byte gets added to 'machineCode' first, followed by the upper byte.
        // =====================================================================

        // TODO: Implement Pass 2 loop here.
        for(ParsedLine line : parsedLines) {
            String lookup = buildLookupString(line);
            int[] data = InstructionMap.getInstructionData(lookup);

            if(data == null) {
                System.err.println("Syntax Error on pass2 : invalid instruction -> " + lookup);
                continue;
            }

            int opcode = data[0];
            int instructionSize = data[1];

            machineCode.add(opcode);

            if(instructionSize > 1) {
                String rawOpearand = (line.operand2 != null && !line.operand2.isEmpty()) ? line.operand2 : line.operand1;

                int resolvedValue = parseOperandValue(rawOpearand);

                if(instructionSize == 2) {
                    // 2-Byte Instruction: Append the single 8-bit literal byte directly
                    machineCode.add(resolvedValue & 0xFF);
                }
                else if(instructionSize == 3) {
                    // 3-Byte Instruction: Break 16-bit address using Little-Endian rules
                    int lowByte = resolvedValue & 0xFF;
                    int highByte = (resolvedValue >> 8) & 0xFF;

                    machineCode.add(lowByte);
                    machineCode.add(highByte);
                }
            }
        }
    }

    // --- The Lexer ---
    private ParsedLine parseLine(String rawLine) {
        // 1. Slice off comments immediately
        int commentIndex = rawLine.indexOf(';');
        if (commentIndex != -1) {
            rawLine = rawLine.substring(0, commentIndex);
        }

        // 2. Standardize casing and trim edge spaces
        String cleanLine = rawLine.trim().toUpperCase();
        if (cleanLine.isEmpty()) return null;

        // 3. Hunt for labels (indicated by a colon)
        String label = null;
        int colonIndex = cleanLine.indexOf(':');
        if (colonIndex != -1) {
            // Extract label without the colon
            label = cleanLine.substring(0, colonIndex).trim();
            // Delete the label from the main instruction string
            cleanLine = cleanLine.substring(colonIndex + 1).trim();
        }

        // Return early if the line was just a label and nothing else
        if (cleanLine.isEmpty()) {
            return new ParsedLine(label, null, null, null);
        }

        // 4. Regex Split: Chops the string wherever a space or comma occurs
        String[] parts = cleanLine.split("[,\\s]+");

        // 5. Unpack the array safely to avoid OutOfBounds errors
        String mnemonic = parts.length > 0 ? parts[0] : null;
        String op1 = parts.length > 1 ? parts[1] : null;
        String op2 = parts.length > 2 ? parts[2] : null;

        return new ParsedLine(label, mnemonic, op1, op2);
    }

    // --- Utility Helpers ---
    
    // 1. Define the universal list of valid 8085 registers and pairs
    private static final List<String> VALID_REGISTERS = List.of(
            "A", "B", "C", "D", "E", "H", "L", "M", "SP", "PSW"
    );

    // 2. A quick helper to classify the operand
    private boolean isRegister(String operand) {
        return operand != null && VALID_REGISTERS.contains(operand);
    }

    // Bridges the gap between the ParsedLine object and the InstructionMap keys
    private String buildLookupString(ParsedLine line) {

        // The True Exception: RST bakes the number directly into the opcode
        if (line.mnemonic.equals("RST")) {
            return "RST " + line.operand1;
        }

        // The Standard Filter for the other 95% of instructions
        StringBuilder lookup = new StringBuilder(line.mnemonic);

        if (isRegister(line.operand1)) {
            lookup.append(" ").append(line.operand1);
        }

        if (isRegister(line.operand2)) {
            lookup.append(", ").append(line.operand2);
        }

        return lookup.toString();
    }

    // Safely transforms our dynamic Integer list into a strict Java primitive byte array
    private byte[] generateByteArray() {
        byte[] finalBytes = new byte[machineCode.size()];
        for (int i = 0; i < machineCode.size(); i++) {
            // byteValue() crushes the int down, simulating hardware overflow natively
            finalBytes[i] = machineCode.get(i).byteValue();
        }
        return finalBytes;
    }

    private int parseOperandValue(String operand) {
        if (operand == null || operand.isEmpty()) {
            return 0;
        }

        // 1. Is it a text label sitting in our Symbol Table?
        if (symbolTable.containsKey(operand)) {
            return symbolTable.get(operand);
        }

        // 2. Is it a Hexadecimal constant? (e.g., "55H", "2000H")
        if (operand.endsWith("H")) {
            String cleanHex = operand.substring(0, operand.length() - 1);
            return Integer.parseInt(cleanHex, 16);
        }

        // 3. Otherwise, treat it as a standard decimal integer base-10
        try {
            return Integer.parseInt(operand);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Assembler Error: Unresolved literal token -> " + operand);
        }
    }
}