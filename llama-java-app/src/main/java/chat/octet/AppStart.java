package chat.octet;


import chat.octet.api.ModelBuilder;
import chat.octet.model.Model;
import chat.octet.model.parameters.GenerateParameter;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class AppStart {

    private final static GenerateParameter DEFAULT_PARAMETER = GenerateParameter.builder().build();
    private static final Options OPTIONS = new Options();

    static {
        //Based parameters
        OPTIONS.addOption("h", "help", false, "Show this help message and exit.");
        OPTIONS.addOption(null, "app", true, "App launch type: cli | api (default: cli).");
        OPTIONS.addOption("c", "completions", false, "Use completions mode.");
        OPTIONS.addOption("m", "model", true, "Load model name, default: llama2-chat.");
        //Generate parameters
        OPTIONS.addOption(null, "system", true, "Set a system prompt.");
        OPTIONS.addOption(null, "temperature", true, "Adjust the randomness of the generated text (default: 0.8).");
        OPTIONS.addOption(null, "repeat-penalty", true, "Control the repetition of token sequences in the generated text (default: 1.1).");
        OPTIONS.addOption(null, "no-penalize-nl", true, "Disable penalization for newline tokens when applying the repeat penalty (default: true).");
        OPTIONS.addOption(null, "frequency-penalty", true, "Repeat alpha frequency penalty (default: 0.0, 0.0 = disabled)");
        OPTIONS.addOption(null, "presence-penalty", true, "Repeat alpha presence penalty (default: 0.0, 0.0 = disabled)");
        OPTIONS.addOption(null, "top-k", true, "Top-k sampling (default: 40, 0 = disabled).");
        OPTIONS.addOption(null, "top-p", true, "Top-p sampling (default: 0.9).");
        OPTIONS.addOption(null, "min-p", true, "Min-p sampling (default: 0.05, 0 = disabled).");
        OPTIONS.addOption(null, "tfs", true, "Enable tail free sampling with parameter z (default: 1.0, 1.0 = disabled).");
        OPTIONS.addOption(null, "typical", true, "Enable typical sampling sampling with parameter p (default: 1.0, 1.0 = disabled).");
        OPTIONS.addOption(null, "mirostat", true, "Enable Mirostat sampling, controlling perplexity during text generation (default: 0, 0 = disabled, 1 = Mirostat, 2 = Mirostat 2.0).");
        OPTIONS.addOption(null, "mirostat-lr", true, "Set the Mirostat learning rate, parameter eta (default: 0.1).");
        OPTIONS.addOption(null, "mirostat-ent", true, "Set the Mirostat target entropy, parameter tau (default: 5.0).");
        OPTIONS.addOption(null, "max-new-tokens", true, "Maximum new token generation size (default: 0 unlimited).");
        OPTIONS.addOption(null, "verbose-prompt", false, "Print the prompt before generating text.");
    }

    private static GenerateParameter parseCmdParameter(CommandLine cmd) {
        GenerateParameter.MirostatMode mirostatMode;
        String type = cmd.getOptionValue("mirostat", "0");
        if ("1".equals(type)) {
            mirostatMode = GenerateParameter.MirostatMode.V1;
        } else if ("2".equals(type)) {
            mirostatMode = GenerateParameter.MirostatMode.V2;
        } else {
            mirostatMode = GenerateParameter.MirostatMode.DISABLED;
        }

        return GenerateParameter.builder()
                .temperature(Float.parseFloat(cmd.getOptionValue("temperature", String.valueOf(DEFAULT_PARAMETER.getTemperature()))))
                .repeatPenalty(Float.parseFloat(cmd.getOptionValue("repeat-penalty", String.valueOf(DEFAULT_PARAMETER.getRepeatPenalty()))))
                .penalizeNl(Boolean.parseBoolean(cmd.getOptionValue("no-penalize-nl", "true")))
                .frequencyPenalty(Float.parseFloat(cmd.getOptionValue("frequency-penalty", String.valueOf(DEFAULT_PARAMETER.getFrequencyPenalty()))))
                .presencePenalty(Float.parseFloat(cmd.getOptionValue("presence-penalty", String.valueOf(DEFAULT_PARAMETER.getPresencePenalty()))))
                .topK(Integer.parseInt(cmd.getOptionValue("top-k", String.valueOf(DEFAULT_PARAMETER.getTopK()))))
                .topP(Float.parseFloat(cmd.getOptionValue("top-p", String.valueOf(DEFAULT_PARAMETER.getTopP()))))
                .minP(Float.parseFloat(cmd.getOptionValue("min-p", String.valueOf(DEFAULT_PARAMETER.getMinP()))))
                .tsf(Float.parseFloat(cmd.getOptionValue("tfs", String.valueOf(DEFAULT_PARAMETER.getTsf()))))
                .typical(Float.parseFloat(cmd.getOptionValue("typical", String.valueOf(DEFAULT_PARAMETER.getTypical()))))
                .mirostatMode(mirostatMode)
                .mirostatETA(Float.parseFloat(cmd.getOptionValue("mirostat-lr", String.valueOf(DEFAULT_PARAMETER.getMirostatETA()))))
                .mirostatTAU(Float.parseFloat(cmd.getOptionValue("mirostat-en", String.valueOf(DEFAULT_PARAMETER.getMirostatTAU()))))
                .maxNewTokenSize(Integer.parseInt(cmd.getOptionValue("max-new-tokens", String.valueOf(DEFAULT_PARAMETER.getMaxNewTokenSize()))))
                .verbosePrompt(cmd.hasOption("verbose-prompt"))
                .build();
    }

    public static void main(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(OPTIONS, args, false);

        if (cmd.hasOption("h") || cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("LLAMA-JAVA-APP", OPTIONS);
            System.exit(0);
        }
        String mode = cmd.getOptionValue("app", "api");
        if ("api".equalsIgnoreCase(StringUtils.trimToEmpty(mode))) {
            SpringApplication.run(AppStart.class, args);
        } else {
            String modelName = cmd.getOptionValue("model", ModelBuilder.DEFAULT_MODEL_NAME);
            boolean completions = cmd.hasOption("completions");

            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
                 Model model = ModelBuilder.getInstance().getModel(modelName)) {

                GenerateParameter generateParams = parseCmdParameter(cmd);
                String system = cmd.getOptionValue("system", "Answer the questions.");

                while (true) {
                    System.out.print("\n\nUser: ");
                    String input = bufferedReader.readLine();

                    if (StringUtils.trimToEmpty(input).equalsIgnoreCase("exit")) {
                        break;
                    }

                    if (!completions) {
                        System.out.print("AI: ");
                        model.chat(generateParams, system, input).output();
                    } else {
                        System.err.print(input);
                        model.generate(generateParams, input).output();
                    }
                    System.out.print("\n");
                    model.metrics();
                }
            } catch (Exception e) {
                System.err.println("Error: " + e);
                System.exit(1);
            }
        }
    }

}
