package chat.octet.model.utils;

import chat.octet.model.enums.ModelType;
import org.apache.commons.lang3.StringUtils;

/**
 * Prompt builder
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
public class PromptBuilder {

    public static final String DEFAULT_SYSTEM = "Below is an instruction that describes a task. Write a response that appropriately completes the request.";

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
        String formateSystem = StringUtils.EMPTY;
        switch (modelType) {
            case ALPACA:
                formateSystem = StringUtils.isBlank(system) ? "\n\n" : StringUtils.join(system, "\n\n");
                return StringUtils.join(formateSystem, "### Instruction:\n", question, "\n\n### Response:\n");
            case SNOOZY:
                formateSystem = StringUtils.isBlank(system) ? "\n" : StringUtils.join("### Instruction:\n", system, "\n\n");
                return StringUtils.join(formateSystem, "### Prompt\n", question, "\n### Response\n");
            case VICUNA:
                formateSystem = StringUtils.isBlank(system) ? "\n\n" : StringUtils.join(system, "\n\n");
                return StringUtils.join(formateSystem, "USER:\n", question, "\n\nASSISTANT:\n");
            case OPEN_BUDDY:
                formateSystem = StringUtils.isBlank(system) ? "\n" : StringUtils.join(system, "\n\n");
                return StringUtils.join(formateSystem, "User:\n", question, "\nAssistant:\n");
            case OASST_LLAMA:
                formateSystem = StringUtils.isBlank(system) ? "\n\n" : StringUtils.join("[INST] <<SYS>>\n", system, "\n<</SYS>>\n\n");
                return StringUtils.join(formateSystem, "<|prompter|>\n", question, "\n\n<|assistant|>\n");
            case REDPAJAMA_INCITE:
                formateSystem = StringUtils.isBlank(system) ? "\n" : StringUtils.join(system, "\n\n");
                return StringUtils.join(formateSystem, "<human>\n", question, "\n<bot>\n");
            case LLAMA2:
            default:
                if (StringUtils.isNotBlank(system)) {
                    formateSystem = StringUtils.join("<<SYS>>\n", system, "\n<</SYS>>\n\n");
                }
                return StringUtils.join("[INST] ", formateSystem, question, " [/INST] ");
        }
    }

}
