package chat.octet.agent.plugin.model;


import chat.octet.utils.DataTypeConvert;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ExecuteResult extends ConcurrentHashMap<String, Object> {

    @SuppressWarnings("unchecked")
    public void findAndAddParameters(List<Parameter> outputParameter, Map<String, Object> result) {
        result.forEach((key, value) -> {
            outputParameter.forEach(parameter -> {
                if (parameter.getName().equalsIgnoreCase(key)) {
                    this.put(parameter.getName(), DataTypeConvert.getValue(parameter.getType(), value));
                }
            });
            if (value instanceof Map) {
                findAndAddParameters(outputParameter, (Map<String, Object>) value);
            }
            if (value instanceof List) {
                ((List<?>) value).forEach(e -> {
                    if (e instanceof Map) {
                        findAndAddParameters(outputParameter, (Map<String, Object>) e);
                    }
                });
            }
        });
    }
}
