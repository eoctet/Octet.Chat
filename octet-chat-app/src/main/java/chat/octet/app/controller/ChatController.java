package chat.octet.app.controller;


import chat.octet.app.core.constants.AppConstants;
import chat.octet.app.core.controls.MessageView;
import chat.octet.app.core.exceptions.ResourceException;
import chat.octet.app.model.CharacterConfig;
import chat.octet.app.service.AppService;
import chat.octet.app.service.ChatService;
import chat.octet.app.utils.FileUtils;
import chat.octet.app.utils.MessageI18N;
import chat.octet.model.beans.ChatMessage;
import chat.octet.model.utils.JsonUtils;
import com.google.common.collect.Lists;
import io.github.palexdev.materialfx.controls.MFXContextMenu;
import io.github.palexdev.materialfx.controls.MFXContextMenuItem;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import io.github.palexdev.materialfx.utils.ScrollUtils;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

import static chat.octet.app.core.constants.AppConstants.DEFAULT_SESSION_ID;

@Slf4j
public class ChatController implements Initializable {
    @FXML
    private HBox chatToolbar;
    @FXML
    private MFXScrollPane messageScrollPane;
    @FXML
    private VBox messagesContainer;
    @FXML
    private MFXFontIcon exportHistoryIcon;
    @FXML
    private MFXFontIcon resetIcon;
    @FXML
    private MFXFontIcon startChatIcon;
    @FXML
    private MFXFontIcon regenerateIcon;
    @FXML
    private MFXFontIcon deleteSessionIcon;
    @FXML
    private MFXFontIcon cancelChatIcon;
    @FXML
    private TextArea messageInputText;

    private final String sessionId;
    private final List<ChatMessage> historyMessages;
    private boolean isDefaultTitle;
    private MFXContextMenu contextMenu;

    public ChatController(String sessionId) {
        this.historyMessages = Lists.newLinkedList();
        this.sessionId = sessionId;
        log.debug("Create chat controller, session id: {}", sessionId);
    }

    private void switchToolbarStatus() {
        cancelChatIcon.visibleProperty().set(!cancelChatIcon.isVisible());
        exportHistoryIcon.disableProperty().set(!exportHistoryIcon.isDisabled());
        resetIcon.disableProperty().set(!resetIcon.isDisabled());
        startChatIcon.disableProperty().set(!startChatIcon.isDisabled());
        regenerateIcon.disableProperty().set(!regenerateIcon.isDisabled());
        deleteSessionIcon.disableProperty().set(!deleteSessionIcon.isDisabled());
        messageInputText.disableProperty().set(!messageInputText.isDisabled());
        contextMenu.setDisabled(!contextMenu.isDisabled());
        AppService.get().updateChatSessionStatus(sessionId, contextMenu.isDisabled());
    }

    private void afterProcess(WorkerStateEvent event) {
        Throwable throwable = event.getSource().getException();
        if (throwable != null) {
            log.error("Chat handler error", throwable);
            AppService.get().showErrorDialog(MessageI18N.get("ui.message.runtime.error", throwable.getMessage()));
        }
        switchToolbarStatus();
        //Update assistant message
        if (!historyMessages.isEmpty()) {
            ChatMessage lastChatMessage = historyMessages.get(historyMessages.size() - 1);
            if (ChatMessage.ChatRole.ASSISTANT == lastChatMessage.getRole()) {
                lastChatMessage.setContent(event.getSource().messageProperty().get());
            }
        }
        messageInputText.requestFocus();
    }

    private void startChat(String message) {
        if (message.isBlank()) {
            return;
        }
        if (DEFAULT_SESSION_ID.equals(sessionId)) {
            AppService.get().showInfoDialog(MessageI18N.get("ui.message.character.empty"));
            return;
        }

        CharacterConfig character = ChatService.get().getCharacterConfig();
        ChatMessage userMessage = ChatMessage.toUser(message);
        List<ChatMessage> messages = Lists.newArrayList(userMessage);
        if (StringUtils.isNotBlank(character.getPrompt())) {
            messages.add(0, ChatMessage.toSystem(character.getPrompt()));
        }

        try {
            ChatService.ChatHandler handler = ChatService.get().newChatHandler(sessionId, messages);
            handler.setOnRunning(event -> switchToolbarStatus());
            handler.setOnSucceeded(this::afterProcess);
            handler.setOnCancelled(this::afterProcess);
            handler.setOnFailed(this::afterProcess);

            if (!isDefaultTitle) {
                isDefaultTitle = true;
                AppService.get().updateChatSessionTitle(sessionId, message.trim());
            }

            ChatMessage assistantMessage = ChatMessage.toAssistant("");
            historyMessages.add(userMessage);
            historyMessages.add(assistantMessage);

            Node userView = MessageView.build(userMessage).setCharacter(character).get();
            Node assistantView = MessageView.build(assistantMessage).setCharacter(character).get(handler.messageProperty());
            messageInputText.clear();
            messagesContainer.getChildren().addAll(userView, assistantView);

            cancelChatIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> cancelChat(handler));
            handler.start();
        } catch (ResourceException re) {
            AppService.get().showInfoDialog(MessageI18N.get("ui.message.character.busy"));
        } catch (Exception e) {
            log.error("Create chat handler error", e);
            AppService.get().showErrorDialog(MessageI18N.get("ui.message.runtime.error", e.getMessage()));
        }
    }

    private void startChat() {
        String message = messageInputText.getText();
        startChat(message);
    }

    private void exportHistory() {
        if (historyMessages.isEmpty()) {
            AppService.get().showInfoDialog(MessageI18N.get("ui.message.history.empty"));
            return;
        }

        CharacterConfig character = ChatService.get().getCharacterConfig();

        StringBuilder sb = new StringBuilder();
        for (ChatMessage message : historyMessages) {
            String name = message.getRole() == ChatMessage.ChatRole.USER ?
                    character.getModelConfig().getGenerateParameter().getUser()
                    : character.getModelConfig().getGenerateParameter().getAssistant();
            sb.append(name).append(": ").append(message.getContent()).append("\n\n");
        }

        String exportFilePath = StringUtils.join(
                System.getProperty("user.home"), File.separator,
                character.getName(), "_", AppConstants.DT_FORMATTER_MONTH.format(LocalDateTime.now()), ".txt");

        try {
            FileUtils.writeFile(exportFilePath, sb.toString());
            AppService.get().showInfoDialog(MessageI18N.get("ui.message.export.success", exportFilePath));
        } catch (Exception e) {
            log.error("", e);
            AppService.get().showErrorDialog(MessageI18N.get("ui.message.runtime.error", e.getMessage()));
        }
    }

    private Label createMessageLabel(String name) {
        String text = AppConstants.DT_FORMATTER_FULL.format(LocalDateTime.now()) + " · " + name;
        Label label = new Label(text);
        label.getStyleClass().add("chat-message-tips");
        return label;
    }

    private void resetChat() {
        messagesContainer.getChildren().clear();
        historyMessages.clear();
        isDefaultTitle = false;

        CharacterConfig character;
        ChatMessage defaultMessage;

        if (DEFAULT_SESSION_ID.equals(sessionId)) {
            character = CharacterConfig.builder().name("Welcome").icon("AI bot").build();
            defaultMessage = ChatMessage.toAssistant(MessageI18N.get("ui.app.welcome"));
        } else {
            ChatService.get().removeChatSession(sessionId);
            character = ChatService.get().getCharacterConfig();
            defaultMessage = ChatMessage.toAssistant(MessageI18N.get("ui.chat.message.welcome"));
        }

        Node view = MessageView.build(defaultMessage).setCharacter(character).get();
        messagesContainer.getChildren().addAll(createMessageLabel(character.getName()), view);
    }

    private void regenerate() {
        if (historyMessages.isEmpty()) {
            AppService.get().showInfoDialog(MessageI18N.get("ui.message.history.empty"));
            return;
        }

        if (ChatService.get().isRunning()) {
            AppService.get().showInfoDialog(MessageI18N.get("ui.message.character.busy"));
        } else {
            List<ChatMessage> lastMessages = historyMessages.subList(historyMessages.size() - 2, historyMessages.size());
            ChatMessage userMessage = lastMessages.stream().filter(message -> ChatMessage.ChatRole.USER == message.getRole()).findFirst().orElse(null);
            if (userMessage == null) {
                log.error("Cannot found user message in chat history, history list: {}", JsonUtils.toJson(historyMessages));
                return;
            }
            ObservableList<Node> nodes = messagesContainer.getChildren();
            nodes.remove(nodes.size() - 1);
            nodes.remove(nodes.size() - 1);
            historyMessages.remove(historyMessages.size() - 1);
            historyMessages.remove(historyMessages.size() - 1);

            startChat(userMessage.getContent());
        }
    }

    private void deleteSession() {
        if (AppService.get().showConfirmDialog(MessageI18N.get("ui.message.session.remove"))) {
            AppService.get().deleteChatSession(sessionId);
        }
    }

    private void cancelChat(ChatService.ChatHandler chatHandler) {
        if (chatHandler.isRunning() && AppService.get().showConfirmDialog(MessageI18N.get("ui.message.session.stop"))) {
            chatHandler.cancel();
        }
    }

    private void createContextMenu() {
        MFXContextMenuItem exportHistoryItem = MFXContextMenuItem.Builder.build()
                .setIcon(new MFXFontIcon("fas-file-export", 16))
                .setText(MessageI18N.get("ui.menu.session.export"))
                .setOnAction(event -> exportHistory())
                .get();

        MFXContextMenuItem clearHistoryItem = MFXContextMenuItem.Builder.build()
                .setIcon(new MFXFontIcon("fas-delete-left", 16))
                .setText(MessageI18N.get("ui.menu.session.reset"))
                .setOnAction(event -> resetChat())
                .get();

        MFXContextMenuItem repeatChatItem = MFXContextMenuItem.Builder.build()
                .setIcon(new MFXFontIcon("fas-repeat", 16))
                .setText(MessageI18N.get("ui.menu.session.regenerate"))
                .setOnAction(event -> regenerate())
                .get();

        MFXContextMenuItem deleteSessionItem = MFXContextMenuItem.Builder.build()
                .setIcon(new MFXFontIcon("fas-trash-arrow-up", 16))
                .setText(MessageI18N.get("ui.menu.session.remove"))
                .setOnAction(event -> deleteSession())
                .get();


        contextMenu = MFXContextMenu.Builder.build(messageScrollPane)
                .addItems(exportHistoryItem, clearHistoryItem, repeatChatItem)
                .addLineSeparator()
                .addItem(deleteSessionItem)
                .setPopupStyleableParent(messageScrollPane)
                .installAndGet();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        messageInputText.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && event.isShiftDown()) {
                event.consume();
                startChat();
            }
        });
        messageInputText.clear();

        chatToolbar.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getClickCount() == 2) {
                if (messageInputText.getPrefHeight() == 100) {
                    messageInputText.setPrefHeight(messageInputText.getPrefHeight() + 100);
                } else {
                    messageInputText.setPrefHeight(100);
                }
            }
        });

        //toolbar button click events
        exportHistoryIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> exportHistory());
        resetIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> resetChat());
        startChatIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> startChat());
        regenerateIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> regenerate());
        deleteSessionIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> deleteSession());

        ScrollUtils.addSmoothScrolling(messageScrollPane, 0.5);

        messagesContainer.heightProperty().addListener((observable, oldValue, newValue) -> messageScrollPane.setVvalue(messageScrollPane.getVmax()));
        createContextMenu();
        resetChat();
    }
}
