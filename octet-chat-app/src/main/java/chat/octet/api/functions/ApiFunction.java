package chat.octet.api.functions;


import chat.octet.api.functions.model.FunctionConfig;
import chat.octet.api.functions.model.Parameter;
import chat.octet.api.functions.model.RequestConfig;
import chat.octet.model.functions.*;
import chat.octet.model.utils.JsonUtils;
import chat.octet.utils.CommonUtils;
import chat.octet.utils.XmlParser;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static chat.octet.model.functions.FunctionConstants.FUNCTION_PARAMETER_SCHEMA;

@Slf4j
@Getter
public class ApiFunction implements Function {

    private final FunctionConfig config;
    private final OkHttpClient client;
    private final RequestConfig request;

    public ApiFunction(FunctionConfig config) {
        this.config = config;
        this.request = config.getConfig();

        Preconditions.checkArgument(StringUtils.isNotBlank(request.getUrl()), "Request url cannot be empty.");
        if (!HttpMethod.GET.name().equalsIgnoreCase(request.getMethod())) {
            Preconditions.checkArgument(StringUtils.isNotBlank(request.getBody()), "Request body cannot be empty.");
        }

        Proxy proxyServer = null;
        if (StringUtils.isNotBlank(request.getProxyServerAddress()) && request.getProxyServerPort() != -1) {
            proxyServer = new Proxy(request.getProxyType(), new InetSocketAddress(request.getProxyServerAddress(), request.getProxyServerPort()));
            log.debug("Enable proxy service support, proxy server address: {}.", StringUtils.join(request.getProxyServerAddress(), ":", request.getProxyServerPort()));
        }
        this.client = new OkHttpClient().newBuilder()
                .proxy(proxyServer)
                .callTimeout(request.getTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(request.getTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(request.getTimeout(), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(request.isRetryOnConnectionFailure())
                .build();
    }

    private MediaType getMediaType() {
        String contentType = request.getHeaders().get("Content-Type");
        if (StringUtils.isBlank(contentType)) {
            //Set it according to the response data format
            if ("json".equalsIgnoreCase(request.getResponseDataFormat())) {
                contentType = "application/json";
            } else if ("xml".equalsIgnoreCase(request.getResponseDataFormat())) {
                contentType = "application/xml";
            } else {
                contentType = "";
                log.warn("Request header [Content-Type] is not specified.");
            }
        }
        return MediaType.parse(contentType);
    }

    private String exchange(FunctionInput params) throws IOException {
        //Set request headers
        Map<String, String> headersMaps = Maps.newHashMap();
        if (!request.getHeaders().isEmpty()) {
            request.getHeaders().forEach((key, value) -> headersMaps.put(key, StringSubstitutor.replace(value, params)));
        }
        Headers headers = Headers.of(headersMaps);
        //Set request params
        String url = StringSubstitutor.replace(request.getUrl(), params);
        HttpUrl.Builder urlBuilder = HttpUrl.get(url).newBuilder();
        if (!request.getRequest().isEmpty()) {
            request.getRequest().forEach((key, value) -> urlBuilder.addQueryParameter(key, StringSubstitutor.replace(value, params)));
        }
        HttpUrl httpUrl = urlBuilder.build();
        //Set request body
        RequestBody body = null;
        if (StringUtils.isNotBlank(request.getBody())) {
            MediaType mediaType = getMediaType();
            String bodyStr = StringSubstitutor.replace(request.getBody(), params);
            body = RequestBody.create(bodyStr, mediaType);
        }
        log.debug("Request url: {}, request headers: {}, request body: {}.", httpUrl.url(), headersMaps, body);
        Request req = new Request.Builder()
                .url(urlBuilder.build())
                .headers(headers)
                .method(request.getMethod(), body)
                .build();
        try (Response response = client.newCall(req).execute()) {
            if (response.body() != null) {
                return response.body().string();
            }
            return null;
        }
    }

    @Override
    public FunctionOutput execute(FunctionInput params) {
        FunctionOutput output = new FunctionOutput();
        try {
            String responseBody = exchange(params);
            if (StringUtils.isBlank(responseBody)) {
                log.warn("Response result is empty, please check if the API is normal.");
                return output;
            }
            LinkedHashMap<String, Object> responseMaps = null;
            switch (request.getResponseDataFormat().toLowerCase()) {
                case "json":
                    responseMaps = JsonUtils.parseJsonToMap(responseBody, String.class, Object.class);
                    break;
                case "xml":
                    responseMaps = XmlParser.parseXmlToMap(responseBody);
                    break;
            }
            if (responseMaps == null) {
                throw new IllegalArgumentException("Parse response string to map failed, the response data is empty");
            }
            List<Parameter> outputParameter = config.getOutputParameters();
            if (!CommonUtils.isEmpty(outputParameter)) {
                output.putAll(CommonUtils.parse(outputParameter, responseMaps, request.getResponseResultLimit()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return output;
    }

    @Override
    public FunctionDescriptor getDesc() {
        List<FunctionParameter> parameters = Lists.newArrayList();
        for (Parameter inputParameter : config.getInputParameters()) {
            parameters.add(FunctionParameter.builder()
                    .name(inputParameter.getName())
                    .description(inputParameter.getDescription())
                    .required(inputParameter.isRequired())
                    .addSchema(FUNCTION_PARAMETER_SCHEMA, inputParameter.getType())
                    .build()
            );
        }
        return FunctionDescriptor.builder()
                .name(config.getName())
                .alias(config.getAlias())
                .description(config.getDescription())
                .parameters(parameters)
                .build();
    }
}
