package chat.octet.examples;

import chat.octet.model.Model;
import chat.octet.model.beans.CompletionResult;
import chat.octet.model.parameters.GenerateParameter;

public class ChatCompletionsExample {
    private static final String MODEL_PATH = "/octet-chat/models/llama2/ggml-model-7b-q6_k.gguf";

    public static void main(String[] args) {
        GenerateParameter generateParams = GenerateParameter.builder().build();

        try (Model model = new Model(MODEL_PATH)) {
            CompletionResult result = model.chatCompletions(generateParams, "Who are you?");
            System.out.println(result);
        }
    }
}
