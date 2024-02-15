package chat.octet;


import chat.octet.api.CharacterModelBuilder;
import chat.octet.cli.CmdInteraction;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static chat.octet.api.CharacterModelBuilder.DEFAULT_CHARACTER_NAME;


@SpringBootApplication
public class AppStart {

    private static final Options OPTIONS = new Options();

    static {
        //Based parameters
        OPTIONS.addOption("h", "help", false, "Show this help message and exit.");
        OPTIONS.addOption(null, "app", true, "App launch type: cli | api (default: cli).");
        OPTIONS.addOption("c", "completions", false, "Use completions mode.");
        OPTIONS.addOption("ch", "character", true, "Load the specified AI character, default: llama2-chat.");
        OPTIONS.addOption("q", "questions", true, "Load the specified user question list, example: /PATH/questions.txt.");
    }

    public static void main(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(OPTIONS, args, false);

        if (cmd.hasOption("h") || cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Octet.Chat", OPTIONS);
            System.exit(0);
        }
        String mode = cmd.getOptionValue("app", "cli");
        if ("api".equalsIgnoreCase(StringUtils.trimToEmpty(mode))) {
            String characterName = cmd.getOptionValue("character", DEFAULT_CHARACTER_NAME);
            CharacterModelBuilder.getInstance().getCharacterModel(characterName);
            SpringApplication.run(AppStart.class, args);
        } else {
            CmdInteraction cli = new CmdInteraction(cmd);
            String questions = cmd.getOptionValue("questions", "");
            if (StringUtils.isBlank(questions)) {
                cli.interaction();
            } else {
                cli.automation();
            }
        }
    }

}
