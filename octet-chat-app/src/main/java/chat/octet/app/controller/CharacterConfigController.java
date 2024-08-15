package chat.octet.app.controller;


import chat.octet.app.core.AppCache;
import chat.octet.app.core.constants.AppConstants;
import chat.octet.app.core.controls.DialogWrapper;
import chat.octet.app.core.enums.ConfigType;
import chat.octet.app.core.enums.IconMapper;
import chat.octet.app.model.CharacterConfig;
import chat.octet.app.model.ModelConfig;
import chat.octet.app.service.AppService;
import chat.octet.app.service.ChatService;
import chat.octet.app.utils.FileUtils;
import chat.octet.app.utils.MessageI18N;
import chat.octet.model.utils.JsonUtils;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.utils.others.FunctionalStringConverter;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;

@Slf4j
public class CharacterConfigController implements Initializable {
    @FXML
    private MFXTextField nameInputText;
    @FXML
    private MFXComboBox<ConfigType> configTypeCombo;
    @FXML
    private MFXComboBox<IconMapper> iconCombo;
    @FXML
    private MFXTextField modelPathInputText;
    @FXML
    private TextArea promptTextInput;
    @FXML
    private TextArea modelConfigTextInput;
    @FXML
    private MFXButton saveConfigBtn;
    @FXML
    private MFXButton cancelConfigBtn;
    @FXML
    private VBox appSettingContainer;
    @FXML
    private MFXButton deleteConfigBtn;

    private final String characterId;

    public CharacterConfigController(String characterId) {
        if (StringUtils.isNotBlank(characterId)) {
            this.characterId = characterId;
        } else {
            this.characterId = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        }
    }

    private void initTypeSettings() {
        ObservableList<ConfigType> types = FXCollections.observableArrayList(ConfigType.MODEL);
        StringConverter<ConfigType> converter = FunctionalStringConverter.to(type -> (type == null) ? "" : MessageI18N.get(type.getName()));
        configTypeCombo.setItems(types);
        configTypeCombo.setConverter(converter);
        configTypeCombo.getValidator().constraint(MessageI18N.get("ui.character.config.validate.type"), configTypeCombo.selectedItemProperty().isNotNull());
    }

    private void initIconSettings() {
        ObservableList<IconMapper> icons = FXCollections.observableArrayList(IconMapper.values());
        StringConverter<IconMapper> iconConverter = FunctionalStringConverter.to(icon -> (icon == null) ? "" : icon.getName());
        iconCombo.setItems(icons);
        iconCombo.setConverter(iconConverter);
        iconCombo.getValidator().constraint(MessageI18N.get("ui.character.config.validate.icon"), iconCombo.selectedItemProperty().isNotNull());
    }

    private void initFormValues() {
        Map<String, CharacterConfig> characters = ChatService.get().getCharacterConfigList();
        if (characters.containsKey(characterId)) {
            deleteConfigBtn.setVisible(true);
            deleteConfigBtn.setManaged(true);

            CharacterConfig config = characters.get(characterId);
            nameInputText.setText(config.getName());
            configTypeCombo.selectItem(ConfigType.getByValue(config.getType()));
            iconCombo.selectItem(IconMapper.getByName(config.getIcon()));
            modelPathInputText.setText(config.getModelPath());
            promptTextInput.setText(Optional.ofNullable(config.getPrompt()).orElse(""));
            modelConfigTextInput.setText(config.getModelConfig() == null ? "" : JsonUtils.toJson(config.getModelConfig(), true));
        } else {
            deleteConfigBtn.setVisible(false);
            deleteConfigBtn.setManaged(false);
            configTypeCombo.selectItem(ConfigType.MODEL);
            iconCombo.selectItem(IconMapper.STAR);
        }
    }

    private boolean formValidate() {
        if (!nameInputText.getValidator().isValid()) {
            String message = nameInputText.getValidator().validate().get(0).getMessage();
            showFormValidateMessage(message);
            nameInputText.requestFocus();
            return false;
        }
        if (!configTypeCombo.getValidator().isValid()) {
            String message = configTypeCombo.getValidator().validate().get(0).getMessage();
            showFormValidateMessage(message);
            configTypeCombo.requestFocus();
            return false;
        }
        if (!iconCombo.getValidator().isValid()) {
            String message = iconCombo.getValidator().validate().get(0).getMessage();
            showFormValidateMessage(message);
            iconCombo.requestFocus();
            return false;
        }
        if (!modelPathInputText.getValidator().isValid()) {
            String message = modelPathInputText.getValidator().validate().get(0).getMessage();
            showFormValidateMessage(message);
            modelPathInputText.requestFocus();
            return false;
        }
        File file = new File(modelPathInputText.getText());
        if (!file.exists() || !file.isFile()) {
            showFormValidateMessage(MessageI18N.get("ui.character.config.validate.file"));
            modelPathInputText.requestFocus();
            return false;
        }
        return true;
    }

    private void showFormValidateMessage(String message) {
        if (appSettingContainer.getChildren().stream().anyMatch(node -> node instanceof Label && node.getStyleClass().contains("form-validate-message"))) {
            return;
        }
        Label label = new Label();
        label.getStyleClass().add("form-validate-message");
        label.setText(message);

        appSettingContainer.getChildren().add(0, label);

        FadeTransition fadeTransition = new FadeTransition(Duration.millis(2000));
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.0);
        fadeTransition.setCycleCount(1);
        fadeTransition.setAutoReverse(true);
        fadeTransition.setNode(label);
        fadeTransition.play();
        fadeTransition.setOnFinished(actionEvent -> appSettingContainer.getChildren().remove(label));
    }

    private void saveConfig() {
        if (formValidate()) {
            String prompt = StringUtils.isBlank(promptTextInput.getText()) ? null : promptTextInput.getText();
            ModelConfig modelConfig = StringUtils.isBlank(modelConfigTextInput.getText()) ? ModelConfig.getDefault() : JsonUtils.parseToObject(modelConfigTextInput.getText(), ModelConfig.class);

            CharacterConfig model = CharacterConfig.builder()
                    .id(characterId)
                    .name(nameInputText.getText().trim())
                    .type(configTypeCombo.getSelectedItem().getValue())
                    .icon(iconCombo.getSelectedItem().getName())
                    .modelPath(modelPathInputText.getText().trim())
                    .prompt(prompt)
                    .modelConfig(modelConfig)
                    .build();

            String json = JsonUtils.toJson(model);
            try {
                FileUtils.writeFile(StringUtils.join(AppConstants.APP_CHARACTERS_CONFIG_PATH, File.separator, characterId, ".json"), json);
                AppCache.removeView("config", characterId);
                AppService.get().showView(AppConstants.FXML_SETTING, true, controller -> new AppSettingController());
            } catch (Exception e) {
                log.error("Save config error", e);
                AppService.get().showErrorDialog(MessageI18N.get("ui.message.runtime.error", e.getMessage()));
            }
        }
    }

    private void deleteConfig() {
        if (AppService.get().showConfirmDialog(DialogWrapper.DialogType.WARN, MessageI18N.get("ui.message.confirm.title"), MessageI18N.get("ui.message.character.delete"))) {
            try {
                String filePath = StringUtils.join(AppConstants.APP_CHARACTERS_CONFIG_PATH, File.separator, characterId, ".json");
                boolean flag = Files.deleteIfExists(Paths.get(filePath));
                if (flag) {
                    AppCache.removeView("config", characterId);
                    AppService.get().showView(AppConstants.FXML_SETTING, true, controller -> new AppSettingController());
                }
            } catch (Exception e) {
                log.error("Delete config error", e);
                AppService.get().showErrorDialog(MessageI18N.get("ui.message.runtime.error", e.getMessage()));
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //init config type settings
        initTypeSettings();
        //init icon settings
        initIconSettings();
        //Load config or create new config
        initFormValues();
        //
        nameInputText.getValidator().constraint(MessageI18N.get("ui.character.config.validate.name"), nameInputText.textProperty().isNotEmpty());
        modelPathInputText.getValidator().constraint(MessageI18N.get("ui.character.config.validate.model"), modelPathInputText.textProperty().isNotEmpty());
        saveConfigBtn.setOnMouseClicked(event -> saveConfig());
        cancelConfigBtn.setOnMouseClicked(event -> AppService.get().showView(AppConstants.FXML_SETTING, controller -> new AppSettingController()));
        deleteConfigBtn.setOnMouseClicked(event -> deleteConfig());
    }

}
