package chat.octet.model.enums;


import com.google.common.collect.Maps;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

/**
 * Llama special token type.
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@Getter
public enum LlamaSpecialTokenType {

    TOKEN_BOS(0),

    TOKEN_EOS(1),

    TOKEN_CLS(2),

    TOKEN_SEP(3),

    TOKEN_NL(4),

    TOKEN_PREFIX(5),

    TOKEN_MIDDLE(6),

    TOKEN_SUFFIX(7),

    TOKEN_EOT(8);

    private static final Map<Integer, LlamaSpecialTokenType> TYPES;

    static {
        Map<Integer, LlamaSpecialTokenType> map = Maps.newHashMap();

        for (LlamaSpecialTokenType type : values()) {
            if (map.put(type.type, type) != null) {
                throw new IllegalStateException("Duplicated key found: " + type.name());
            }
        }
        TYPES = Collections.unmodifiableMap(map);
    }

    private final int type;

    LlamaSpecialTokenType(int type) {
        this.type = type;
    }

    public static LlamaSpecialTokenType valueOfType(int type) {
        return TYPES.get(type);
    }

    @Override
    public String toString() {
        return this.name();
    }
}
