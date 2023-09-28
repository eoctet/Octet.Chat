package chat.octet.test;

import chat.octet.model.Model;
import chat.octet.model.PromptBuilder;
import chat.octet.model.parameters.GenerateParameter;
import chat.octet.model.parameters.ModelParameter;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConsoleQA {
    private static final String MODEL_PATH = "/llama.cpp/models/llama2/ggml-model-7b-q6_k.gguf";

    public static void main(String[] args) {
        ModelParameter modelParams = ModelParameter.builder()
                .modelPath(MODEL_PATH)
                .threads(8)
                .contextSize(4096)
                .verbose(true)
                .build();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
             Model model = new Model(modelParams)) {

            GenerateParameter generateParams = GenerateParameter.builder().build();
            String system = "Answer the questions.";

            while (true) {
                System.out.print("\nQuestion: ");
                String input = bufferedReader.readLine();
                if (StringUtils.trimToEmpty(input).equalsIgnoreCase("exit")) {
                    break;
                }
                String question = PromptBuilder.toPrompt(system, input);
                model.generate(generateParams, question).forEach(e -> System.out.print(e.getText()));
                System.out.print("\n");
                model.metrics();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e);
            System.exit(1);
        }
    }
}
