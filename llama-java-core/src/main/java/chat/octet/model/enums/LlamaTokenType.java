package chat.octet.model.enums;


import com.google.common.collect.Maps;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

/**
 * Llama token type define
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@Getter
@Deprecated
public enum LlamaTokenType {
    /**
     * Undefined type.
     */
    LLAMA_TOKEN_TYPE_UNDEFINED(0),
    /**
     * Normal type.
     */
    LLAMA_TOKEN_TYPE_NORMAL(1),
    /**
     * Unknown type.
     */
    LLAMA_TOKEN_TYPE_UNKNOWN(2),
    /**
     * Control type.
     */
    LLAMA_TOKEN_TYPE_CONTROL(3),
    /**
     * User defined type.
     */
    LLAMA_TOKEN_TYPE_USER_DEFINED(4),
    /**
     * Unused type.
     */
    LLAMA_TOKEN_TYPE_UNUSED(5),
    /**
     * Byte type.
     */
    LLAMA_TOKEN_TYPE_BYTE(6);

    private static final Map<Integer, LlamaTokenType> TYPES;

    static {
        Map<Integer, LlamaTokenType> map = Maps.newHashMap();

        for (LlamaTokenType type : values()) {
            if (map.put(type.type, type) != null) {
                throw new IllegalStateException("Duplicated key found: " + type.name());
            }
        }
        TYPES = Collections.unmodifiableMap(map);
    }

    private final int type;

    LlamaTokenType(int type) {
        this.type = type;
    }

    public static LlamaTokenType valueOfType(int type) {
        return TYPES.get(type);
    }

    @Override
    public String toString() {
        return this.name();
    }
}
