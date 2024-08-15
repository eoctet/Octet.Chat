package chat.octet.app.controller;

import chat.octet.app.core.constants.AppConstants;
import chat.octet.app.core.controls.DialogWrapper;
import chat.octet.app.core.enums.LanguageType;
import chat.octet.app.core.enums.ThemeType;
import chat.octet.app.core.exceptions.ResourceException;
import chat.octet.app.model.CharacterConfig;
import chat.octet.app.service.AppService;
import chat.octet.app.service.ChatService;
import chat.octet.app.utils.MessageI18N;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import io.github.palexdev.materialfx.utils.ScrollUtils;
import io.github.palexdev.materialfx.utils.others.FunctionalStringConverter;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import static chat.octet.app.core.constants.AppConstants.DEFAULT_SESSION_ID;


@Slf4j
public class AppSettingController implements Initializable {
    @FXML
    private MFXComboBox<LanguageType> appLanguageCombo;
    @FXML
    private MFXComboBox<ThemeType> appThemeCombo;
    @FXML
    private MFXFontIcon addNewCharacterIcon;
    @FXML
    private BorderPane appConfigPane;
    @FXML
    private MFXComboBox<CharacterConfig> appCharacterCombo;
    @FXML
    private MFXScrollPane characterScrollPane;

    private void initLanguageSettings() {
        ObservableList<LanguageType> languages = FXCollections.observableArrayList(LanguageType.values());
        StringConverter<LanguageType> languageConverter = FunctionalStringConverter.to(lang -> (lang == null) ? "" : lang.getLocale().getDisplayLanguage(lang.getLocale()));
        appLanguageCombo.setItems(languages);
        appLanguageCombo.setConverter(languageConverter);
        appLanguageCombo.selectItem(AppService.get().getLanguage());

        appLanguageCombo.selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
            if (newValue != null) {
                AppService.get().changeLanguage(newValue);
                AppService.get().showInfoDialog(MessageI18N.get("ui.message.i18n"));
            }
        });
    }

    private void initThemeSettings() {
        ObservableList<ThemeType> themes = FXCollections.observableArrayList(ThemeType.values());
        StringConverter<ThemeType> themeConverter = FunctionalStringConverter.to(theme -> (theme == null) ? "" : MessageI18N.get(theme.getName()));
        appThemeCombo.setItems(themes);
        appThemeCombo.setConverter(themeConverter);
        appThemeCombo.selectItem(AppService.get().getTheme());

        appThemeCombo.selectedItemProperty().addListener((observableValue, oldValue, newValue) -> AppService.get().changeTheme(Optional.ofNullable(newValue).orElse(oldValue)).applyTheme());
    }

    private void initCharacterSettings(Map<String, CharacterConfig> characters) {
        CharacterConfig defaultValue = CharacterConfig.builder().id("default").name(MessageI18N.get("ui.setting.app.character.default")).build();
        ObservableList<CharacterConfig> charactersList = FXCollections.observableArrayList(characters.values());
        charactersList.add(0, defaultValue);
        StringConverter<CharacterConfig> characterConverter = FunctionalStringConverter.to(ch -> (ch == null) ? "" : ch.getName());
        appCharacterCombo.setItems(charactersList);
        appCharacterCombo.setConverter(characterConverter);

        if (ChatService.get().isModelLoaded() && characters.containsKey(ChatService.get().getCharacterConfig().getId())) {
            appCharacterCombo.selectItem(ChatService.get().getCharacterConfig());
        } else {
            appCharacterCombo.selectItem(defaultValue);
        }

        appCharacterCombo.selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                String id = newValue.getId();
                if (!DEFAULT_SESSION_ID.equalsIgnoreCase(id) && (!ChatService.get().isModelLoaded() || !id.equals(ChatService.get().getCharacterConfig().getId()))) {
                    if (!ChatService.get().isModelLoaded() || AppService.get().showConfirmDialog(DialogWrapper.DialogType.WARN, MessageI18N.get("ui.message.confirm.title"), MessageI18N.get("ui.message.character.switch"))) {
                        Platform.runLater(() -> {
                            try {
                                ChatService.get().loadCharacter(id);
                                AppService.get().changeCharacter(id);
                            } catch (ResourceException re) {
                                AppService.get().showInfoDialog(MessageI18N.get("ui.message.character.busy"));
                            } catch (Exception e) {
                                log.error("load character error", e);
                                AppService.get().showErrorDialog(MessageI18N.get("ui.message.runtime.error", e.getMessage()));
                            }
                        });
                    } else {
                        appCharacterCombo.selectItem(ChatService.get().getCharacterConfig());
                    }
                }
            }
        });

    }

    private void initCharacterList(Map<String, CharacterConfig> characters) {
        addNewCharacterIcon.addEventHandler(MouseEvent.MOUSE_CLICKED,
                event -> AppService.get().showView(AppConstants.FXML_CHARACTER_CONFIG, true, controller -> new CharacterConfigController(null))
        );

        Platform.runLater(() -> {
            VBox characterListBox = new VBox();
            characterListBox.setSpacing(5);
            characterListBox.getStyleClass().add("app-setting-container");

            characterScrollPane.setContent(characterListBox);
            ScrollUtils.addSmoothScrolling(characterScrollPane, 0.5);

            for (Map.Entry<String, CharacterConfig> entry : characters.entrySet()) {
                CharacterConfig character = entry.getValue();

                EventHandler<MouseEvent> eventHandler = event -> {
                    String vid = "config:" + character.getId();
                    AppService.get().showView(vid, AppConstants.FXML_CHARACTER_CONFIG, controller -> new CharacterConfigController(character.getId()));
                };

                Label label = new Label();
                label.setText(character.getName());

                MFXFontIcon icon = new MFXFontIcon();
                icon.getStyleClass().add("app-setting-icon");
                icon.setDescription("fas-angle-right");
                icon.setSize(16);
                icon.addEventHandler(MouseEvent.MOUSE_CLICKED, eventHandler);

                HBox itemBox = new HBox();
                itemBox.getStyleClass().add("character-setting-item");
                itemBox.addEventHandler(MouseEvent.MOUSE_CLICKED, eventHandler);
                itemBox.getChildren().addAll(label, icon);

                characterListBox.getChildren().add(itemBox);
            }
        });
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initLanguageSettings();
        initThemeSettings();
        Map<String, CharacterConfig> characters = ChatService.get().getCharacterConfigList();
        initCharacterSettings(characters);
        initCharacterList(characters);
    }
}
