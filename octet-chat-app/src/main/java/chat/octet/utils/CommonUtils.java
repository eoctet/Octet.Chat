package chat.octet.utils;

import chat.octet.exceptions.ServerException;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class CommonUtils {

    private static final String CHARACTER_SET = "abcdefghijklmnopqrstuvwxyz0123456789";

    public static String randomString(String prefixString) {
        String randomString = IntStream.range(0, 10).map(i -> new SecureRandom().nextInt(CHARACTER_SET.length())).mapToObj(randomInt -> CHARACTER_SET.substring(randomInt, randomInt + 1)).collect(Collectors.joining());
        return StringUtils.join(prefixString, "-", randomString);
    }

    public static boolean isEmpty(Object value) {
        if (value instanceof Collection<?>) {
            return ((Collection<?>) value).isEmpty();
        } else if (value instanceof Map) {
            return ((Map<?, ?>) value).isEmpty();
        } else {
            return value == null;
        }
    }

    public static List<String> readFileLines(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.isRegularFile(path) || !Files.exists(path)) {
            throw new ServerException("Can not read file, please make sure it is valid: " + filePath);
        }
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ServerException("Read file error", e);
        }
    }

}
