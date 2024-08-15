package chat.octet.app.core.enums;


import lombok.Getter;

@Getter
public enum ThemeType {

    DARK("ui.setting.app.theme.dark", "dark"),
    LIGHT("ui.setting.app.theme.light", "light");

    private final String name;
    private final String value;

    ThemeType(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public static ThemeType getByValue(String value) {
        for (ThemeType type : values()) {
            if (type.getValue().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }

}
