package chat.octet.agent.plugin;


import chat.octet.agent.plugin.enums.PluginType;
import chat.octet.agent.plugin.impl.ApiPlugin;
import chat.octet.agent.plugin.impl.DateTimePlugin;
import chat.octet.agent.plugin.model.ExecuteResult;
import chat.octet.agent.plugin.model.PluginConfig;
import chat.octet.agent.plugin.model.QueryParameter;
import chat.octet.exceptions.ServerException;
import chat.octet.utils.JsonUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class PluginManager {

    private static volatile PluginManager register;
    private final static Map<String, String> PLUGIN_MAPPING = Maps.newLinkedHashMap();
    private final static Map<String, PluginService> PLUGIN_INSTANCE = Maps.newLinkedHashMap();
    private List<PluginConfig> pluginConfigs;

    static {
        PLUGIN_MAPPING.put(PluginType.DATETIME.name(), DateTimePlugin.class.getName());
        PLUGIN_MAPPING.put(PluginType.API.name(), ApiPlugin.class.getName());
    }

    private PluginManager() {
    }

    public static PluginManager getInstance() {
        if (register == null) {
            synchronized (PluginManager.class) {
                if (register == null) {
                    register = new PluginManager();
                }
            }
        }
        return register;
    }

    public void loadPlugins() {
        if (pluginConfigs == null) {
            synchronized (PluginManager.class) {
                pluginConfigs = loadPluginConfig();
                pluginConfigs.forEach(plugin -> {
                    Preconditions.checkArgument(StringUtils.isNotBlank(plugin.getPluginType()), "Plugin type cannot be empty.");
                    Preconditions.checkArgument(StringUtils.isNotBlank(plugin.getNameForHuman()), "Name of human cannot be empty.");
                    Preconditions.checkArgument(StringUtils.isNotBlank(plugin.getNameForModel()), "Name of model cannot be empty.");
                    Preconditions.checkArgument(StringUtils.isNotBlank(plugin.getDescriptionForModel()), "Plugin description cannot be empty.");

                    String className = PLUGIN_MAPPING.get(plugin.getPluginType().toUpperCase());
                    if (className == null) {
                        throw new ServerException("Plugin type " + plugin.getPluginType() + " is not supported.");
                    }
                    try {
                        Class<?> clazz = Class.forName(className);
                        PLUGIN_INSTANCE.put(plugin.getNameForModel().toUpperCase(), (PluginService) clazz.getConstructor(PluginConfig.class).newInstance(plugin));
                    } catch (Exception e) {
                        throw new ServerException(e.getMessage(), e);
                    }
                });
            }
        }
    }

    private List<PluginConfig> loadPluginConfig() {
        String filePath = StringUtils.join(Paths.get("").toAbsolutePath().toString(), File.separator, "characters", File.separator, "plugins.json");
        File file = new File(filePath);
        if (!file.isFile() || !file.exists()) {
            throw new ServerException("Can not read plugin configuration file, please make sure it is valid");
        }
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            String json = bufferedReader.lines().collect(Collectors.joining());
            return JsonUtils.parseJsonToList(json, PluginConfig.class);
        } catch (Exception e) {
            throw new ServerException("Parse characters configuration file error", e);
        }
    }

    public String getAllPluginDescriptions() {
        Preconditions.checkArgument(pluginConfigs != null, "Please load and initialize all plugins first.");

        StringBuilder desc = new StringBuilder();
        pluginConfigs.forEach(pluginConfig -> {
            Map<String, Object> params = Maps.newLinkedHashMap();
            params.put("name_for_model", pluginConfig.getNameForModel());
            params.put("name_for_human", pluginConfig.getNameForHuman());
            params.put("description_for_model", pluginConfig.getDescriptionForModel());
            params.put("parameters", JsonUtils.toJson(Optional.ofNullable(pluginConfig.getInputParameters()).orElse(Lists.newArrayList())));
            String prompt = StringSubstitutor.replace(FormatUtils.PLUGINS_DESC_TEMPLATE, params);
            desc.append(prompt).append("\n\n");
        });
        return desc.toString();
    }

    public String getAllPluginNames() {
        Preconditions.checkArgument(pluginConfigs != null, "Please load and initialize all plugins first.");

        StringBuilder names = new StringBuilder();
        pluginConfigs.forEach(pluginConfig -> names.append(pluginConfig.getNameForModel()).append(","));
        if (names.length() > 0) {
            names.deleteCharAt(names.length() - 1);
        }
        return names.toString();
    }

    public String execute(String pluginName, String parameters, String question) {
        String name = pluginName.toUpperCase();
        if (!PLUGIN_INSTANCE.containsKey(name)) {
            return "No tools found.";
        }
        String result = String.format("%s has no results, please answer according to the understanding of question '%s'", pluginName, question.trim());
        PluginService plugin = PLUGIN_INSTANCE.get(name);
        try {
            QueryParameter queryParams = new QueryParameter();
            queryParams.putAll(Objects.requireNonNull(JsonUtils.parseJsonToMap(parameters, String.class, Object.class)));

            ExecuteResult executeResult = plugin.execute(queryParams);
            if (!executeResult.isEmpty()) {
                result = JsonUtils.toJson(executeResult);
            }
        } catch (Exception e) {
            log.error("Plugin execution error ", e);
        }
        return result;
    }

}
