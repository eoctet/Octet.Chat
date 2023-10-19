package chat.octet.model.utils;

import chat.octet.model.enums.ModelType;
import org.apache.commons.lang3.StringUtils;

/**
 * Prompt builder
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
public class PromptBuilder {

    public static final String DEFAULT_ALPACA_SYSTEM = "Below is an instruction that describes a task. Write a response that appropriately completes the request.";

    public static final String DEFAULT_COMMON_SYSTEM = "You are a helpful, respectful and honest assistant. Always answer as helpfully as possible, while being safe.  Your answers should not include any harmful, unethical, racist, sexist, toxic, dangerous, or illegal content. Please ensure that your responses are socially unbiased and positive in nature.\n" +
            "If a question does not make any sense, or is not factually coherent, explain why instead of answering something not correct. If you don't know the answer to a question, please don't share false information.";

    /**
     * Create prompt text
     *
     * @param modelType Model type.
     * @param question  User question.
     * @return Prompt text.
     * @see ModelType
     */
    public static String toPrompt(ModelType modelType, String question) {
        return toPrompt(modelType, null, question);
    }

    /**
     * Create prompt text
     *
     * @param modelType Model type.
     * @param system    System prompt (optional).
     * @param question  User question.
     * @return Prompt text.
     * @see ModelType
     */
    public static String toPrompt(ModelType modelType, String system, String question) {
        String formatSystem;
        switch (modelType) {
            case ALPACA:
                formatSystem = StringUtils.isBlank(system) ? "\n\n" : StringUtils.join(system, "\n\n");
                return StringUtils.join(formatSystem, "### Instruction:\n", question, "\n\n### Response:\n");
            case SNOOZY:
                formatSystem = StringUtils.isBlank(system) ? "\n" : StringUtils.join("### Instruction:\n", system, "\n\n");
                return StringUtils.join(formatSystem, "### Prompt\n", question, "\n### Response\n");
            case VICUNA:
                formatSystem = StringUtils.isBlank(system) ? "\n\n" : StringUtils.join(system, "\n\n");
                return StringUtils.join(formatSystem, "USER:\n", question, "\n\nASSISTANT:\n");
            case OASST_LLAMA:
                formatSystem = StringUtils.isBlank(system) ? "\n\n" : StringUtils.join("[INST] <<SYS>>\n", system, "\n<</SYS>>\n\n");
                return StringUtils.join(formatSystem, "<|prompter|>\n", question, "\n\n<|assistant|>\n");
            case REDPAJAMA_INCITE:
                formatSystem = StringUtils.isBlank(system) ? "\n" : StringUtils.join(system, "\n\n");
                return StringUtils.join(formatSystem, "<human>\n", question, "\n<bot>\n");
            case LLAMA2:
                formatSystem = StringUtils.isBlank(system) ? StringUtils.EMPTY : StringUtils.join("<<SYS>>\n", system, "\n<</SYS>>\n\n");
                return StringUtils.join("[INST] ", formatSystem, question, " [/INST] ");
            case OPEN_BUDDY:
            case FALCON:
            case BAICHUAN:
            case AQUILA:
            case COMMON:
            default:
                formatSystem = StringUtils.isBlank(system) ? "\n" : StringUtils.join(system, "\n\n");
                return StringUtils.join(formatSystem, "User: ", question, "\nAssistant: ");
        }
    }

}
