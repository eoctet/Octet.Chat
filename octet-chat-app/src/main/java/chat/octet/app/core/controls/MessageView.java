package chat.octet.app.core.controls;


import chat.octet.app.AppResourcesLoader;
import chat.octet.app.core.constants.AppConstants;
import chat.octet.app.core.enums.IconMapper;
import chat.octet.app.core.markdown.MarkdownRender;
import chat.octet.app.model.CharacterConfig;
import chat.octet.app.service.AppService;
import chat.octet.app.utils.MessageI18N;
import chat.octet.model.beans.ChatMessage;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Getter
@Slf4j
public class MessageView {

    private final ChatMessage message;
    private CharacterConfig characterConfig;

    private MessageView(ChatMessage message) {
        this.message = message;
    }

    private Pane createMarkdownMessage(String text) {
        return MarkdownRender.get().render(text);
    }

    private Node createMessageView(String id, ChatMessage.ChatRole role, ObservableValue<String> content) {
        VBox messageView = new VBox();
        messageView.getStyleClass().add("chat-message");
        messageView.getStyleClass().add(ChatMessage.ChatRole.USER == role ? "user-message" : "assistant-message");
        messageView.setSpacing(5);
        messageView.setId(id);

        HBox messageContainer = new HBox();
        messageContainer.setSpacing(5);
        messageContainer.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getClickCount() == 2) {
                try {
                    if (AppConstants.clipboardContent(content.getValue())) {
                        AppService.get().showInfoDialog(MessageI18N.get("ui.message.clipboard.copy"));
                    }
                } catch (Exception e) {
                    log.error("Failed to copy message", e);
                }
            }
        });

        ImageView avatar = new ImageView();
        avatar.setFitWidth(32);
        avatar.setFitHeight(32);
        avatar.getStyleClass().add("chat-avatar");
        String iconName = Objects.requireNonNull(IconMapper.getByName(characterConfig.getIcon())).getIcon();
        String res = ChatMessage.ChatRole.USER == role ? "static/user-avatar.png" : ("static/" + iconName + ".png");
        avatar.setImage(new Image(AppResourcesLoader.load(res)));

        VBox markdownNode = new VBox();
        markdownNode.getStyleClass().add("chat-content");
        markdownNode.setFillWidth(true);
        markdownNode.getChildren().setAll(createMarkdownMessage(content.getValue()));
        content.addListener((observable, oldValue, newValue) -> markdownNode.getChildren().setAll(createMarkdownMessage(content.getValue())));

        messageContainer.getChildren().addAll(avatar, markdownNode);
        messageView.getChildren().setAll(messageContainer);

        return messageView;
    }

    public static MessageView build(ChatMessage message) {
        return new MessageView(message);
    }

    public MessageView setCharacter(CharacterConfig characterConfig) {
        this.characterConfig = characterConfig;
        return this;
    }

    public Node get() {
        return createMessageView(message.getId(), message.getRole(), new SimpleStringProperty(message.getContent()));
    }

    public Node get(ObservableValue<String> content) {
        return createMessageView(message.getId(), message.getRole(), content);
    }

}
