package chat.octet.examples;

import chat.octet.model.Model;
import chat.octet.model.beans.CompletionResult;
import chat.octet.model.parameters.GenerateParameter;

public class GrammarExample {
    private static final String MODEL_PATH = "/octet-chat/models/llama2/ggml-model-7b-q6_k.gguf";

    public static void main(String[] args) {

        String grammarRules = "root   ::= object\n" +
                "value  ::= object | array | string | number | (\"true\" | \"false\" | \"null\") ws\n" +
                "\n" +
                "object ::=\n" +
                "  \"{\" ws (\n" +
                "            string \":\" ws value\n" +
                "    (\",\" ws string \":\" ws value)*\n" +
                "  )? \"}\" ws\n" +
                "\n" +
                "array  ::=\n" +
                "  \"[\" ws (\n" +
                "            value\n" +
                "    (\",\" ws value)*\n" +
                "  )? \"]\" ws\n" +
                "\n" +
                "string ::=\n" +
                "  \"\\\"\" (\n" +
                "    [^\"\\\\] |\n" +
                "    \"\\\\\" ([\"\\\\/bfnrt] | \"u\" [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F]) # escapes\n" +
                "  )* \"\\\"\" ws\n" +
                "\n" +
                "number ::= (\"-\"? ([0-9] | [1-9] [0-9]*)) (\".\" [0-9]+)? ([eE] [-+]? [0-9]+)? ws\n" +
                "\n" +
                "# Optional space: by convention, applied in this grammar after literal chars when allowed\n" +
                "ws ::= ([ \\t\\n] ws)?\n";

        GenerateParameter generateParams = GenerateParameter.builder().grammarRules(grammarRules).build();

        try (Model model = new Model(MODEL_PATH)) {
            CompletionResult result = model.chatCompletions(generateParams, "Who are you?");
            System.out.println(result);
        }
    }
}
