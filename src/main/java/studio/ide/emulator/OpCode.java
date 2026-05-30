package studio.ide.emulator;

@FunctionalInterface
public interface OpCode {

    void execute(CPU cpu);

}