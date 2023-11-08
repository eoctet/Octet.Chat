package chat.octet.model.enums;


import com.google.common.collect.Maps;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

/**
 * Llama RoPE scaling type define
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@Getter
public enum LlamaRoPEScalingType {
    /**
     * unspecified type.
     */
    LLAMA_ROPE_SCALING_UNSPECIFIED(-1),
    /**
     * Scaling none type.
     */
    LLAMA_ROPE_SCALING_NONE(0),
    /**
     * Scaling linear type.
     */
    LLAMA_ROPE_SCALING_LINEAR(1),
    /**
     * Scaling YaRN type.
     */
    LLAMA_ROPE_SCALING_YARN(2),
    /**
     * Max value type.
     */
    LLAMA_ROPE_SCALING_MAX_VALUE(2);

    private final int type;

    private static final Map<Integer, LlamaRoPEScalingType> TYPES;

    static {
        Map<Integer, LlamaRoPEScalingType> map = Maps.newHashMap();

        for (LlamaRoPEScalingType type : values()) {
            if (map.put(type.type, type) != null) {
                throw new IllegalStateException("Duplicated key found: " + type.name());
            }
        }
        TYPES = Collections.unmodifiableMap(map);
    }

    LlamaRoPEScalingType(int type) {
        this.type = type;
    }

    public static LlamaRoPEScalingType valueOfType(int type) {
        return TYPES.get(type);
    }

    @Override
    public String toString() {
        return this.name();
    }
}
