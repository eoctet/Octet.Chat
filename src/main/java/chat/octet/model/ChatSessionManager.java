package chat.octet.model;

import chat.octet.model.exceptions.ModelException;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentMap;

@Slf4j

public class ChatSessionManager {
    public static final int CHAT_SESSION_LIMIT = 256;

    private static final ConcurrentMap<String, ChatSession> CHAT_SESSION_CACHE = Maps.newConcurrentMap();
    private static volatile ChatSessionManager manager;

    private ChatSessionManager() {
    }

    public static ChatSessionManager getInstance() {
        if (manager == null) {
            synchronized (ChatSessionManager.class) {
                if (manager == null) {
                    manager = new ChatSessionManager();
                }
            }
        }
        return manager;
    }

    protected ChatSession createChatSession(String user) {
        boolean exists = CHAT_SESSION_CACHE.containsKey(user);
        if (!exists) {
            if (CHAT_SESSION_CACHE.size() > CHAT_SESSION_LIMIT) {
                throw new ModelException("Cache session size is out of limit: " + CHAT_SESSION_LIMIT);
            }
            ChatSession session = new ChatSession(user);
            CHAT_SESSION_CACHE.put(user, session);
            log.debug("Create new chat session, User id: {}, chat session cache size: {}.", session.getId(), CHAT_SESSION_CACHE.size());
        }
        return CHAT_SESSION_CACHE.get(user);
    }

    public void removeChatSession(String id) {
        boolean exists = CHAT_SESSION_CACHE.containsKey(id);
        if (exists) {
            ChatSession session = CHAT_SESSION_CACHE.remove(id);
            if (session != null) {
                session.destroy();
            }
            log.info("Removed chat session, User id: {}.", id);
        }
    }

    public void removeAllSessions() {
        int size = CHAT_SESSION_CACHE.size();
        CHAT_SESSION_CACHE.keySet().forEach(this::removeChatSession);
        log.info("Removed all chat sessions, size: {}.", size);
    }

}
