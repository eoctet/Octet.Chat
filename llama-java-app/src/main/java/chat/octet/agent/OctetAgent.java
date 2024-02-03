package chat.octet.agent;


import chat.octet.agent.plugin.FormatUtils;
import chat.octet.agent.plugin.PluginManager;
import chat.octet.agent.plugin.model.ThoughtProcess;
import chat.octet.api.CharacterModelBuilder;
import chat.octet.config.CharacterConfig;
import chat.octet.model.LlamaService;
import chat.octet.model.Model;
import chat.octet.model.beans.Token;
import chat.octet.model.parameters.GenerateParameter;
import chat.octet.model.utils.ColorConsole;
import chat.octet.model.utils.PromptBuilder;
import chat.octet.utils.JsonUtils;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static chat.octet.agent.plugin.FormatUtils.FINAL_ANSWER;
import static chat.octet.agent.plugin.FormatUtils.THOUGHT;


@Slf4j
public class OctetAgent implements AutoCloseable {

    public static final int AGENT_THINKING_LIMIT = 10;
    private final Model model;
    private final CharacterConfig characterConfig;
    private final GenerateParameter generateParams;
    private boolean firstTime;

    public OctetAgent(String characterName) {
        Preconditions.checkArgument(StringUtils.isNotBlank(characterName), "AI character name cannot be empty.");
        this.model = CharacterModelBuilder.getInstance().getCharacterModel(characterName);
        this.characterConfig = CharacterModelBuilder.getInstance().getCharacterConfig();
        this.generateParams = this.characterConfig.getGenerateParameter();
        PluginManager.getInstance().loadPlugins();
    }

    public OctetAgent(Model model, CharacterConfig characterConfig) {
        Preconditions.checkArgument(model != null, "Model cannot be null.");
        Preconditions.checkArgument(characterConfig != null, "AI character config cannot be null.");
        this.model = model;
        this.characterConfig = characterConfig;
        this.generateParams = characterConfig.getGenerateParameter();
        PluginManager.getInstance().loadPlugins();
    }

    private String getCompletionResult(List<Token> tokens) {
        return tokens.stream().map(Token::getText).collect(Collectors.joining());
    }

    private List<Token> splitTokens(List<Token> tokens, String keyword) {
        int[] answers = LlamaService.tokenize(keyword, false, true);
        for (int i = 0; i < tokens.size(); i++) {
            int nextIndex = i + 1;
            if (nextIndex < tokens.size() && tokens.get(i).getId() == answers[0] && tokens.get(nextIndex).getId() == answers[1]) {
                return tokens.subList(i + answers.length, tokens.size());
            }
        }
        return tokens;
    }

    private List<Token> inference(String question, boolean showThoughts) {
        String system = Optional.ofNullable(StringUtils.stripToNull(characterConfig.getPrompt())).orElse(PromptBuilder.DEFAULT_COMMON_SYSTEM);
        StringBuilder input = !firstTime ? new StringBuilder(FormatUtils.formatUserQuestion(question)) : new StringBuilder(question);
        firstTime = true;

        List<Token> tokens;
        int times = 0;
        while (true) {
            tokens = model.chat(generateParams, system, input.toString()).tokens();
            String response = getCompletionResult(tokens);

            ThoughtProcess thought = FormatUtils.formatAgentResponse(response);
            log.debug("#{} Agent thinking process: {}", ++times, JsonUtils.toJson(thought));
            //only for console
            if (showThoughts) {
                System.out.println("💡 " + ColorConsole.grey(Optional.ofNullable(thought.getThought()).orElse("...")));
            }

            if (StringUtils.isNotBlank(thought.getFinalAnswer())) {
                tokens = splitTokens(tokens, FINAL_ANSWER);
                break;
            }
            if (!thought.isComplete()) {
                if (StringUtils.isNotBlank(thought.getThought())) {
                    tokens = splitTokens(tokens, THOUGHT);
                }
                log.warn("Without a complete thinking process, break and return directly. Agent response: {}", response);
                break;
            }
            //calling tools chain
            String result = PluginManager.getInstance().execute(thought.getAction(), thought.getActionInput(), question);
            thought.setObservation(result);
            log.debug("=> Plugin execute result: {}", result);
            //continue to add prompt text
            if (times > 1) {
                input.append("\n").append(response).append(" ").append(result);
            } else {
                input.delete(0, input.length()).append(FormatUtils.formatUserQuestion(question)).append("\n").append(response).append(" ").append(result);
            }

            if (times >= AGENT_THINKING_LIMIT) {
                log.warn("Agent has exceeded the limit of thinking times and has now stopped..");
                break;
            }
        }
        return tokens;
    }

    public String completion(String question) {
        List<Token> tokens = inference(question, false);
        return StringUtils.strip(getCompletionResult(tokens));
    }

    public void chat(String question) {
        chat(question, false);
    }

    public void chat(String question, boolean showThoughts) {
        List<Token> tokens = inference(question, showThoughts);
        //Analog stream output
        for (int i = 0; i < tokens.size(); i++) {
            String text = tokens.get(i).getText();
            if (i < 3) {
                text = StringUtils.strip(text);
            }
            System.out.print(ColorConsole.cyan(text));
            try {
                Thread.sleep(12);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void close() {
        if (model != null) {
            model.close();
        }
    }
}
