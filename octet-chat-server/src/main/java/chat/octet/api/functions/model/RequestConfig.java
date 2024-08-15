package chat.octet.api.functions.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.common.collect.Maps;
import lombok.Data;

import java.net.Proxy;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RequestConfig {
    private String url;
    private String method;
    private Map<String, String> headers = Maps.newLinkedHashMap();
    private Map<String, String> request = Maps.newLinkedHashMap();
    private String body;
    private Long timeout = 1000L * 5;
    private String responseDataFormat;
    private boolean retryOnConnectionFailure = true;
    private Proxy.Type proxyType;
    private String proxyServerAddress;
    private int proxyServerPort = -1;
    private int responseResultLimit = -1;
}
