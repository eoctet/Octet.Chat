package chat.octet.model.components.prompt;

import chat.octet.model.beans.ChatMessage;
import chat.octet.model.exceptions.ModelException;
import chat.octet.model.functions.Function;
import chat.octet.model.functions.FunctionConstants;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default chat template formatter.
 * Provides a standardized method for handling and displaying chat prompts.
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@Slf4j
public class DefaultChatTemplateFormatter implements ChatTemplateFormatter {
    private final Jinjava jinJava;
    @Getter
    private String chatTemplate;

    public DefaultChatTemplateFormatter(String modelType, String defaultChatTemplate) {
        Preconditions.checkNotNull(modelType, "Model type name cannot be null");
        String resourcePath = "chat_templates/" + modelType.toLowerCase() + ".tmpl";

        JinjavaConfig config = JinjavaConfig.newBuilder().withTrimBlocks(true).build();
        this.jinJava = new Jinjava(config);
        try {
            this.chatTemplate = Resources.toString(Resources.getResource(resourcePath), Charsets.UTF_8);
            log.info("Loaded chat template from local resource: {}", resourcePath);
        } catch (Exception e) {
            if (StringUtils.isBlank(defaultChatTemplate)) {
                throw new ModelException("Failed to load local chat template: " + resourcePath, e);
            }
            this.chatTemplate = defaultChatTemplate;
            log.warn("Failed to load local chat template, use default template.");
        }
        log.debug("Created a new chat formatter, chat template: {}", this.chatTemplate);
    }

    public DefaultChatTemplateFormatter(String modelType) {
        this(modelType, null);
    }

    @Override
    public String format(List<ChatMessage> messages, List<Function> functions, boolean addGenerationPrompt, Map<String, Object> params) {
        Preconditions.checkNotNull(messages, "Chat messages cannot be null");
        //check chat template
        if (!StringUtils.contains(chatTemplate, "messages")) {
            throw new IllegalArgumentException("The chat template must contain the 'messages' variable");
        }

        Map<String, Object> context = Maps.newLinkedHashMap();
        if (params != null && !params.isEmpty()) {
            context.putAll(params);
        }
        if (StringUtils.contains(chatTemplate, "add_generation_prompt")) {
            context.put("add_generation_prompt", addGenerationPrompt);
        }

        if (StringUtils.contains(chatTemplate, "add_function_calls") && functions != null && !functions.isEmpty()) {
            context.put("add_function_calls", true);
            context.put("functions", functions);
            // filter chat messages and reformat user message
            List<ChatMessage> lists = Lists.newArrayList();
            for (ChatMessage message : messages) {
                if (ChatMessage.ChatRole.SYSTEM == message.getRole()) {
                    lists.add(message);
                } else if (ChatMessage.ChatRole.USER == message.getRole()) {
                    context.put(FunctionConstants.FUNCTION_TEMPLATE_ARGS_QUERY, message.getContent());
                    lists.add(message);
                } else if (ChatMessage.ChatRole.ASSISTANT == message.getRole() && message.getToolCalls() != null) {
                    context.put("function_call", true);
                } else if (ChatMessage.ChatRole.FUNCTION == message.getRole()) {
                    context.put("function_feedback", true);
                    context.put(FunctionConstants.FUNCTION_TEMPLATE_ARGS_RESULT,
                            Optional.ofNullable(message.getContent()).orElse(FunctionConstants.FUNCTION_TEMPLATE_ARGS_NONE));
                }
            }
            context.put("messages", lists);
        } else {
            context.put("messages", messages);
        }
        return jinJava.render(chatTemplate, context);
    }

}
