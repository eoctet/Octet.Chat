package chat.octet.agent.plugin.model;


import chat.octet.agent.plugin.enums.DataType;
import chat.octet.utils.CommonUtils;
import chat.octet.utils.DataTypeConvert;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class ExecuteResult extends ConcurrentHashMap<String, Object> implements Serializable {

    public void parse(List<Parameter> outputParameter, Map<String, Object> result, int limit) {
        Map<String, Object> data = deepCopySelectedFields(outputParameter, result, limit);
        this.putAll(data);
    }

    private Map<String, Object> deepCopySelectedFields(List<Parameter> params, Map<String, Object> source, int limit) {
        return source.entrySet().stream()
                .filter(entry -> isFieldToCopy(entry.getKey(), params))
                .collect(Collectors.toMap(
                        entry -> getParamDesc(entry.getKey(), "", params),
                        entry -> copyValue(entry.getValue(), entry.getKey(), params, limit)
                )).entrySet().stream()
                .filter(entry -> !CommonUtils.isEmpty(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean isFieldToCopy(String key, List<Parameter> params) {
        return params.stream().anyMatch(p -> (p.getName().equals(key) || p.getName().startsWith(key + ".")));
    }

    private String getParamDataType(String key, List<Parameter> params) {
        Optional<Parameter> op = params.stream().filter(p -> p.getName().equals(key)).findFirst();
        return op.isPresent() ? op.get().getType() : DataType.STRING.name();
    }

    private String getParamDesc(String key, String parentKey, List<Parameter> params) {
        String k = !parentKey.isEmpty() ? parentKey + "." + key : key;
        Optional<Parameter> op = params.stream().filter(p -> StringUtils.isNotBlank(p.getDescription()) && p.getName().equals(k)).findFirst();
        return op.isPresent() ? op.get().getDescription() : key;
    }

    @SuppressWarnings("unchecked")
    private Object copyValue(Object value, String parentKey, List<Parameter> params, int limit) {
        if (value instanceof Map) {
            return ((Map<String, Object>) value).entrySet().stream()
                    .filter(entry -> isFieldToCopy(parentKey + "." + entry.getKey(), params))
                    .collect(Collectors.toMap(
                            entry -> getParamDesc(entry.getKey(), parentKey, params),
                            entry -> copyValue(entry.getValue(), parentKey + "." + entry.getKey(), params, limit)
                    ));
        } else if (value instanceof List) {
            List<Object> copiedList = ((List<?>) value).stream()
                    .filter(Objects::nonNull)
                    .map(element -> copyValue(element, parentKey, params, limit))
                    .collect(Collectors.toList());
            if (!copiedList.isEmpty() && limit > 0 && limit < copiedList.size()) {
                return copiedList.subList(0, limit);
            }
            return copiedList;
        } else {
            return DataTypeConvert.getValue(getParamDataType(parentKey, params), value);
        }
    }

}
