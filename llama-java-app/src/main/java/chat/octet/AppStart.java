package chat.octet;


import chat.octet.api.ModelBuilder;
import chat.octet.config.ModelConfig;
import chat.octet.model.Model;
import chat.octet.model.parameters.GenerateParameter;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@SpringBootApplication
public class AppStart {

    private static final Options OPTIONS = new Options();

    static {
        //Based parameters
        OPTIONS.addOption("h", "help", false, "Show this help message and exit.");
        OPTIONS.addOption(null, "app", true, "App launch type: cli | api (default: cli).");
        OPTIONS.addOption("c", "completions", false, "Use completions mode.");
        OPTIONS.addOption("m", "model", true, "Load model name, default: llama2-chat.");
    }

    public static void main(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(OPTIONS, args, false);

        if (cmd.hasOption("h") || cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("LLAMA-JAVA-APP", OPTIONS);
            System.exit(0);
        }
        String mode = cmd.getOptionValue("app", "cli");
        if ("api".equalsIgnoreCase(StringUtils.trimToEmpty(mode))) {
            SpringApplication.run(AppStart.class, args);
        } else {
            String modelName = cmd.getOptionValue("model", "flows");
            boolean completions = cmd.hasOption("completions");

            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
                 Model model = ModelBuilder.getInstance().getModel(modelName)) {

                ModelConfig config = ModelBuilder.getInstance().getModelConfig();
                GenerateParameter generateParams = config.getGenerateParameter();
                String system = Optional.ofNullable(config.getPrompt()).orElse("Answer the questions.");

                while (true) {
                    System.out.print("\n\n" + generateParams.getUser() + ": ");
                    String input = bufferedReader.readLine();

                    if (StringUtils.trimToEmpty(input).equalsIgnoreCase("exit")) {
                        break;
                    }

                    if (!completions) {
                        System.out.print(generateParams.getAssistant() + ": ");
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
