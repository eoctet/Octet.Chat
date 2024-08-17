package chat.octet.model.enums;


import com.google.common.collect.Maps;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

/**
 * llama attention type
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@Getter
public enum LlamaAttentionType {
    /**
     * unspecified type.
     */
    LLAMA_ATTENTION_TYPE_UNSPECIFIED(-1),
    /**
     * causal type.
     */
    LLAMA_ATTENTION_TYPE_CAUSAL(0),
    /**
     * non causal type.
     */
    LLAMA_ATTENTION_TYPE_NON_CAUSAL(1);


    private static final Map<Integer, LlamaAttentionType> TYPES;

    static {
        Map<Integer, LlamaAttentionType> map = Maps.newHashMap();

        for (LlamaAttentionType type : values()) {
            if (map.put(type.type, type) != null) {
                throw new IllegalStateException("Duplicated key found: " + type.name());
            }
        }
        TYPES = Collections.unmodifiableMap(map);
    }

    private final int type;

    LlamaAttentionType(int type) {
        this.type = type;
    }

    public static LlamaAttentionType valueOfType(int type) {
        return TYPES.get(type);
    }

    @Override
    public String toString() {
        return this.name();
    }
}
