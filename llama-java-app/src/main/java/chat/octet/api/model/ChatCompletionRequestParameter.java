package chat.octet.api.model;

import chat.octet.model.beans.ChatMessage;
import chat.octet.model.parameters.GenerateParameter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionRequestParameter {

    //chat completion parameters
    @JsonProperty("user")
    private String user;

    @JsonProperty("messages")
    private List<ChatMessage> messages;

    //completion parameters
    @JsonProperty("prompt")
    private String prompt;

    //common completion parameters
    @JsonProperty("temperature")
    private Float temperature;

    @JsonProperty("top_k")
    private Integer topK;

    @JsonProperty("top_p")
    private Float topP;

    @JsonProperty("min_p")
    private Float minP;

    @JsonProperty("tfs_z")
    private Float tfs;

    @JsonProperty("typical_p")
    private Float typical;

    @JsonProperty("stream")
    private boolean stream;

    @JsonProperty("stop")
    private List<String> stopWords;

    @JsonProperty("max_tokens")
    private Integer maxNewTokensSize;

    @JsonProperty("frequency_penalty")
    private Float frequencyPenalty;

    @JsonProperty("presence_penalty")
    private Float presencePenalty;

    @JsonProperty("repeat_penalty")
    private Float repeatPenalty;

    @JsonProperty("mirostat_mode")
    private GenerateParameter.MirostatMode mirostatMode;

    @JsonProperty("mirostat_eta")
    private Float mirostatETA;

    @JsonProperty("mirostat_tau")
    private Float mirostatTAU;

    @JsonProperty("logprobs")
    private Integer logprobs;

    @JsonProperty("logit_bias")
    private Map<Integer, String> logitBias;

    @JsonProperty("verbose")
    private boolean verbose;

    @JsonProperty("timeout")
    private Long timeout;

}
