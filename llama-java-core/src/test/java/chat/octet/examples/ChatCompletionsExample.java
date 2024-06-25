package chat.octet.examples;

import chat.octet.model.Model;
import chat.octet.model.beans.CompletionResult;

public class ChatCompletionsExample {
    private static final String MODEL_PATH = "/octet-chat/models/llama2/ggml-model-7b-q6_k.gguf";

    public static void main(String[] args) {
        try (Model model = new Model(MODEL_PATH)) {
            CompletionResult result = model.chat("Who are you?").result();
            System.out.println(result);
        }
    }
}
