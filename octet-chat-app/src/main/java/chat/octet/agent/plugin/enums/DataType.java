package chat.octet.agent.plugin.enums;

import com.google.common.collect.Maps;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * Data type define.
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
public enum DataType {

    /**
     * STRING java data type
     */
    STRING(StringUtils.EMPTY),
    /**
     * LONG java data type
     */
    LONG(0L),
    /**
     * INT java data type
     */
    INT(0),
    /**
     * DOUBLE java data type
     */
    DOUBLE(0.0d),
    /**
     * DECIMAL java data type
     */
    DECIMAL(BigDecimal.valueOf(0, 10)),
    /**
     * DATETIME java data type
     */
    DATETIME(new Date()),
    /**
     * BOOLEAN java data type
     */
    BOOLEAN(false);

    private static final Map<String, DataType> TYPES;

    static {
        Map<String, DataType> map = Maps.newHashMap();

        for (DataType type : values()) {
            if (map.put(type.name(), type) != null) {
                throw new IllegalStateException("Duplicated key found: " + type.name());
            }
        }
        TYPES = Collections.unmodifiableMap(map);
    }


    @Getter
    private final Serializable defaultValue;

    private final Class<? extends Serializable> clazz;

    <T extends Serializable> DataType(T defaultValue) {
        this.defaultValue = defaultValue;
        this.clazz = defaultValue.getClass();
    }

    public static DataType valueOfType(String key) {
        return TYPES.get(key.trim().toUpperCase());
    }

    public Class<? extends Serializable> getClassType() {
        return clazz;
    }

}
