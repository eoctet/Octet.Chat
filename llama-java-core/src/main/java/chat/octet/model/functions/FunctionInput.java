package chat.octet.model.functions;


import chat.octet.model.utils.JsonUtils;

import java.util.LinkedHashMap;

public class FunctionInput extends LinkedHashMap<String, Object> {

    public FunctionInput fromJson(String json) {
        this.putAll(JsonUtils.parseJsonToMap(json, String.class, Object.class));
        return this;
    }

    @Override
    public String toString() {
        return JsonUtils.toJson(this);
    }
}
