package chat.octet.agent;


import chat.octet.agent.plugin.FormatUtils;
import chat.octet.agent.plugin.PluginManager;
import chat.octet.agent.plugin.model.ThoughtProcess;
import chat.octet.model.Model;
import chat.octet.model.TokenDecoder;
import chat.octet.model.beans.Token;
import chat.octet.model.enums.LlamaTokenType;
import chat.octet.model.parameters.GenerateParameter;
import chat.octet.model.utils.ColorConsole;
import chat.octet.utils.JsonUtils;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static chat.octet.agent.plugin.FormatUtils.FINAL_ANSWER;
import static chat.octet.agent.plugin.FormatUtils.THOUGHT;

@Slf4j
public class OctetAgent {
    private static volatile OctetAgent instance;
    private final Map<String, Boolean> status = Maps.newConcurrentMap();

    private OctetAgent() {
    }

    public static OctetAgent getInstance() {
        if (instance == null) {
            synchronized (OctetAgent.class) {
                if (instance == null) {
                    instance = new OctetAgent();

                }
            }
        }
        return instance;
    }

    private boolean getStatus(GenerateParameter generateParams) {
        String key = StringUtils.isBlank(generateParams.getSession()) ? generateParams.getUser() : (generateParams.getUser() + ":" + generateParams.getSession());
        status.put(key, status.containsKey(key));
        return status.get(key);
    }

    public Generator chat(Model model, GenerateParameter generateParams, String system, String question) {
        return new Generator(model, generateParams, system, question, true, getStatus(generateParams));
    }

    public void output(Model model, GenerateParameter generateParams, String system, String question) {
        Generator generator = new Generator(model, generateParams, system, question, false, getStatus(generateParams));

        while (generator.iterator().hasNext()) {
            List<Token> tokens = generator.iterator().next();
            if (!tokens.isEmpty()) {
                //Analog stream output
                for (int i = 0; i < tokens.size(); i++) {
                    String text = tokens.get(i).getText();
                    if (i < 3) {
                        text = StringUtils.strip(text);
                    }
                    System.out.print(ColorConsole.cyan(text));
                }
            }
        }
    }

    public void reset(String key) {
        status.remove(key);
    }

    public void reset() {
        status.clear();
    }

    public static class Generator implements Iterable<List<Token>> {
        public static final int AGENT_THINKING_LIMIT = 5;
        private final Inference inference;

        public Generator(Model model, GenerateParameter generateParams, String system, String question, boolean apiEnabled, boolean firstTime) {
            this.inference = new Inference(model, generateParams, system, question, apiEnabled, firstTime);
        }

        @NotNull
        @Override
        public Iterator<List<Token>> iterator() {
            return inference;
        }

        @Slf4j
        private static class Inference implements Iterator<List<Token>> {
            private final Model model;
            private final GenerateParameter generateParams;
            private final String system;
            private final String question;
            private final boolean apiEnabled;
            private final StringBuffer input;
            private boolean finished = false;
            private int times = 0;

            public Inference(Model model, GenerateParameter generateParams, String system, String question, boolean apiEnabled, boolean firstTime) {
                this.model = model;
                this.generateParams = generateParams;
                this.system = system;
                this.question = question;
                this.apiEnabled = apiEnabled;
                this.input = !firstTime ? new StringBuffer(FormatUtils.formatUserQuestion(question)) : new StringBuffer(question);
            }

            @Override
            public boolean hasNext() {
                return !finished;
            }

            @Override
            public List<Token> next() {
                List<Token> tokens = model.chat(generateParams, system, input.toString()).tokens();
                String response = tokens.stream().map(Token::getText).collect(Collectors.joining());

                ThoughtProcess thought = FormatUtils.formatAgentResponse(response);
                log.debug("#{} Agent thinking process: {}", ++times, JsonUtils.toJson(thought));

                if (StringUtils.isNotBlank(thought.getFinalAnswer())) {
                    tokens = TokenDecoder.subTokensBetween(tokens, FINAL_ANSWER);
                    if (apiEnabled) {
                        tokens.add(new Token(-1, LlamaTokenType.LLAMA_TOKEN_TYPE_USER_DEFINED, "[DONE]"));
                    }
                    finished = true;
                    return tokens;
                }
                if (thought.isComplete()) {
                    //calling tools chain
                    String result = PluginManager.getInstance().execute(thought.getAction(), thought.getActionInput(), question);
                    thought.setObservation(result);
                    tokens.clear();
                    log.debug("=> Plugin execute result: {}", result);
                    //continue to add prompt text
                    if (times > 1) {
                        input.append("\n").append(response).append(" ").append(result);
                    } else {
                        input.delete(0, input.length()).append(FormatUtils.formatUserQuestion(question)).append("\n").append(response).append(" ").append(result);
                    }
                } else {
                    if (StringUtils.isNotBlank(thought.getThought())) {
                        tokens = TokenDecoder.subTokensBetween(tokens, THOUGHT);
                        if (apiEnabled) {
                            tokens.add(new Token(-1, LlamaTokenType.LLAMA_TOKEN_TYPE_USER_DEFINED, "[DONE]"));
                        }
                    }
                    finished = true;
                }

                if (times >= AGENT_THINKING_LIMIT) {
                    log.warn("Agent has exceeded the limit of thinking times and has now stopped..");
                    finished = true;
                }
                return tokens;
            }
        }
    }

}
