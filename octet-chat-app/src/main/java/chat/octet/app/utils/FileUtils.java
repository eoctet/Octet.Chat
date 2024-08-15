package chat.octet.app.utils;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class FileUtils {

    private FileUtils() {
    }

    public static String readFile(String filePath) throws IOException {
        return String.join("\n", readFileLines(filePath));
    }

    public static List<String> readFileLines(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.isRegularFile(path) || !Files.exists(path)) {
            throw new IOException("Can not read file, Please make sure it is valid: " + filePath);
        }
        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }

    public static void writeFile(String filePath, String data) throws IOException {
        Path path = Paths.get(filePath);
        if (!path.toFile().getParentFile().exists()) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, data, StandardCharsets.UTF_8, Files.exists(path) ? StandardOpenOption.TRUNCATE_EXISTING : StandardOpenOption.CREATE);
    }

}
