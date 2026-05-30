package studio.ide.emulator;

public class Memory {
        public final byte[] memory = new byte[65536];

        public Memory() {}

        public void write(int address, byte value) {
            memory[address & 0xFFFF] = value;
        }

        public byte read(int address) {
            return memory[address & 0xFFFF];
        }
}