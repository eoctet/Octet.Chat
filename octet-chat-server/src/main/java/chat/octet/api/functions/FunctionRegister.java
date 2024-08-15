package chat.octet.api.functions;


import chat.octet.api.functions.model.FunctionConfig;
import chat.octet.model.Model;
import chat.octet.model.beans.ChatMessage;
import chat.octet.model.beans.CompletionResult;
import chat.octet.model.functions.*;
import chat.octet.model.functions.impl.DataTimeFunction;
import chat.octet.model.parameters.GenerateParameter;
import chat.octet.model.utils.JsonUtils;
import chat.octet.utils.CommonUtils;
import chat.octet.utils.QwenRAGFormatter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static chat.octet.model.functions.FunctionConstants.*;

@Slf4j
public class FunctionRegister {

    private final static Map<String, Function> FUNCTIONS_INSTANCE = Maps.newLinkedHashMap();
    private static volatile FunctionRegister register;

    static {
        FUNCTIONS_INSTANCE.put("datetime_query", new DataTimeFunction(
                FunctionDescriptor.builder()
                        .name("datetime_query")
                        .alias("时间查询")
                        .description("时间查询是一个查询当前系统时间的工具，可用于查询现在的时间，时间格式为 yyyy-MM-dd HH:mm:ss.SSS")
                        .build()));
    }


    private FunctionRegister() {
    }

    public static FunctionRegister getInstance() {
        if (register == null) {
            synchronized (FunctionRegister.class) {
                if (register == null) {
                    register = new FunctionRegister();
                }
            }
        }
        return register;
    }

    public List<Function> getFunctions() {
        List<FunctionConfig> configs = getFunctionConfigs();
        for (FunctionConfig config : configs) {
            if (!FUNCTIONS_INSTANCE.containsKey(config.getName())) {
                ApiFunction api = new ApiFunction(config);
                FUNCTIONS_INSTANCE.put(config.getName(), api);
                log.debug("Create API function: {}, config: {}", config.getName(), config);
            }
        }
        return Lists.newArrayList(FUNCTIONS_INSTANCE.values());
    }

    private List<FunctionConfig> getFunctionConfigs() {
        String filePath = StringUtils.join(CommonUtils.getUserConfigPath(), File.separator, "functions.json");
        String json = CommonUtils.readFile(filePath);
        return Optional.ofNullable(JsonUtils.parseJsonToList(json, FunctionConfig.class)).orElse(Lists.newLinkedList());
    }

    public FunctionOutput execute(List<Function> tools, List<FunctionCall> toolCalls) {
        for (FunctionCall call : toolCalls) {
            Function tool = tools.stream().filter(e -> e.getDesc().getName().equals(call.getFunction().get(FunctionConstants.FUNCTION_CALL_NAME))).findFirst().orElse(null);
            if (tool != null) {
                FunctionInput input = new FunctionInput().fromJson(call.getFunction().get(FunctionConstants.FUNCTION_CALL_ARGUMENTS).toString());
                return tool.execute(input);
            }
        }
        return new FunctionOutput();
    }

    public CompletionResult functionCall(Model model, GenerateParameter generateParams, List<ChatMessage> messages) {
        List<Function> functions = getFunctions();

        CompletionResult result;
        Map<String, Object> params = null;
        while (true) {
            CompletionResult res = model.chat(generateParams, messages, functions, params).result();
            log.debug("Chat completion result:\n{}", res.getContent());

            params = QwenRAGFormatter.parseResponse(res.getContent());
            ChatMessage assistantMsg = ChatMessage.toAssistant(Optional.ofNullable(params.get(FUNCTION_TEMPLATE_ARGS_ASSISTANT)).orElse("").toString());
            if (params.containsKey(FUNCTION_TEMPLATE_ARGS_NAME) && params.containsKey(FUNCTION_TEMPLATE_ARGS_INPUT)) {
                assistantMsg.setToolCalls(Lists.newArrayList(
                        FunctionCall.builder()
                                .addParameter(FUNCTION_CALL_NAME, params.get(FUNCTION_TEMPLATE_ARGS_NAME).toString())
                                .addParameter(FUNCTION_CALL_ARGUMENTS, params.get(FUNCTION_TEMPLATE_ARGS_INPUT).toString())
                                .build()
                ));
                log.debug("Received assistant function call response: {}", JsonUtils.toJson(assistantMsg.getToolCalls()));
            } else {
                result = CompletionResult.builder()
                        .content(assistantMsg.getContent())
                        .completionTokens(res.getCompletionTokens())
                        .promptTokens(res.getPromptTokens())
                        .finishReason(res.getFinishReason())
                        .build();
                break;
            }
            FunctionOutput output = execute(functions, assistantMsg.getToolCalls());
            ChatMessage functionCallback = ChatMessage.toFunction(JsonUtils.toJson(output));
            log.debug("Function call completed, output: {}", output);
            messages.add(assistantMsg);
            messages.add(functionCallback);
        }
        return result;
    }
}
