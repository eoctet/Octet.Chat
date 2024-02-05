package chat.octet.agent.plugin.impl;


import chat.octet.agent.plugin.PluginService;
import chat.octet.agent.plugin.config.ApiConfig;
import chat.octet.agent.plugin.enums.HttpMethod;
import chat.octet.agent.plugin.model.ExecuteResult;
import chat.octet.agent.plugin.model.Parameter;
import chat.octet.agent.plugin.model.PluginConfig;
import chat.octet.agent.plugin.model.QueryParameter;
import chat.octet.exceptions.PluginExecuteException;
import chat.octet.utils.CommonUtils;
import chat.octet.utils.JsonUtils;
import chat.octet.utils.XmlParser;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static chat.octet.agent.plugin.enums.DataFormatType.JSON;
import static chat.octet.agent.plugin.enums.DataFormatType.XML;

@Slf4j
public class ApiPlugin implements PluginService {

    private final PluginConfig pluginConfig;
    private final ApiConfig apiConfig;
    private final OkHttpClient client;

    public ApiPlugin(PluginConfig pluginConfig) {
        this.pluginConfig = pluginConfig;
        this.apiConfig = pluginConfig.getConfig(ApiConfig.class, "API config cannot be null.");

        Preconditions.checkArgument(StringUtils.isNotBlank(apiConfig.getUrl()), "Request url cannot be empty.");
        if (HttpMethod.GET != apiConfig.getMethod()) {
            Preconditions.checkArgument(StringUtils.isNotBlank(apiConfig.getBody()), "Request body cannot be empty.");
        }
        log.debug("Create API plugin, config: {}.", JsonUtils.toJson(apiConfig));
        Proxy proxyServer = null;
        if (StringUtils.isNotBlank(apiConfig.getProxyServerAddress()) && apiConfig.getProxyServerPort() != -1) {
            proxyServer = new Proxy(apiConfig.getProxyType(), new InetSocketAddress(apiConfig.getProxyServerAddress(), apiConfig.getProxyServerPort()));
            log.debug("Enable proxy service support, proxy server address: {}.", StringUtils.join(apiConfig.getProxyServerAddress(), ":", apiConfig.getProxyServerPort()));
        }
        this.client = new OkHttpClient().newBuilder()
                .proxy(proxyServer)
                .callTimeout(apiConfig.getTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(apiConfig.getTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(apiConfig.getTimeout(), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(apiConfig.isRetryOnConnectionFailure())
                .build();
    }

    private MediaType getMediaType() {
        String contentType = apiConfig.getHeaders().get("Content-Type");
        if (StringUtils.isBlank(contentType)) {
            //Set it according to the response data format
            if (apiConfig.getResponseDataFormat() == JSON) {
                contentType = "application/json";
            } else if (apiConfig.getResponseDataFormat() == XML) {
                contentType = "application/xml";
            } else {
                contentType = "";
                log.warn("Request header [Content-Type] is not specified.");
            }
        }
        return MediaType.parse(contentType);
    }

    private String exchange(QueryParameter queryParameter) throws IOException {
        //Set request headers
        Map<String, String> headersMaps = Maps.newHashMap();
        if (!apiConfig.getHeaders().isEmpty()) {
            apiConfig.getHeaders().forEach((key, value) -> headersMaps.put(key, StringSubstitutor.replace(value, queryParameter)));
        }
        Headers headers = Headers.of(headersMaps);
        //Set request params
        String url = StringSubstitutor.replace(apiConfig.getUrl(), queryParameter);
        HttpUrl.Builder urlBuilder = HttpUrl.get(url).newBuilder();
        if (!apiConfig.getRequest().isEmpty()) {
            apiConfig.getRequest().forEach((key, value) -> urlBuilder.addQueryParameter(key, StringSubstitutor.replace(value, queryParameter)));
        }
        HttpUrl httpUrl = urlBuilder.build();
        //Set request body
        RequestBody body = null;
        if (StringUtils.isNotBlank(apiConfig.getBody())) {
            MediaType mediaType = getMediaType();
            String bodyStr = StringSubstitutor.replace(apiConfig.getBody(), queryParameter);
            body = RequestBody.create(bodyStr, mediaType);
        }
        log.debug("Request url: {}, request headers: {}, request body: {}.", httpUrl.url(), headersMaps, body);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .headers(headers)
                .method(apiConfig.getMethod().name(), body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                return response.body().string();
            }
            return null;
        }
    }

    @Override
    public ExecuteResult execute(QueryParameter params) {
        ExecuteResult result = new ExecuteResult();

        try {
            String responseBody = exchange(params);
            if (StringUtils.isBlank(responseBody)) {
                log.warn("Response result is empty, please check if the API is normal.");
                return result;
            }
            LinkedHashMap<String, Object> responseMaps = null;
            switch (apiConfig.getResponseDataFormat()) {
                case JSON:
                    responseMaps = JsonUtils.parseJsonToMap(responseBody, String.class, Object.class);
                    break;
                case XML:
                    responseMaps = XmlParser.parseXmlToMap(responseBody);
                    break;
            }
            if (responseMaps == null) {
                throw new IllegalArgumentException("Parse response string to map failed, the response data is empty");
            }
            List<Parameter> outputParameter = pluginConfig.getOutputParameters();
            if (!CommonUtils.isEmpty(outputParameter)) {
                result.parse(outputParameter, responseMaps, apiConfig.getResponseResultLimit());
            }
        } catch (Exception e) {
            throw new PluginExecuteException(e.getMessage(), e);
        }
        return result;
    }

}
