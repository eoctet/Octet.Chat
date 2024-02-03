package chat.octet.agent.plugin;


import chat.octet.agent.plugin.model.ExecuteResult;
import chat.octet.agent.plugin.model.QueryParameter;
import chat.octet.exceptions.PluginExecuteException;

public interface PluginService {

    ExecuteResult execute(QueryParameter params) throws PluginExecuteException;

}
