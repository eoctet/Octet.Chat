package chat.octet.app.core.enums;


import lombok.Getter;

import java.util.Locale;

@Getter
public enum LanguageType {

    SIMPLIFIED_CHINESE(Locale.SIMPLIFIED_CHINESE),
    ENGLISH(Locale.ENGLISH),
    RUSSIAN(Locale.forLanguageTag("ru"));

    private final Locale locale;

    LanguageType(Locale locale) {
        this.locale = locale;
    }

    public static LanguageType defaultLanguage() {
        return SIMPLIFIED_CHINESE;
    }

    public static LanguageType getByValue(String value) {
        for (LanguageType type : values()) {
            if (type.getLocale().toString().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }

}
