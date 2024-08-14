package chat.octet.model.utils;

import chat.octet.model.beans.ChatMessage;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.TimeZone;

@Slf4j
public class JsonUtils {

    private static final ObjectMapper JACKSON_MAPPER;

    static {
        JACKSON_MAPPER = JsonMapper.builder()
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                .addModule(new SimpleModule().addSerializer(ChatMessage.ChatRole.class, new JsonSerializer<ChatMessage.ChatRole>() {
                    @Override
                    public void serialize(ChatMessage.ChatRole value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                        gen.writeString(value.name().toLowerCase());
                    }
                }))
                .build();
        JACKSON_MAPPER.setTimeZone(TimeZone.getDefault());
    }

    private JsonUtils() {
    }

    public static ObjectMapper getObjectMapper() {
        return JACKSON_MAPPER;
    }

    public static <T> T parseToObject(String json, Class<T> clazz) {
        try {
            if (StringUtils.isNotBlank(json)) {
                return JACKSON_MAPPER.readValue(json, clazz);
            }
        } catch (Exception ex) {
            log.error("Parse JSON to Object error", ex);
        }
        return null;
    }

    public static String toJson(Object obj) {
        return toJson(obj, false);
    }

    public static String toJson(Object obj, boolean prettyPrint) {
        try {
            if (prettyPrint) {
                return JACKSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
            } else {
                return JACKSON_MAPPER.writeValueAsString(obj);
            }
        } catch (JsonProcessingException ex) {
            log.error("Parse Object to JSON error", ex);
        }
        return null;
    }

    public static <K, V> LinkedHashMap<K, V> parseJsonToMap(String json, Class<K> key, Class<V> value) {
        JavaType javaType = JACKSON_MAPPER.getTypeFactory().constructMapType(LinkedHashMap.class, key, value);
        try {
            if (StringUtils.isNotBlank(json)) {
                return JACKSON_MAPPER.readValue(json, javaType);
            }
        } catch (Exception ex) {
            log.error("Parse JSON to MAP error", ex);
        }
        return null;
    }

    public static <E> LinkedList<E> parseJsonToList(String json, Class<E> clazz) {
        JavaType javaType = JACKSON_MAPPER.getTypeFactory().constructParametricType(LinkedList.class, clazz);
        try {
            if (StringUtils.isNotBlank(json)) {
                return JACKSON_MAPPER.readValue(json, javaType);
            }
        } catch (Exception ex) {
            log.error("Parse JSON to List error", ex);
        }
        return null;
    }

}
