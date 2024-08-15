package chat.octet.utils;


import chat.octet.model.beans.ChatMessage;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class Shortcuts {

    public static final String FUNCTION_CALL = "@func";

    private Shortcuts() {
    }

    public static boolean useFunctionCall(List<ChatMessage> messages) {
        ChatMessage lastChatMessage = messages.get(messages.size() - 1);
        boolean useFunctionCall = false;
        if (ChatMessage.ChatRole.USER == lastChatMessage.getRole()) {
            String content = lastChatMessage.getContent();
            useFunctionCall = StringUtils.startsWithIgnoreCase(content.trim(), FUNCTION_CALL);
            if (useFunctionCall) {
                lastChatMessage.setContent(StringUtils.substring(content, StringUtils.indexOfIgnoreCase(content, FUNCTION_CALL) + 5).trim());
            }
        }
        return useFunctionCall;
    }

}
