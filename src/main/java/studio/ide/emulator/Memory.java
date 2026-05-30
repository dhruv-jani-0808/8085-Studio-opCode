package studio.ide.emulator;

public class Memory {
        private final byte[] memory = new byte[65536];

        Memory() {}

        public void write(int address, byte value) {
            memory[address & 0xFFFF] = value;
        }

        public byte read(int address) {
            return memory[address & 0xFFFF];
        }
}