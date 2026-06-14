import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;

/**
 * Command-line entry point for testing the J2RV lexer.
 *
 * <p>Reads the configured source file, scans it into tokens, and prints each
 * token to standard output.
 */
public class Main {
    private static final String SOURCE_FILE = "test.J2RV.txt";

    /**
     * Runs the lexer against {@link #SOURCE_FILE}.
     *
     * @param args unused; kept for the standard Java entry point signature
     */
    public static void main(String[] args) {
        try {
            String source = readSourceFile(SOURCE_FILE);

            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.scanTokens();

            for (Token token : tokens) {
                System.out.println(token);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Could not read source file: " + e.getMessage());
        }
    }

    /**
     * Reads an entire source file using Java 8-compatible Scanner APIs.
     *
     * @param fileName name of the source file to read
     * @return full source text
     * @throws FileNotFoundException when the configured file cannot be opened
     */
    private static String readSourceFile(String fileName) throws FileNotFoundException {
        StringBuilder source = new StringBuilder();

        try (Scanner scanner = new Scanner(new File(fileName), "UTF-8")) {
            while (scanner.hasNextLine()) {
                source.append(scanner.nextLine()).append('\n');
            }
        }

        return source.toString();
    }
}
