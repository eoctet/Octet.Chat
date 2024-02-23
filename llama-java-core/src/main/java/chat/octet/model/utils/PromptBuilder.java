package chat.octet.model.utils;

import chat.octet.model.enums.ModelType;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;

/**
 * Prompt builder
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
public class PromptBuilder {

    private PromptBuilder() {
    }

    /**
     * Default prompt of Alpaca model.
     */
    public static final String DEFAULT_ALPACA_SYSTEM = "Below is an instruction that describes a task. Write a response that appropriately completes the request.";

    /**
     * Default common prompt.
     */
    public static final String DEFAULT_COMMON_SYSTEM = "You are a helpful, respectful and honest assistant. Always answer as helpfully as possible, while being safe.  Your answers should not include any harmful, unethical, racist, sexist, toxic, dangerous, or illegal content. Please ensure that your responses are socially unbiased and positive in nature.\n" +
            "If a question does not make any sense, or is not factually coherent, explain why instead of answering something not correct. If you don't know the answer to a question, please don't share false information.";

    /**
     * Format user question as prompt text.
     *
     * @param modelType Model type.
     * @param question  User question.
     * @return Prompt text.
     * @see ModelType
     */
    public static String format(ModelType modelType, String question) {
        return format(modelType, null, question);
    }

    /**
     * Format user question as prompt text.
     *
     * @param modelType Model type.
     * @param system    System prompt.
     * @param question  User question.
     * @return Prompt text.
     * @see ModelType
     */
    public static String format(ModelType modelType, String system, String question) {
        Preconditions.checkNotNull(question, "User question cannot be null");
        String formatSystem = StringUtils.EMPTY;
        if (StringUtils.isNotBlank(system)) {
            if (StringUtils.isNotBlank(modelType.getSystemTemplate())) {
                formatSystem = MessageFormat.format(modelType.getSystemTemplate(), system) + "\n";
            } else {
                formatSystem = system + "\n";
            }
        }
        return MessageFormat.format(modelType.getPromptTemplate(), formatSystem, question);
    }

}
