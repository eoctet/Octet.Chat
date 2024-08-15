package chat.octet.app.core.constants;


import chat.octet.app.AppResourcesLoader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.time.format.DateTimeFormatter;

public class AppConstants {

    public static final String APP_NAME = "Octet.chat";
    public static final String APP_SETTING_LANGUAGE = "app.language";
    public static final String APP_SETTING_THEME = "app.theme";
    public static final String APP_SETTING_CHARACTER = "app.character";

    public static final String FXML_MAIN = "fxml/app-main.fxml";
    public static final String FXML_SETTING = "fxml/app-setting.fxml";
    public static final String FXML_CHARACTER_CONFIG = "fxml/character-config.fxml";
    public static final String FXML_CHAT = "fxml/chat.fxml";

    public static final int MAX_CHAT_SESSION = 100;
    public static final String DEFAULT_SESSION_ID = "default";

    public static final String APP_USER_CONFIG_PATH;
    public static final String APP_CHARACTERS_CONFIG_PATH;
    public static final String APP_PROPERTIES_PATH;

    public static final DateTimeFormatter DT_FORMATTER_FULL;
    public static final DateTimeFormatter DT_FORMATTER_MONTH;

    static {
        APP_USER_CONFIG_PATH = StringUtils.join(System.getProperty("user.home"), File.separator, ".llama_java");
        APP_CHARACTERS_CONFIG_PATH = StringUtils.join(APP_USER_CONFIG_PATH, File.separator, "characters");
        APP_PROPERTIES_PATH = StringUtils.join(APP_USER_CONFIG_PATH, File.separator, "app.properties");

        DT_FORMATTER_FULL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DT_FORMATTER_MONTH = DateTimeFormatter.ofPattern("MMddHHmmss");
    }

    private AppConstants() {
    }

    public static ImageView createChatSessionIcon() {
        ImageView view = new ImageView(new Image(AppResourcesLoader.loadStream("static/chat-session.png")));
        view.setFitHeight(32);
        view.setFitWidth(32);
        return view;
    }

    public static boolean clipboardContent(String content) {
        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(content);
        Clipboard clipboard = Clipboard.getSystemClipboard();
        return clipboard.setContent(clipboardContent);
    }

}
