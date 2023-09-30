package chat.octet.model.beans;


import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.Map;

public enum LlamaTokenType {
    /**
     * LLAMA_TOKEN_TYPE_UNDEFINED
     */
    LLAMA_TOKEN_TYPE_UNDEFINED(0),
    /**
     * LLAMA_TOKEN_TYPE_NORMAL
     */
    LLAMA_TOKEN_TYPE_NORMAL(1),
    /**
     * LLAMA_TOKEN_TYPE_UNKNOWN
     */
    LLAMA_TOKEN_TYPE_UNKNOWN(2),
    /**
     * LLAMA_TOKEN_TYPE_CONTROL
     */
    LLAMA_TOKEN_TYPE_CONTROL(3),
    /**
     * LLAMA_TOKEN_TYPE_USER_DEFINED
     */
    LLAMA_TOKEN_TYPE_USER_DEFINED(4),
    /**
     * LLAMA_TOKEN_TYPE_UNUSED
     */
    LLAMA_TOKEN_TYPE_UNUSED(5),
    /**
     * LLAMA_TOKEN_TYPE_BYTE
     */
    LLAMA_TOKEN_TYPE_BYTE(6);

    private final int type;

    public int getType() {
        return type;
    }

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
