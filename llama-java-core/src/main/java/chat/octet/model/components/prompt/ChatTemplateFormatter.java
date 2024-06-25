package chat.octet.model.components.prompt;


import chat.octet.model.beans.ChatMessage;
import chat.octet.model.functions.Function;

import java.util.List;
import java.util.Map;


/**
 * The ChatTemplateFormatter interface defines methods for formatting chat templates.
 * This interface aims to provide a standard way of formatting chat content to suit various inference requirements.
 * A chat template formatter should handle various elements of chat messages, such as system prompt, user question, and function calls,
 * combining these elements into a final prompt format based on a specific template.
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
public interface ChatTemplateFormatter {
    /**
     * Default system prompt.
     */
    String DEFAULT_COMMON_SYSTEM = "You are a helpful, respectful and honest assistant. Always answer as helpfully as possible, while being safe.  Your answers should not include any harmful, unethical, racist, sexist, toxic, dangerous, or illegal content. Please ensure that your responses are socially unbiased and positive in nature.\n" +
            "If a question does not make any sense, or is not factually coherent, explain why instead of answering something not correct. If you don't know the answer to a question, please don't share false information.";

    /**
     * Format chat messages to generate final prompt text.
     *
     * @param messages            Chat messages list, must contain at least one user message.
     * @param functions           Function list, if available, will attempt to format function call templates.
     * @param addGenerationPrompt Should add generation prompt suffix.
     * @param params              Chat template parameters.
     * @return Formatted prompt text.
     */
    String format(List<ChatMessage> messages, List<Function> functions, boolean addGenerationPrompt, Map<String, Object> params);

    /**
     * Format chat messages to generate final prompt text.
     *
     * @param messages            Chat messages list, must contain at least one user message.
     * @param addGenerationPrompt Should add generation prompt suffix.
     * @param params              Chat template parameters.
     * @return Formatted prompt text.
     */
    default String format(List<ChatMessage> messages, boolean addGenerationPrompt, Map<String, Object> params) {
        return format(messages, null, addGenerationPrompt, params);
    }

    /**
     * Format chat messages to generate final prompt text.
     *
     * @param messages            Chat messages list, must contain at least one user message.
     * @param addGenerationPrompt Should add generation prompt suffix.
     * @return Formatted prompt text.
     */
    default String format(List<ChatMessage> messages, boolean addGenerationPrompt) {
        return format(messages, null, addGenerationPrompt, null);
    }

    /**
     * Format chat messages to generate final prompt text.
     *
     * @param messages Chat messages list, must contain at least one user message.
     * @return Formatted prompt text.
     */

    default String format(List<ChatMessage> messages) {
        return format(messages, null, false, null);
    }
}
