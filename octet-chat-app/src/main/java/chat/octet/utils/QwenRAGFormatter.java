package chat.octet.utils;


import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

import static chat.octet.model.functions.FunctionConstants.*;

public class QwenRAGFormatter {

    public static final String THOUGHT = "Thought:";
    public static final String ACTION = "Action:";
    public static final String ACTION_INPUT = "Action Input:";
    public static final String OBSERVATION = "Observation:";
    public static final String FINAL_ANSWER = "Final Answer:";

    private QwenRAGFormatter() {
    }

    private static String formatString(String str, String start, String end) {
        return StringUtils.substring(str, str.indexOf(start), str.indexOf(end)).replaceAll(start, "").trim();
    }

    private static String formatString(String str, String start) {
        return StringUtils.substring(str, str.indexOf(start), str.length()).replaceAll(start, "").trim();
    }

    public static Map<String, Object> parseResponse(String response) {
        Map<String, Object> result = Maps.newLinkedHashMap();
        if (response.contains(THOUGHT) && response.contains(ACTION)) {
            result.put(FUNCTION_TEMPLATE_ARGS_ASSISTANT, formatString(response, THOUGHT, ACTION));
        } else if (response.contains(THOUGHT) && response.contains(FINAL_ANSWER)) {
            result.put(FUNCTION_TEMPLATE_ARGS_ASSISTANT, formatString(response, THOUGHT, FINAL_ANSWER));
        } else if (response.contains(THOUGHT)) {
            result.put(FUNCTION_TEMPLATE_ARGS_ASSISTANT, formatString(response, THOUGHT));
        }
        if (response.contains(ACTION) && response.contains(ACTION_INPUT)) {
            result.put(FUNCTION_TEMPLATE_ARGS_NAME, formatString(response, ACTION, ACTION_INPUT));
        }
        if (response.contains(ACTION_INPUT) && response.contains(OBSERVATION)) {
            result.put(FUNCTION_TEMPLATE_ARGS_INPUT, formatString(response, ACTION_INPUT, OBSERVATION));
        } else if (response.contains(ACTION_INPUT)) {
            result.put(FUNCTION_TEMPLATE_ARGS_ASSISTANT, formatString(response, ACTION_INPUT));
        }
        if (response.contains(FINAL_ANSWER)) {
            result.put(FUNCTION_TEMPLATE_ARGS_ASSISTANT, formatString(response, FINAL_ANSWER));
        }
        if (!result.containsKey(FUNCTION_TEMPLATE_ARGS_ASSISTANT)) {
            result.put(FUNCTION_TEMPLATE_ARGS_ASSISTANT, response);
        }
        return result;
    }


}
