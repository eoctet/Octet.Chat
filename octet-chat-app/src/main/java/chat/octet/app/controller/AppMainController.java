package chat.octet.app.controller;

import chat.octet.app.core.constants.AppConstants;
import chat.octet.app.core.controls.DialogWrapper;
import chat.octet.app.service.AppService;
import chat.octet.app.service.ChatService;
import chat.octet.app.utils.MessageI18N;
import io.github.palexdev.materialfx.utils.ScrollUtils;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.ResourceBundle;


@Slf4j
public class AppMainController implements Initializable {
    @FXML
    private AnchorPane rootPane;
    @FXML
    private ScrollPane sidebarScrollPane;
    @FXML
    private HBox windowControlTabs;
    @FXML
    private MFXFontIcon appSettingIcon;
    @FXML
    private MFXFontIcon createChatSessionIcon;
    @FXML
    private MFXFontIcon deleteChatSessionIcon;
    @FXML
    private MFXFontIcon miniSidebarIcon;
    @FXML
    private MFXFontIcon alwaysOnTopIcon;
    @FXML
    private MFXFontIcon minimizeIcon;
    @FXML
    private MFXFontIcon closeIcon;
    @FXML
    private VBox sidebarNav;
    @FXML
    private StackPane windowContentPane;
    @FXML
    private HBox sidebarPane;
    @FXML
    private VBox miniSidebar;
    @FXML
    private VBox sidebarMenu;
    @FXML
    private VBox miniSidebarToolbar;
    @FXML
    private HBox sidebarToolbar;

    private final Stage appStage;
    private double xOffset;
    private double yOffset;
    private boolean showSidebar = false;

    public AppMainController(Stage appStage) {
        this.appStage = appStage;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        closeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            boolean confirm = AppService.get().showConfirmDialog(MessageI18N.get("ui.message.close.app"));
            if (confirm) {
                ChatService.get().close();
                Platform.exit();
            }
        });
        minimizeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> appStage.setIconified(true));
        alwaysOnTopIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            boolean newVal = !appStage.isMaximized();
            alwaysOnTopIcon.pseudoClassStateChanged(PseudoClass.getPseudoClass("always-on-top"), newVal);
            appStage.setMaximized(newVal);
            if (!appStage.isMaximized() && appStage.getHeight() > 800) {
                appStage.setHeight(800);
                appStage.setWidth(1024);
            }
        });

        miniSidebar.setVisible(showSidebar);
        miniSidebar.setManaged(showSidebar);

        miniSidebarIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            double width = showSidebar ? 245.0 : 60.0;
            AnchorPane.setLeftAnchor(windowControlTabs, width);
            AnchorPane.setLeftAnchor(windowContentPane, width);
            sidebarPane.setPrefWidth(width);

            if (showSidebar) {
                sidebarToolbar.getChildren().setAll(miniSidebarToolbar.getChildren());
            } else {
                miniSidebarToolbar.getChildren().setAll(sidebarToolbar.getChildren());
            }
            showSidebar = !showSidebar;
            miniSidebar.setVisible(showSidebar);
            miniSidebar.setManaged(showSidebar);
            sidebarMenu.setVisible(!showSidebar);
            sidebarMenu.setManaged(!showSidebar);
        });

        appSettingIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> AppService.get().clearChatSessionSelected().showView(AppConstants.FXML_SETTING, controller -> new AppSettingController()));

        createChatSessionIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (!ChatService.get().isModelLoaded()) {
                AppService.get().showInfoDialog(MessageI18N.get("ui.message.character.empty"));
            } else {
                AppService.get().createChatSession();
            }
        });

        deleteChatSessionIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (!ChatService.get().isModelLoaded()) {
                AppService.get().showInfoDialog(MessageI18N.get("ui.message.character.empty"));
            } else {
                if (AppService.get().showConfirmDialog(DialogWrapper.DialogType.WARN, MessageI18N.get("ui.message.confirm.title"), MessageI18N.get("ui.message.session.clear"))) {
                    AppService.get().deleteAllChatSession();
                }
            }
        });

        deleteChatSessionIcon.disableProperty().bind(ChatService.get().getRunning());

        windowControlTabs.setOnMousePressed(event -> {
            xOffset = appStage.getX() - event.getScreenX();
            yOffset = appStage.getY() - event.getScreenY();
        });
        windowControlTabs.setOnMouseDragged(event -> {
            appStage.setX(event.getScreenX() + xOffset);
            appStage.setY(event.getScreenY() + yOffset);
        });

        ScrollUtils.addSmoothScrolling(sidebarScrollPane, 0.5);

        AppService.get().setAppWindowPane(rootPane)
                .setAppContentPane(windowContentPane)
                .setAppSidebarNav(sidebarNav)
                .applyLanguage()
                .applyTheme()
                .loadChatCharacter();
    }
}
