package chat.octet.app.service;

import chat.octet.app.AppResourcesLoader;
import chat.octet.app.controller.ChatController;
import chat.octet.app.core.AppCache;
import chat.octet.app.core.constants.AppConstants;
import chat.octet.app.core.controls.DialogWrapper;
import chat.octet.app.core.enums.LanguageType;
import chat.octet.app.core.enums.ThemeType;
import chat.octet.app.core.exceptions.AppException;
import chat.octet.app.utils.FileUtils;
import chat.octet.app.utils.MessageI18N;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.github.palexdev.materialfx.beans.NumberRange;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import io.github.palexdev.materialfx.controls.MFXRectangleToggleNode;
import io.github.palexdev.materialfx.utils.ToggleButtonsUtil;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static chat.octet.app.core.constants.AppConstants.DEFAULT_SESSION_ID;
import static chat.octet.app.core.constants.AppConstants.MAX_CHAT_SESSION;

@Slf4j
public final class AppService {
    private static volatile AppService handler;

    private final ToggleGroup toggleGroup;
    private final HBox chatSessionStatusNode;
    private final Properties properties;

    private AnchorPane appWindowPane;
    private StackPane appContentPane;
    private VBox appSidebarNav;

    public AppService setAppWindowPane(AnchorPane appWindowPane) {
        this.appWindowPane = appWindowPane;
        return this;
    }

    public AppService setAppContentPane(StackPane appContentPane) {
        this.appContentPane = appContentPane;
        return this;
    }

    public AppService setAppSidebarNav(VBox appSidebarNav) {
        this.appSidebarNav = appSidebarNav;
        return this;
    }

    private AppService() {
        this.toggleGroup = new ToggleGroup();
        ToggleButtonsUtil.addAlwaysOneSelectedSupport(toggleGroup);

        this.properties = new Properties();
        try {
            File file = new File(AppConstants.APP_PROPERTIES_PATH);
            if (!file.exists()) {
                FileUtils.writeFile(
                        file.getAbsolutePath(),
                        Resources.toString(Resources.getResource("default.app.properties"), Charsets.UTF_8)
                );
            }
            this.properties.load(Files.newInputStream(file.toPath()));
        } catch (IOException e) {
            throw new AppException("Load properties error", e);
        }

        this.chatSessionStatusNode = new HBox();
        this.chatSessionStatusNode.setAlignment(Pos.CENTER);
        this.chatSessionStatusNode.getStyleClass().add("chat-session-status");
        MFXProgressSpinner spinner = new MFXProgressSpinner();
        spinner.getRanges1().add(NumberRange.of(0.0, 0.5));
        spinner.getRanges2().add(NumberRange.of(0.51, 1.0));
        this.chatSessionStatusNode.getChildren().setAll(spinner);
        this.chatSessionStatusNode.setCache(true);
    }

    public static AppService get() {
        if (handler == null) {
            synchronized (AppService.class) {
                if (handler == null) {
                    handler = new AppService();
                }
            }
        }
        return handler;
    }

    public void updateAppSetting(String key, String value) {
        try {
            properties.setProperty(key, value);
            properties.store(Files.newOutputStream(Paths.get(AppConstants.APP_PROPERTIES_PATH), StandardOpenOption.TRUNCATE_EXISTING), "Auto store by app.");
        } catch (IOException e) {
            log.error("Update properties error", e);
            AppService.get().showErrorDialog(MessageI18N.get("ui.message.runtime.error", e.getMessage()));
        }
    }

    public FXMLLoader createFXMLLoader(String fxmlPath) {
        ResourceBundle bundle = MessageI18N.getBundle(getLanguage().getLocale());
        return new FXMLLoader(AppResourcesLoader.loadURL(fxmlPath), bundle);
    }

    private ToggleButton createToggle(String sessionId) {
        MFXRectangleToggleNode toggleNode = new MFXRectangleToggleNode(MessageI18N.get("ui.main.sidebar.session"), AppConstants.createChatSessionIcon());
        toggleNode.setId(sessionId);
        toggleNode.setToggleGroup(toggleGroup);
        toggleNode.wrapTextProperty().set(true);

        toggleNode.setOnAction(event -> {
            String vid = "chat:" + toggleNode.getId();
            showView(vid, AppConstants.FXML_CHAT, controller -> new ChatController(toggleNode.getId()));
        });
        return toggleNode;
    }

    public void createChatSession() {
        if (appSidebarNav.getChildren().size() > MAX_CHAT_SESSION) {
            showInfoDialog(MessageI18N.get("ui.message.session.limit"));
            return;
        }

        String sessionId = properties.getProperty(AppConstants.APP_SETTING_CHARACTER, "").isEmpty() ?
                DEFAULT_SESSION_ID : UUID.randomUUID().toString().replace("-", "").toLowerCase();
        ToggleButton toggleButton = createToggle(sessionId);

        appSidebarNav.getChildren().add(toggleButton);
        toggleButton.fire();
    }

    public void updateChatSessionTitle(String sessionId, String title) {
        Node node = appSidebarNav.getChildren().stream().filter(n -> n.getId().equals(sessionId)).findFirst().orElse(null);
        if (node instanceof MFXRectangleToggleNode btn) {
            String value = title.length() > 10 ? title.substring(0, 10) + "..." : title;
            btn.setText(value);
        }
    }

    public void updateChatSessionStatus(String sessionId, boolean running) {
        Node node = appSidebarNav.getChildren().stream().filter(n -> n.getId().equals(sessionId)).findFirst().orElse(null);
        if (node instanceof MFXRectangleToggleNode btn) {
            if (running) {
                btn.setLabelLeadingIcon(chatSessionStatusNode);
            } else {
                btn.setLabelLeadingIcon(AppConstants.createChatSessionIcon());
            }
        }
    }

    public void deleteAllChatSession() {
        Platform.runLater(() -> {
            appSidebarNav.getChildren().forEach(node -> {
                ChatService.get().removeChatSession(node.getId());
                AppCache.removeView("chat", node.getId());
            });
            appSidebarNav.getChildren().clear();
            AppService.get().createChatSession();
        });
    }

    public void deleteChatSession(String sessionId) {
        Node node = appSidebarNav.getChildren().stream().filter(n -> n.getId().equals(sessionId)).findFirst().orElse(null);
        if (node != null) {
            int index = Math.max(0, appSidebarNav.getChildren().indexOf(node) - 1);
            if (appSidebarNav.getChildren().remove(node)) {
                ChatService.get().removeChatSession(sessionId);
                AppCache.removeView("chat", sessionId);
                if (!appSidebarNav.getChildren().isEmpty()) {
                    Optional.ofNullable(appSidebarNav.getChildren().get(index)).ifPresent(n -> ((MFXRectangleToggleNode) n).fire());
                } else {
                    createChatSession();
                }
            }
        }
    }

    public AppService clearChatSessionSelected() {
        appSidebarNav.getChildren().forEach(n -> ((MFXRectangleToggleNode) n).selectedProperty().set(false));
        return this;
    }

    public void showView(String id, String fxmlPath, boolean reload, Callback<Class<?>, Object> controllerFactory) {
        if (!reload && AppCache.hasCache(id)) {
            appContentPane.getChildren().setAll(AppCache.getView(id));
            appContentPane.requestFocus();
        } else {
            Platform.runLater(() -> {
                try {
                    FXMLLoader loader = createFXMLLoader(fxmlPath);
                    if (controllerFactory != null) {
                        loader.setControllerFactory(controllerFactory);
                    }
                    Parent view = Objects.requireNonNull(loader.load());
                    view.setCache(true);
                    view.setCacheHint(CacheHint.DEFAULT);
                    appContentPane.getChildren().setAll(view);
                    appContentPane.requestFocus();
                    AppCache.addView(view, id);
                } catch (IOException e) {
                    throw new AppException(e.getMessage(), e);
                }
            });
        }
    }

    public void showView(String id, String fxmlPath, Callback<Class<?>, Object> controllerFactory) {
        showView(id, fxmlPath, false, controllerFactory);
    }

    public void showView(String fxmlPath, boolean reload, Callback<Class<?>, Object> controllerFactory) {
        showView(fxmlPath, fxmlPath, reload, controllerFactory);
    }

    public void showView(String fxmlPath, Callback<Class<?>, Object> controllerFactory) {
        showView(fxmlPath, fxmlPath, false, controllerFactory);
    }

    public AppService applyTheme() {
        ThemeType theme = getTheme();
        appWindowPane.getStylesheets().setAll(AppResourcesLoader.load("css/" + theme.getValue().toLowerCase() + ".css"));
        appWindowPane.applyCss();
        appWindowPane.layout();
        return this;
    }

    public AppService changeTheme(ThemeType theme) {
        updateAppSetting(AppConstants.APP_SETTING_THEME, theme.getValue());
        return this;
    }

    public ThemeType getTheme() {
        String value = properties.getProperty(AppConstants.APP_SETTING_THEME, ThemeType.LIGHT.getValue());
        return ThemeType.getByValue(value);
    }

    public AppService applyLanguage() {
        MessageI18N.setLanguage(getLanguage());
        return this;
    }

    public void changeLanguage(LanguageType lang) {
        updateAppSetting(AppConstants.APP_SETTING_LANGUAGE, lang.getLocale().toString());
    }

    public LanguageType getLanguage() {
        String value = properties.getProperty(AppConstants.APP_SETTING_LANGUAGE, "");
        return StringUtils.isBlank(value) ? LanguageType.defaultLanguage() : LanguageType.getByValue(value);
    }

    public void loadChatCharacter() {
        String value = properties.getProperty(AppConstants.APP_SETTING_CHARACTER, "");
        Platform.runLater(() -> {
            try {
                if (StringUtils.isNotBlank(value)) {
                    ChatService.get().loadCharacter(value);
                }
                createChatSession();
            } catch (Exception e) {
                log.error("load character error", e);
                AppService.get().showErrorDialog(MessageI18N.get("ui.message.runtime.error", e.getMessage()));
            }
        });
    }

    public void changeCharacter(String id) {
        updateAppSetting(AppConstants.APP_SETTING_CHARACTER, id);
    }

    public boolean showConfirmDialog(DialogWrapper.DialogType type, String title, String content) {
        return DialogWrapper.build()
                .setTitle(title)
                .setContent(content)
                .setTheme(getTheme())
                .setOwnerNode(appWindowPane)
                .setOwnerWindow(appWindowPane.getScene().getWindow())
                .setType(type)
                .showConfirmDialog();
    }

    public boolean showConfirmDialog(String title, String content) {
        return showConfirmDialog(DialogWrapper.DialogType.INFO, title, content);
    }

    public boolean showConfirmDialog(String content) {
        return showConfirmDialog(DialogWrapper.DialogType.INFO, MessageI18N.get("ui.message.confirm.title"), content);
    }

    private void showDialog(DialogWrapper.DialogType type, String title, String content) {
        DialogWrapper.build()
                .setTitle(title)
                .setContent(content)
                .setTheme(getTheme())
                .setOwnerNode(appWindowPane)
                .setOwnerWindow(appWindowPane.getScene().getWindow())
                .setType(type)
                .showDialog();
    }

    public void showInfoDialog(String content) {
        showDialog(DialogWrapper.DialogType.INFO, MessageI18N.get("ui.message.info.title"), content);
    }

    public void showErrorDialog(String content) {
        showDialog(DialogWrapper.DialogType.ERROR, MessageI18N.get("ui.message.error.title"), content);
    }

    public void showWarnDialog(String content) {
        showDialog(DialogWrapper.DialogType.WARN, MessageI18N.get("ui.message.warn.title"), content);
    }

}
