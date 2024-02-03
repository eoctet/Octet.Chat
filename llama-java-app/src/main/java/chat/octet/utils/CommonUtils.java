package chat.octet.utils;

import chat.octet.exceptions.ServerException;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class CommonUtils {

    private static final String CHARACTER_SET = "abcdefghijklmnopqrstuvwxyz0123456789";

    public static String randomString(String prefixString) {
        String randomString = IntStream.range(0, 10).map(i -> new SecureRandom().nextInt(CHARACTER_SET.length())).mapToObj(randomInt -> CHARACTER_SET.substring(randomInt, randomInt + 1)).collect(Collectors.joining());
        return StringUtils.join(prefixString, "-", randomString);
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static String readFile(String filePath) {
        return StringUtils.join(readFileLines(filePath));
    }

    public static List<String> readFileLines(String filePath) {
        File file = new File(filePath);
        if (!file.isFile() || !file.exists()) {
            throw new ServerException("Can not read file, please make sure it is valid: " + filePath);
        }
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            return bufferedReader.lines().collect(Collectors.toList());
        } catch (Exception e) {
            throw new ServerException("Read file error", e);
        }
    }

}
