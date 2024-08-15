package chat.octet.app.core.enums;


import lombok.Getter;

@Getter
public enum IconMapper {

    STAR("Star", "star-icon"),
    EDGE("Edge Box", "edge-icon"),
    CHATGPT("ChatGPT", "chatgpt-icon"),
    AI("AI bot", "ai-icon"),
    CHATBOT("Chat bot", "chatbot-icon"),
    CODE("Code assistant", "code-icon"),
    DINOSAUR("Dinosaur", "dinosaur-icon"),
    FOX("Fox", "fox-icon"),
    PANDA("Kiss panda", "panda-icon"),
    PROGRAM("Program", "program-icon");

    private final String name;
    private final String icon;

    IconMapper(String name, String icon) {
        this.name = name;
        this.icon = icon;
    }

    public static IconMapper getByName(String name) {
        for (IconMapper iconMapper : values()) {
            if (iconMapper.getName().equalsIgnoreCase(name)) {
                return iconMapper;
            }
        }
        return null;
    }
}
