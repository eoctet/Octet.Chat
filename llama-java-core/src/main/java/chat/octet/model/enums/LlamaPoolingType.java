package chat.octet.model.enums;


import com.google.common.collect.Maps;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

/**
 * Llama Pooling type define
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@Getter
public enum LlamaPoolingType {
    /**
     * unspecified type.
     */
    LLAMA_POOLING_TYPE_UNSPECIFIED(-1),
    /**
     * pooling none type.
     */
    LLAMA_POOLING_TYPE_NONE(0),
    /**
     * pooling mean type.
     */
    LLAMA_POOLING_TYPE_MEAN(1),
    /**
     * pooling cls type.
     */
    LLAMA_POOLING_TYPE_CLS(2);

    private final int type;

    private static final Map<Integer, LlamaPoolingType> TYPES;

    static {
        Map<Integer, LlamaPoolingType> map = Maps.newHashMap();

        for (LlamaPoolingType type : values()) {
            if (map.put(type.type, type) != null) {
                throw new IllegalStateException("Duplicated key found: " + type.name());
            }
        }
        TYPES = Collections.unmodifiableMap(map);
    }

    LlamaPoolingType(int type) {
        this.type = type;
    }

    public static LlamaPoolingType valueOfType(int type) {
        return TYPES.get(type);
    }

    @Override
    public String toString() {
        return this.name();
    }
}
