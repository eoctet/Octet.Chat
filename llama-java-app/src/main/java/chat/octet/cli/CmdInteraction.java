package chat.octet.cli;

import chat.octet.agent.OctetAgent;
import chat.octet.api.CharacterModelBuilder;
import chat.octet.config.CharacterConfig;
import chat.octet.model.Model;
import chat.octet.model.enums.ModelType;
import chat.octet.model.parameters.GenerateParameter;
import chat.octet.model.utils.ColorConsole;
import chat.octet.model.utils.PromptBuilder;
import chat.octet.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static chat.octet.api.CharacterModelBuilder.DEFAULT_CHARACTER_NAME;

@Slf4j
public class CmdInteraction {

    private final String character;
    private final String questions;
    private final boolean completions;

    public CmdInteraction(CommandLine cmd) {
        this.character = cmd.getOptionValue("character", DEFAULT_CHARACTER_NAME);
        this.questions = cmd.getOptionValue("questions");
        this.completions = cmd.hasOption("completions");
    }

    private OctetAgent getOctetAgent(Model model, CharacterConfig config) {
        if (config.isAgentMode()) {
            if (ModelType.QWEN != ModelType.valueOf(model.getModelType())) {
                throw new IllegalArgumentException("AI Agent only supports Qwen series model");
            }
            return new OctetAgent(model, config);
        }
        return null;
    }

    private void execute(Model model, CharacterConfig config, String system, String input) {
        if (!completions) {
            String botInputPrefix = ColorConsole.cyan(config.getGenerateParameter().getAssistant() + ": ");
            System.out.print(botInputPrefix);

            OctetAgent agent = getOctetAgent(model, config);
            if (agent != null) {
                System.out.println(ColorConsole.grey("[ I'm thinking, please wait a moment.. ]"));
                agent.chat(input, true);
            } else {
                model.chat(config.getGenerateParameter(), system, input).output();
            }
        } else {
            System.out.print(ColorConsole.green(input));
            model.generate(config.getGenerateParameter(), input).output();
        }
        System.out.print("\n");
        model.metrics();
    }

    public void automation() {
        try (Model model = CharacterModelBuilder.getInstance().getCharacterModel(character)) {
            List<String> lines = CommonUtils.readFileLines(questions);

            CharacterConfig config = CharacterModelBuilder.getInstance().getCharacterConfig();
            GenerateParameter generateParams = config.getGenerateParameter();
            String system = Optional.ofNullable(StringUtils.stripToNull(config.getPrompt())).orElse(PromptBuilder.DEFAULT_COMMON_SYSTEM);

            for (String input : lines) {
                String question = StringUtils.trimToEmpty(input);
                if (StringUtils.isNotBlank(question)) {
                    System.out.println("\n" + ColorConsole.green(generateParams.getUser() + ": " + question));
                    execute(model, config, system, input);
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e);
            System.exit(1);
        }
    }

    public void interaction() {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
             Model model = CharacterModelBuilder.getInstance().getCharacterModel(character)) {

            CharacterConfig config = CharacterModelBuilder.getInstance().getCharacterConfig();
            GenerateParameter generateParams = config.getGenerateParameter();
            String system = Optional.ofNullable(StringUtils.stripToNull(config.getPrompt())).orElse(PromptBuilder.DEFAULT_COMMON_SYSTEM);

            while (true) {
                String userInputPrefix = ColorConsole.green(generateParams.getUser() + ": ");
                System.out.print("\n" + userInputPrefix);
                String input = bufferedReader.readLine();

                if (StringUtils.trimToEmpty(input).equalsIgnoreCase("exit")) {
                    break;
                }
                execute(model, config, system, input);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e);
            System.exit(1);
        }
    }
}
