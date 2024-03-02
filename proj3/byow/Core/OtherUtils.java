package byow.Core;

import edu.princeton.cs.introcs.StdDraw;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class OtherUtils {
    private static char[] numbers = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    private static char[] validChars = {':', 'N', 'L', 'Q', 'P', 'W', 'S', 'A', 'D'};

    private static boolean PRINT_TYPED_KEYS = true;

    public static boolean isNumber(char c) {
        for (char number : numbers) {
            if (c == number) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValidChar(char c) {
        for (char validChar : validChars) {
            if (c == validChar) {
                return true;
            }
        }
        return false;
    }

    public static char getNextKey() {
        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char c = Character.toUpperCase(StdDraw.nextKeyTyped());
                if (PRINT_TYPED_KEYS) {
                    System.out.println(c);
                }
                return c;
            }
        }
    }

    /** Return an object of type T read from FILE, casting it to EXPECTEDCLASS. */
    static <T extends Serializable> T readObject(File file, Class<T> expectedClass) {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
            T result = expectedClass.cast(in.readObject());
            in.close();
            return result;
        } catch (IOException | ClassCastException | ClassNotFoundException exception) {
            throw new IllegalArgumentException(exception.getMessage());
        }
    }

    /** Write OBJECT to FILE. */
    static void writeObject(File file, Serializable object) {
        writeContents(file, serialize(object));
    }

    static void writeContents(File file, Object... contents) {
        try {
            if (file.isDirectory()) {
                throw new IllegalArgumentException("cannot overwrite directory");
            }
            BufferedOutputStream str =
                    new BufferedOutputStream(Files.newOutputStream(file.toPath()));
            for (Object obj : contents) {
                if (obj instanceof byte[]) {
                    str.write((byte[]) obj);
                } else {
                    str.write(((String) obj).getBytes(StandardCharsets.UTF_8));
                }
            }
            str.close();
        } catch (IOException | ClassCastException exception) {
            throw new IllegalArgumentException(exception.getMessage());
        }
    }

    static byte[] serialize(Serializable object) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(stream);
            objectStream.writeObject(object);
            objectStream.close();
            return stream.toByteArray();
        } catch (IOException exception) {
            throw error("Internal error serializing commit.");
        }
    }

    static BYOWException error(String message, Object... args) {
        return new BYOWException(String.format(message, args));
    }

    public static class BYOWException extends RuntimeException {
        BYOWException(String message) {
            super(message);
        }
    }

    /** Return the concatenation of FIRST and OTHERS into a File designator,
     *  analogous to the java.nio.file.Paths.get(String, String[]) method. */
    static File join(String first, String... others) {
        return Paths.get(first, others).toFile();
    }

    static File join(File first, String... others) {
        return Paths.get(first.getPath(), others).toFile();
    }
}
