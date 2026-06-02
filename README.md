# 8085 Studio opCode

8085 Studio opCode is an Intel 8085 Microprocessor Emulator and Integrated Development Environment (IDE) built using Java and JavaFX. This project simulates the core hardware architecture of the 8085 microprocessor, allowing users to write, execute, and analyze 8085 assembly instructions with a graphical user interface.

## Features

- **Microprocessor Emulation**: Accurately simulates the Intel 8085 CPU, including the Program Counter (PC), Stack Pointer (SP), general-purpose registers (A, B, C, D, E, H, L), and the condition flags (Zero, Sign, Parity, Carry, Auxiliary Carry).
- **Instruction Dispatch Table**: Uses a highly efficient dispatch table pattern to map and execute hexadecimal opcodes directly to lambda functions, avoiding massive switch statements.
- **Memory Management**: Simulates the standard 64KB (65536 bytes) RAM of the 8085 architecture.
- **Graphical User Interface**: Built with JavaFX, featuring a File Explorer, and a real-time monitor for Registers & Flags to visualize the state of the CPU during execution.

## Technologies Used

- **Java**: Core logic for the emulator backend (CPU, Memory, Instruction Set).
- **JavaFX**: Graphical User Interface frontend.
- **Maven**: Dependency management and build tool.

## Project Structure

The codebase is modularly designed, separating the UI from the emulator backend:

- `studio.ide.emulator.Memory`: Simulates the 64KB physical RAM array.
- `studio.ide.emulator.CPU`: Glues the Memory and Instruction Set together, keeping track of registers and executing instruction cycles.
- `studio.ide.emulator.InstructionSet`: Contains the core logic for all 8085 opcodes, wired up via a dispatch table mapping hex values to executable lambda functions.
- `studio.ide.HelloApplication`: The JavaFX entry point that loads the FXML UI.
- `studio.ide.HelloController`: The bridge between the JavaFX frontend and the CPU emulator backend.

## How to Run

1. Ensure you have **Java** (JDK 11 or higher) and **Maven** installed.
2. Clone the repository.
3. Open the project in your IDE (e.g., IntelliJ IDEA).
4. Run the application via `HelloApplication.java` or using the Maven wrapper:
   ```bash
   ./mvnw javafx:run
   ```
