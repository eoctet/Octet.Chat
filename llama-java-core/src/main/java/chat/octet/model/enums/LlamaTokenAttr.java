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
public enum LlamaTokenAttr {
    /**
     * Undefined type.
     */
    LLAMA_TOKEN_ATTR_UNDEFINED(0),
    /**
     * Unknown type.
     */
    LLAMA_TOKEN_ATTR_UNKNOWN(1),
    /**
     * Unused type.
     */
    LLAMA_TOKEN_ATTR_UNUSED(1 << 1),
    /**
     * Normal type.
     */
    LLAMA_TOKEN_ATTR_NORMAL(1 << 2),
    /**
     * Control type.
     */
    LLAMA_TOKEN_ATTR_CONTROL(1 << 3),
    /**
     * User defined type.
     */
    LLAMA_TOKEN_ATTR_USER_DEFINED(1 << 4),
    /**
     * Byte type.
     */
    LLAMA_TOKEN_ATTR_BYTE(1 << 5),
    /**
     * Normalized type.
     */
    LLAMA_TOKEN_ATTR_NORMALIZED(1 << 6),
    /**
     * Left strip type.
     */
    LLAMA_TOKEN_ATTR_LSTRIP(1 << 7),
    /**
     * Right strip type.
     */
    LLAMA_TOKEN_ATTR_RSTRIP(1 << 8),
    /**
     * Single word type.
     */
    LLAMA_TOKEN_ATTR_SINGLE_WORD(1 << 9);

    private final int type;

    private static final Map<Integer, LlamaTokenAttr> TYPES;

    static {
        Map<Integer, LlamaTokenAttr> map = Maps.newHashMap();

        for (LlamaTokenAttr type : values()) {
            if (map.put(type.type, type) != null) {
                throw new IllegalStateException("Duplicated key found: " + type.name());
            }
        }
        TYPES = Collections.unmodifiableMap(map);
    }

    LlamaTokenAttr(int type) {
        this.type = type;
    }

    public static LlamaTokenAttr valueOfType(int type) {
        return TYPES.get(type);
    }

    @Override
    public String toString() {
        return this.name();
    }
}
