package chat.octet.test;

import chat.octet.model.Model;
import chat.octet.model.parameters.GenerateParameter;

public class ContinuousChatExample {

    private static final String MODEL_PATH = "/llama.cpp/models/llama2/ggml-model-7b-q6_k.gguf";

    public static void main(String[] args) {
        String system = "You are a helpful assistant. ";
        String[] questions = new String[]{
                "List five emojis about food and explain their meanings",
                "Write a fun story based on the third emoji",
                "Continue this story and refine it",
                "Summarize a title for this story, extract five keywords, and the keywords should not exceed five words",
                "Mark the characters, time, and location of this story",
                "Great, translate this story into Chinese",
                "Who are you and why are you here?",
                "Summarize today's conversation"
        };

        GenerateParameter generateParams = GenerateParameter.builder()
                .verbosePrompt(true)
                .user("William")
                .build();

        try (Model model = new Model(MODEL_PATH)) {
            for (String question : questions) {
                //Example 1: Continuous generation example.
                //String text = PromptBuilder.toPrompt(system, question);
                //model.generate(generateParams, text).forEach(e -> System.out.print(e.getText()));

                //Example 2: Continuous chat example
                model.chat(generateParams, system, question).forEach(e -> System.out.print(e.getText()));
                System.out.println("\n");
                model.metrics();
            }
        }
    }
}
