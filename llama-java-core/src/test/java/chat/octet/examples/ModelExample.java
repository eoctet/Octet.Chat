package chat.octet.examples;

import chat.octet.model.Model;
import chat.octet.model.beans.ChatMessage;
import chat.octet.model.beans.CompletionResult;
import chat.octet.model.components.prompt.DefaultChatTemplateFormatter;
import chat.octet.model.enums.ModelType;
import chat.octet.model.parameters.GenerateParameter;
import com.google.common.collect.Lists;

public class ModelExample {
    private static final String MODEL_PATH = "/octet-chat/models/llama2/ggml-model-7b-q6_k.gguf";

    public static void main(String[] args) {
        GenerateParameter generateParams = GenerateParameter.builder().verbosePrompt(true).build();

        try (Model model = new Model(MODEL_PATH)) {

            //Example 1: continue writing the story
            //Model: llama2
            String text = "long time a ago";
            //streaming output
            model.generate(generateParams, text).output();

            //completion output
            CompletionResult result = model.completions(generateParams, text);
            System.out.println(result);

            //Example 2: Normal chat without memory prompt
            //Model: llama2-chat
            String system = "Answer the questions.";
            String question = "Who are you?";
            DefaultChatTemplateFormatter formatter = new DefaultChatTemplateFormatter(ModelType.LLAMA2.name());
            String prompt = formatter.format(Lists.newArrayList(ChatMessage.toSystem(system), ChatMessage.toUser(question)));
            //streaming output
            model.generate(prompt).output();

            //completion output
            CompletionResult answer = model.completions(generateParams, prompt);
            System.out.println(answer);

            //Example 3: Chat with memory prompt
            //Model: llama2-chat

            //streaming output
            model.chat(generateParams, system, question).output();
        }
    }
}
