package chat.octet.model.enums;


import com.google.common.collect.Maps;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

/**
 * Llama numa strategy define
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@Getter
public enum LlamaNumaStrategy {
    /**
     * Disabled strategy.
     */
    NUMA_STRATEGY_DISABLED(0),
    /**
     * Distribute strategy.
     */
    NUMA_STRATEGY_DISTRIBUTE(1),
    /**
     * Isolate strategy.
     */
    NUMA_STRATEGY_ISOLATE(2),
    /**
     * Numa control strategy.
     */
    NUMA_STRATEGY_NUMACTL(3),
    /**
     * Mirror strategy.
     */
    NUMA_STRATEGY_MIRROR(4),
    /**
     * Count strategy.
     */
    NUMA_STRATEGY_COUNT(5);

    private static final Map<Integer, LlamaNumaStrategy> TYPES;

    static {
        Map<Integer, LlamaNumaStrategy> map = Maps.newHashMap();

        for (LlamaNumaStrategy type : values()) {
            if (map.put(type.type, type) != null) {
                throw new IllegalStateException("Duplicated key found: " + type.name());
            }
        }
        TYPES = Collections.unmodifiableMap(map);
    }

    private final int type;

    LlamaNumaStrategy(int type) {
        this.type = type;
    }

    public static LlamaNumaStrategy valueOfType(int type) {
        return TYPES.get(type);
    }

    @Override
    public String toString() {
        return this.name();
    }
}
