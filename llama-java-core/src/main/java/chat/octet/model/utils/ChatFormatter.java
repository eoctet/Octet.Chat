package chat.octet.model.utils;


import chat.octet.model.beans.ChatMessage;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.hubspot.jinjava.Jinjava;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;

/**
 * Chat formatter.
 * Provides a standardized method for handling and displaying chat prompts,
 * ensuring consistency and readability of the chat messages.
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@Slf4j
public class ChatFormatter {

    /**
     * Default system prompt.
     */
    public static final String DEFAULT_COMMON_SYSTEM = "You are a helpful, respectful and honest assistant. Always answer as helpfully as possible, while being safe.  Your answers should not include any harmful, unethical, racist, sexist, toxic, dangerous, or illegal content. Please ensure that your responses are socially unbiased and positive in nature.\n" +
            "If a question does not make any sense, or is not factually coherent, explain why instead of answering something not correct. If you don't know the answer to a question, please don't share false information.";

    //default chat template
    public static final String CHATML_CHAT_TEMPLATE = "{% for message in messages %}{{'<|im_start|>' + message['role'] + '\n' + message['content'] + '<|im_end|>' + '\n'}}{% endfor %}{% if add_generation_prompt %}{{ '<|im_start|>assistant\n' }}{% endif %}";
    public static final String CHATML_BOS_TOKEN = "<s>";
    public static final String CHATML_EOS_TOKEN = "<|im_end|>";

    //jinjava instance
    private final Jinjava jinJava;

    @Getter
    private final String template;
    @Getter
    private final String bos;
    @Getter
    private final String eos;

    public ChatFormatter(String template, String bos, String eos) {
        this.jinJava = new Jinjava();
        this.template = template;
        this.bos = bos;
        this.eos = eos;
        log.debug("Created a new chat formatter, bos: {}, eos: {}, chat template: {}", bos, eos, template);
    }

    public ChatFormatter(String template) {
        this(template, "", "");
    }

    public ChatFormatter() {
        this(CHATML_CHAT_TEMPLATE, CHATML_BOS_TOKEN, CHATML_EOS_TOKEN);
    }

    /**
     * Formats a prompt text based on the provided chat messages.
     *
     * @param addGenerationPrompt A flag indicating whether to add a generation prompt. If true, a generation prompt will be added to the generated text.
     * @param messages            The chat messages to be formatted. This is a variable argument parameter, allowing for the passing of multiple chat messages.
     * @return The formatted prompt text string.
     * @throws NullPointerException if the chat messages array is null.
     */
    public String format(boolean addGenerationPrompt, ChatMessage... messages) {
        Preconditions.checkNotNull(messages, "Chat messages cannot be null");

        Map<String, Object> context = Maps.newLinkedHashMap();
        context.put("messages", Arrays.asList(messages));
        if (StringUtils.contains(template, "bos_token")) {
            context.put("bos_token", bos);
        }
        if (StringUtils.contains(template, "eos_token")) {
            context.put("eos_token", eos);
        }
        if (StringUtils.contains(template, "add_generation_prompt")) {
            context.put("add_generation_prompt", addGenerationPrompt);
        }

        return jinJava.render(template, context);
    }

    /**
     * Formats a prompt text based on the provided chat messages.
     *
     * @param messages The chat messages to be formatted. This is a variable argument parameter, allowing for the passing of multiple chat messages.
     * @return The formatted prompt text string.
     * @throws NullPointerException if the chat messages array is null.
     */
    public String format(ChatMessage... messages) {
        return format(true, messages);
    }

    /**
     * Formats a prompt text based on the provided system prompt and user question.
     *
     * @param system   The system prompt text.
     * @param question The user's question to be incorporated into the prompt text.
     * @return The formatted prompt text string.
     * @throws NullPointerException If the user question is null.
     */
    public String format(String system, String question) {
        Preconditions.checkNotNull(question, "User question cannot be null");
        if (StringUtils.isNotBlank(system)) {
            return format(ChatMessage.toSystem(system), ChatMessage.toUser(question));
        } else {
            return format(ChatMessage.toUser(question));
        }
    }

    /**
     * Formats a prompt text based on the provided user question.
     *
     * @param question The user's question to be incorporated into the prompt text.
     * @return The formatted prompt text string.
     * @throws NullPointerException If the user question is null.
     */
    public String format(String question) {
        return format(null, question);
    }


}
