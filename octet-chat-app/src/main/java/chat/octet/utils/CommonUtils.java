package chat.octet.utils;

import chat.octet.api.functions.model.Parameter;
import chat.octet.exceptions.ServerException;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class CommonUtils {

    private static final String CHARACTER_SET = "abcdefghijklmnopqrstuvwxyz0123456789";

    public static String randomString(String prefixString) {
        String randomString = IntStream.range(0, 18).map(i -> new SecureRandom().nextInt(CHARACTER_SET.length())).mapToObj(randomInt -> CHARACTER_SET.substring(randomInt, randomInt + 1)).collect(Collectors.joining());
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

    public static String readFile(String filePath) {
        return String.join("\n", readFileLines(filePath));
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

    public static Map<String, Object> parse(List<Parameter> outputParameter, Map<String, Object> result, int limit) {
        return deepCopySelectedFields(outputParameter, result, limit);
    }

    private static Map<String, Object> deepCopySelectedFields(List<Parameter> params, Map<String, Object> source, int limit) {
        return source.entrySet().stream()
                .filter(entry -> isFieldToCopy(entry.getKey(), params))
                .collect(Collectors.toMap(
                        entry -> getParamDesc(entry.getKey(), "", params),
                        entry -> copyValue(entry.getValue(), entry.getKey(), params, limit)
                )).entrySet().stream()
                .filter(entry -> !CommonUtils.isEmpty(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static boolean isFieldToCopy(String key, List<Parameter> params) {
        return params.stream().anyMatch(p -> (p.getName().equals(key) || p.getName().startsWith(key + ".")));
    }

    private static String getParamDataType(String key, List<Parameter> params) {
        Optional<Parameter> op = params.stream().filter(p -> p.getName().equals(key)).findFirst();
        return op.isPresent() ? op.get().getType() : "string";
    }

    private static String getParamDesc(String key, String parentKey, List<Parameter> params) {
        String k = !parentKey.isEmpty() ? parentKey + "." + key : key;
        Optional<Parameter> op = params.stream().filter(p -> StringUtils.isNotBlank(p.getDescription()) && p.getName().equals(k)).findFirst();
        return op.isPresent() ? op.get().getDescription() : key;
    }

    @SuppressWarnings("unchecked")
    private static Object copyValue(Object value, String parentKey, List<Parameter> params, int limit) {
        if (value instanceof Map) {
            return ((Map<String, Object>) value).entrySet().stream()
                    .filter(entry -> isFieldToCopy(parentKey + "." + entry.getKey(), params))
                    .collect(Collectors.toMap(
                            entry -> getParamDesc(entry.getKey(), parentKey, params),
                            entry -> copyValue(entry.getValue(), parentKey + "." + entry.getKey(), params, limit)
                    ));
        } else if (value instanceof List) {
            List<Object> copiedList = ((List<?>) value).stream()
                    .filter(Objects::nonNull)
                    .map(element -> copyValue(element, parentKey, params, limit))
                    .collect(Collectors.toList());
            if (!copiedList.isEmpty() && limit > 0 && limit < copiedList.size()) {
                return copiedList.subList(0, limit);
            }
            return copiedList;
        } else {
            return DataTypeConvert.getValue(getParamDataType(parentKey, params), value);
        }
    }


}
