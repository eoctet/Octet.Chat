package chat.octet.model.enums;


import lombok.Getter;

/**
 * Model type
 */
@Getter
public enum ModelType {
    LLAMA2(
            "",
            "<<SYS>>\n{0}\n<</SYS>>",
            "[INST] {0}{1} [/INST] "
    ),
    ALPACA(
            "\n\n",
            "",
            "{0}### Instruction:\n{1}\n\n### Response:\n"
    ),
    VICUNA(
            "\n\n",
            "",
            "{0}USER:\n{1}\n\nASSISTANT:\n"
    ),
    OASST_LLAMA(
            "\n\n",
            "[INST] <<SYS>>\n{0}\n<</SYS>>",
            "{0}<|prompter|>\n{1}\n\n<|assistant|>\n"
    ),
    OPEN_BUDDY(
            "\n",
            "",
            "{0}User: {1}\nAssistant: "
    ),
    REDPAJAMA_INCITE(
            "\n",
            "",
            "{0}<human>\n{1}\n<bot>\n"
    ),
    SNOOZY(
            "\n",
            "### Instruction:\n{0}",
            "{0}### Prompt\n{1}\n### Response\n"
    ),
    FALCON(
            "\n",
            "",
            "{0}User: {1}\nAssistant: "
    ),
    BAICHUAN(
            "\n",
            "",
            "{0}User: {1}\nAssistant: "
    ),
    AQUILA(
            "\n",
            "",
            "{0}User: {1}\nAssistant: "
    ),
    QWEN(
            "\n",
            "",
            "{0}User: {1}\nAssistant: "
    ),
    COMMON(
            "\n",
            "",
            "{0}User: {1}\nAssistant: "
    );

    private final String separator;
    private final String systemTemplate;
    private final String promptTemplate;

    ModelType(String separator, String systemTemplate, String promptTemplate) {
        this.separator = separator;
        this.systemTemplate = systemTemplate;
        this.promptTemplate = promptTemplate;
    }
}
