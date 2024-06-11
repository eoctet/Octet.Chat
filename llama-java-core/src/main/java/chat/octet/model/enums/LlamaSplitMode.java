package chat.octet.model.enums;


import com.google.common.collect.Maps;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

/**
 * Llama split mode define
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@Getter
public enum LlamaSplitMode {
    /**
     * single GPU.
     */
    LLAMA_SPLIT_MODE_NONE(0),
    /**
     * split layers and KV across GPUs.
     */
    LLAMA_SPLIT_MODE_LAYER(1),
    /**
     * split rows across GPUs.
     */
    LLAMA_SPLIT_MODE_ROW(2);

    private final int type;

    private static final Map<Integer, LlamaSplitMode> TYPES;

    static {
        Map<Integer, LlamaSplitMode> map = Maps.newHashMap();

        for (LlamaSplitMode type : values()) {
            if (map.put(type.type, type) != null) {
                throw new IllegalStateException("Duplicated key found: " + type.name());
            }
        }
        TYPES = Collections.unmodifiableMap(map);
    }

    LlamaSplitMode(int type) {
        this.type = type;
    }

    public static LlamaSplitMode valueOfType(int type) {
        return TYPES.get(type);
    }

    @Override
    public String toString() {
        return this.name();
    }
}
