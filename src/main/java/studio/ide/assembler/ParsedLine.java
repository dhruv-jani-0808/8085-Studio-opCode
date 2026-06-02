package studio.ide.assembler;

public class ParsedLine {

    public String label, mnemonic, operand1, operand2;

    public ParsedLine(String l, String m, String o1, String o2) {
        label = l;
        mnemonic = m;
        operand1 = o1;
        operand2 = o2;
    }
}