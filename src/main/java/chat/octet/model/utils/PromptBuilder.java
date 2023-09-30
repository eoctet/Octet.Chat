package chat.octet.model.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * Prompt builder
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
public class PromptBuilder {

    private static final String INST_BEGIN_SUFFIX = "[INST] ";
    private static final String INST_END_SUFFIX = " [/INST] ";
    private static final String SYS_BEGIN_SUFFIX = "<<SYS>>\n";
    private static final String SYS_END_SUFFIX = "\n<</SYS>>\n\n";

    public static String toPrompt(String system, String question) {
        if (StringUtils.isBlank(system)) {
            return toPrompt(question);
        }
        return StringUtils.join(INST_BEGIN_SUFFIX, SYS_BEGIN_SUFFIX, system, SYS_END_SUFFIX, question, INST_END_SUFFIX);
    }

    public static String toPrompt(String question) {
        if (StringUtils.isAnyBlank(question)) {
            throw new IllegalArgumentException("prompt parameter cannot be empty");
        }
        return StringUtils.join(INST_BEGIN_SUFFIX, question, INST_END_SUFFIX);
    }

}
