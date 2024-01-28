package chat.octet.model.utils;


import lombok.Getter;

import java.util.Optional;

public class ColorConsole {

    private ColorConsole() {
    }

    public static String red(String text) {
        return format(text, ColorStyle.RED, FontStyle.DEFAULT);
    }

    public static String green(String text) {
        return format(text, ColorStyle.GREEN, FontStyle.DEFAULT);
    }

    public static String yellow(String text) {
        return format(text, ColorStyle.YELLOW, FontStyle.DEFAULT);
    }

    public static String blue(String text) {
        return format(text, ColorStyle.BLUE, FontStyle.DEFAULT);
    }

    public static String magenta(String text) {
        return format(text, ColorStyle.MAGENTA, FontStyle.DEFAULT);
    }

    public static String cyan(String text) {
        return format(text, ColorStyle.CYAN, FontStyle.DEFAULT);
    }

    public static String white(String text) {
        return format(text, ColorStyle.WHITE, FontStyle.DEFAULT);
    }

    public static String orange(String text) {
        return format(text, ColorStyle.ORANGE, FontStyle.DEFAULT);
    }

    public static String grey(String text) {
        return format(text, ColorStyle.GREY, FontStyle.DEFAULT);
    }

    public static String black(String text) {
        return format(text, ColorStyle.BLACK, FontStyle.DEFAULT);
    }

    public static String format(String text, ColorStyle colorStyle) {
        return format(text, colorStyle, FontStyle.DEFAULT);
    }

    public static String format(String text, ColorStyle colorStyle, FontStyle fontStyle) {
        if (text == null || colorStyle == null) {
            return text;
        }
        String template = "\033[38;5;%d%s%s\033[0m";
        String fontStyleFlag = (Optional.ofNullable(fontStyle).orElse(FontStyle.DEFAULT) == FontStyle.DEFAULT) ? "m" : String.format(";%dm", fontStyle.getCode());
        return String.format(template, colorStyle.getCode(), fontStyleFlag, text);
    }

    @Getter
    public enum ColorStyle {
        RED(9),
        GREEN(10),
        YELLOW(11),
        BLUE(12),
        MAGENTA(13),
        CYAN(14),
        WHITE(15),
        ORANGE(202),
        GREY(243),
        BLACK(0);

        private final int code;

        ColorStyle(int code) {
            this.code = code;
        }
    }

    @Getter
    public enum FontStyle {
        DEFAULT(-1),
        BOLD(1),
        UNDERLINE(4),
        ITALIC(3);

        private final int code;

        FontStyle(int code) {
            this.code = code;
        }
    }

}
