package chat.octet;


import chat.octet.api.CharacterModelBuilder;
import chat.octet.config.CharacterConfig;
import chat.octet.model.Model;
import chat.octet.model.parameters.GenerateParameter;
import chat.octet.model.utils.ColorConsole;
import chat.octet.model.utils.PromptBuilder;
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

    private static final String DEFAULT_CHARACTER_NAME = "llama2-chat";
    private static final Options OPTIONS = new Options();

    static {
        //Based parameters
        OPTIONS.addOption("h", "help", false, "Show this help message and exit.");
        OPTIONS.addOption(null, "app", true, "App launch type: cli | api (default: cli).");
        OPTIONS.addOption("c", "completions", false, "Use completions mode.");
        OPTIONS.addOption("ch", "character", true, "Load the specified AI character, default: llama2-chat.");
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
            String characterName = cmd.getOptionValue("character", DEFAULT_CHARACTER_NAME);
            boolean completions = cmd.hasOption("completions");

            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
                 Model model = CharacterModelBuilder.getInstance().getCharacterModel(characterName)) {

                CharacterConfig config = CharacterModelBuilder.getInstance().getCharacterConfig();
                GenerateParameter generateParams = config.getGenerateParameter();
                String system = Optional.ofNullable(config.getPrompt()).orElse(PromptBuilder.DEFAULT_COMMON_SYSTEM);

                while (true) {
                    String userInputPrefix = ColorConsole.green(generateParams.getUser() + ": ");
                    System.out.print("\n" + userInputPrefix);
                    String input = bufferedReader.readLine();

                    if (StringUtils.trimToEmpty(input).equalsIgnoreCase("exit")) {
                        break;
                    }

                    if (!completions) {
                        String botInputPrefix = ColorConsole.cyan(generateParams.getAssistant() + ": ");
                        System.out.print(botInputPrefix);
                        model.chat(generateParams, system, input).output();
                    } else {
                        System.out.print(ColorConsole.green(input));
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
