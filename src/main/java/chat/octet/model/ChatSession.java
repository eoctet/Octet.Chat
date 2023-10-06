package chat.octet.model;


import chat.octet.model.beans.FinishReason;
import chat.octet.model.beans.Status;
import chat.octet.model.beans.Token;
import chat.octet.model.exceptions.ModelException;
import chat.octet.model.parameters.GenerateParameter;
import chat.octet.model.utils.PromptBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;


@Slf4j
public class ChatSession {
    @Getter
    private final int id;
    @Getter
    private final String user;
    private final Status status;
    private Generator chatGenerator;
    private String initialSystemPrompt;

    protected ChatSession(String user) {
        this.id = RandomUtils.nextInt(10000, 50000);
        this.user = user;
        this.status = new Status();
    }

    protected Iterable<Token> getChatGenerator(GenerateParameter generateParams, String system, String question) {
        if (chatGenerator != null) {
            //copy last generated status
            status.copyToStatus(chatGenerator.getStatus());
            //reset initial system prompt
            if (FinishReason.TRUNCATED == chatGenerator.getFinishReason()) {
                if (StringUtils.isBlank(system)) {
                    system = initialSystemPrompt;
                }
                initialSystemPrompt = null;
            }
            chatGenerator = null;
        }
        if (StringUtils.isNotBlank(system) && system.equals(initialSystemPrompt)) {
            system = null;
        }
        if (StringUtils.isNotBlank(system) && StringUtils.isBlank(initialSystemPrompt)) {
            initialSystemPrompt = system;
        }
        String prompt = PromptBuilder.toPrompt(system, question);
        Iterable<Token> tokenIterable = new Iterable<Token>() {
            private Generator generator = null;

            @Nonnull
            @Override
            public Iterator<Token> iterator() {
                if (generator == null) {
                    generator = new Generator(generateParams, prompt, id, status);
                }
                return generator;
            }

            @Override
            public void forEach(Consumer<? super Token> action) {
                Objects.requireNonNull(action);
                try {
                    for (Token token : this) {
                        action.accept(token);
                    }
                } catch (Exception e) {
                    throw new ModelException("Generate next token error ", e);
                }
            }
        };
        chatGenerator = (Generator) tokenIterable.iterator();
        return tokenIterable;
    }

    protected void destroy() {
        if (chatGenerator != null) {
            chatGenerator.clearCache();
            chatGenerator = null;
        }
        LlamaService.clearCache(id);
    }

}
