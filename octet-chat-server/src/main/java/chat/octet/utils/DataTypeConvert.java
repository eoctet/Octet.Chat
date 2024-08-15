package chat.octet.utils;

import chat.octet.api.functions.enums.DataType;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.Date;


/**
 * Data type converter.
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@Slf4j
public class DataTypeConvert {
    public static final String DATE_FORMAT_WITH_MILLIS = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String DATE_FORMAT_WITH_TIME = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_FORMAT_WITH_MILLIS2 = "yyyy/MM/dd HH:mm:ss.SSS";
    public static final String DATE_FORMAT_WITH_TIME2 = "yyyy/MM/dd HH:mm:ss";
    public static final String DATE_FORMAT2 = "yyyy/MM/dd";

    private static final String POINT = ".";
    private static final String SLASH = "/";
    private static final String TIME = "T";
    private static final String ZONE = "Z";
    private static final String COLON = ":";
    private static final String LINE = "-";
    private static final String TRUE_NUMBER = "1";
    private static final String TRUE_FLAG = "yes";
    private static final String FALSE_NUMBER = "0";
    private static final String FALSE_FLAG = "no";
    private static final int DECIMAL_SCALE = 10;

    public static <T extends Serializable> T getValue(String dataType, Object value) {
        if (StringUtils.isEmpty(dataType)) {
            throw new IllegalArgumentException("DataType cannot be null.");
        }
        DataType dt = DataType.valueOfType(dataType);
        return getValue(dt, value);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T getValue(DataType dataType, Object value) {
        Preconditions.checkNotNull(dataType, MessageFormat.format("Unsupported data type {0}", dataType));
        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
            return (T) dataType.getDefaultValue();
        }
        return (T) convert(dataType.getClassType(), String.valueOf(value));
    }

    private static <T extends Serializable> T convert(Class<T> clazz, String value) {
        if (clazz == Integer.class) {
            if (value.contains(POINT)) {
                value = StringUtils.substringBefore(value, POINT);
            }
            return clazz.cast(Integer.parseInt(value));
        } else if (clazz == Long.class) {
            if (value.contains(POINT)) {
                value = StringUtils.substringBefore(value, POINT);
            }
            return clazz.cast(Long.parseLong(value));
        } else if (clazz == String.class) {
            return clazz.cast(value);
        } else if (clazz == Double.class) {
            return clazz.cast(Double.parseDouble(value));
        } else if (clazz == Boolean.class) {
            boolean b;
            if (StringUtils.equalsAny(value, TRUE_NUMBER, FALSE_NUMBER)) {
                b = TRUE_NUMBER.equals(value);
            } else if (StringUtils.equalsAnyIgnoreCase(value, TRUE_FLAG, FALSE_FLAG)) {
                b = TRUE_FLAG.equalsIgnoreCase(value);
            } else {
                b = Boolean.parseBoolean(value);
            }
            return clazz.cast(b);
        } else if (clazz == BigDecimal.class) {
            return clazz.cast(new BigDecimal(value).setScale(DECIMAL_SCALE, RoundingMode.HALF_UP));
        } else if (clazz == Date.class) {
            String format = null;
            if (value.contains(TIME) && value.contains(ZONE)) {
                value = value.replace(TIME, StringUtils.SPACE).replace(ZONE, StringUtils.EMPTY);
            }
            if (value.contains(SLASH) && StringUtils.indexOf(value, SLASH) == 4) {
                if (value.contains(COLON) && value.contains(POINT)) {
                    format = DATE_FORMAT_WITH_MILLIS2;
                } else if (value.contains(COLON) && !value.contains(POINT)) {
                    format = DATE_FORMAT_WITH_TIME2;
                } else {
                    format = DATE_FORMAT2;
                }
            } else if (value.contains(LINE) && value.contains(COLON) && StringUtils.indexOf(value, LINE) == 4) {
                if (value.contains(COLON) && value.contains(POINT)) {
                    format = DATE_FORMAT_WITH_MILLIS;
                } else if (value.contains(COLON) && !value.contains(POINT)) {
                    format = DATE_FORMAT_WITH_TIME;
                } else {
                    format = DATE_FORMAT;
                }
            }
            if (format == null) {
                return clazz.cast(DateTime.parse(value).toDate());
            }
            return clazz.cast(DateTime.parse(value, DateTimeFormat.forPattern(format)).toDate());
        }
        return clazz.cast(value);
    }

}
