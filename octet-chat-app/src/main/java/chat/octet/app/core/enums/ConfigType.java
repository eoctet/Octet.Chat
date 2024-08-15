package chat.octet.app.core.enums;


import lombok.Getter;

@Getter
public enum ConfigType {

    MODEL("ui.character.config.type.model", "model"),
    API("ui.character.config.type.api", "api");

    private final String name;
    private final String value;

    ConfigType(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public static ConfigType getByValue(String value) {
        for (ConfigType type : values()) {
            if (type.getValue().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }

}
