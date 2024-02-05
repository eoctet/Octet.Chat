package chat.octet.agent.plugin.config;

import chat.octet.agent.plugin.enums.DataFormatType;
import chat.octet.agent.plugin.enums.HttpMethod;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.common.collect.Maps;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import javax.annotation.Nullable;
import java.net.Proxy;
import java.util.Map;


@Getter
@Builder
@ToString
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ApiConfig {
    /**
     * Request url.
     */
    private String url;
    /**
     * Request method.
     *
     * @see HttpMethod
     */
    @Builder.Default
    private HttpMethod method = HttpMethod.GET;
    /**
     * Request headers.
     */
    @Builder.Default
    private Map<String, String> headers = Maps.newLinkedHashMap();
    /**
     * Request query parameters.
     */
    @Builder.Default
    private Map<String, String> request = Maps.newLinkedHashMap();
    /**
     * Request body, supports JSON and XML data format.
     */
    @Nullable
    private String body;
    /**
     * Request timeout period (ms), default value: 5000 ms.
     */
    @Builder.Default
    private Long timeout = 1000L * 5;
    /**
     * Response data format, supports JSON and XML data format.
     * default value: JSON.
     *
     * @see DataFormatType
     */
    @Builder.Default
    private DataFormatType responseDataFormat = DataFormatType.JSON;
    /**
     * Whether to retry on connection failure.
     */
    @Builder.Default
    private boolean retryOnConnectionFailure = true;
    /**
     * Proxy type: DIRECT/HTTP/SOCKS.
     */
    @Nullable
    private Proxy.Type proxyType;
    /**
     * Proxy server address.
     */
    @Nullable
    private String proxyServerAddress;
    /**
     * Proxy server port.
     */
    @Builder.Default
    private int proxyServerPort = -1;

    /**
     * Number of response result list limit,
     * If the response result list exceeds the limit, it will be truncated.
     * Default unlimited (-1).
     */
    @Builder.Default
    private int responseResultLimit = -1;
}
