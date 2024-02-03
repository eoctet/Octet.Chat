package chat.octet.agent.plugin;


import chat.octet.agent.plugin.model.ThoughtProcess;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import java.util.Map;

public class FormatUtils {

    private FormatUtils() {
    }

    public static final String PROMPT_TEMPLATE = "Answer the following questions as best you can. You have access to the following tools:\n\n" +
            "${tool_descs}" +
            "Use the following format:\n\n" +
            "Question: the input question you must answer\n" +
            "Thought: you should always think about what to do\n" +
            "Action: the action to take, should be one of [${tool_names}]\n" +
            "Action Input: the input to the action\n" +
            "Observation: the result of the action\n" +
            "... (this Thought/Action/Action Input/Observation can be repeated zero or more times)\n" +
            "Thought: I now know the final answer\n" +
            "Final Answer: the final answer to the original input question\n\n" +
            "Begin!\n\n" +
            "Question: ${question}";

    public static final String PLUGINS_DESC_TEMPLATE = "${name_for_model}: Call this tool to interact with the ${name_for_human} API. What is the ${name_for_human} API useful for? ${description_for_model} Parameters: ${parameters} Format the arguments as a JSON object.";

    public static final String THOUGHT = "Thought:";
    public static final String ACTION = "Action:";
    public static final String ACTION_INPUT = "Action Input:";
    public static final String OBSERVATION = "Observation:";
    public static final String FINAL_ANSWER = "Final Answer:";

    public static String formatUserQuestion(String question) {
        Map<String, Object> params = Maps.newLinkedHashMap();
        params.put("tool_descs", PluginManager.getInstance().getAllPluginDescriptions());
        params.put("tool_names", PluginManager.getInstance().getAllPluginNames());
        params.put("question", question);
        return StringSubstitutor.replace(PROMPT_TEMPLATE, params);
    }

    public static String formatString(String str, String start, String end) {
        return StringUtils.substring(str, str.indexOf(start), str.indexOf(end)).replaceAll(start, "").trim();
    }

    public static String formatString(String str, String start) {
        return StringUtils.substring(str, str.indexOf(start), str.length()).replaceAll(start, "").trim();
    }

    public static ThoughtProcess formatAgentResponse(String response) {
        ThoughtProcess process = new ThoughtProcess();

        if (response.contains(THOUGHT) && response.contains(ACTION)) {
            process.setThought(formatString(response, THOUGHT, ACTION));
        } else if (response.contains(THOUGHT) && response.contains(FINAL_ANSWER)) {
            process.setThought(formatString(response, THOUGHT, FINAL_ANSWER));
        } else if (response.contains(THOUGHT)) {
            process.setThought(formatString(response, THOUGHT));
        }
        if (response.contains(ACTION) && response.contains(ACTION_INPUT)) {
            process.setAction(formatString(response, ACTION, ACTION_INPUT));
        }
        if (response.contains(ACTION_INPUT) && response.contains(OBSERVATION)) {
            process.setActionInput(formatString(response, ACTION_INPUT, OBSERVATION));
        } else if (response.contains(ACTION_INPUT)) {
            process.setThought(formatString(response, ACTION_INPUT));
        }
        if (response.contains(FINAL_ANSWER)) {
            process.setFinalAnswer(formatString(response, FINAL_ANSWER));
        }
        return process;
    }


}
