package chat.octet.model.functions;


import chat.octet.model.utils.JsonUtils;

import java.util.LinkedHashMap;

public class FunctionOutput extends LinkedHashMap<String, Object> {

    @Override
    public String toString() {
        return JsonUtils.toJson(this);
    }
}
