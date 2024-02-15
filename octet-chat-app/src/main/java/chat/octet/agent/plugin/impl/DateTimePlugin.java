package chat.octet.agent.plugin.impl;


import chat.octet.agent.plugin.PluginService;
import chat.octet.agent.plugin.model.ExecuteResult;
import chat.octet.agent.plugin.model.PluginConfig;
import chat.octet.agent.plugin.model.QueryParameter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimePlugin implements PluginService {

    private final PluginConfig pluginConfig;
    private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public DateTimePlugin(PluginConfig pluginConfig) {
        this.pluginConfig = pluginConfig;
    }

    @Override
    public ExecuteResult execute(QueryParameter params) {
        ExecuteResult result = new ExecuteResult();
        result.put("DateTime", formatter.format(LocalDateTime.now()));
        return result;
    }

}
