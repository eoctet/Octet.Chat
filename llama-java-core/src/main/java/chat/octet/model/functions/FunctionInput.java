package chat.octet.model.functions;


import chat.octet.model.utils.JsonUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class FunctionInput extends LinkedHashMap<String, Object> {

    public FunctionInput fromJson(String json) {
        Map<String, Object> map = JsonUtils.parseJsonToMap(json, String.class, Object.class);
        if (map != null && !map.isEmpty()) {
            this.putAll(map);
        }
        return this;
    }

    @Override
    public String toString() {
        return JsonUtils.toJson(this);
    }
}
