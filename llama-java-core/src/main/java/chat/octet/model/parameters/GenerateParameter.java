package chat.octet.model.parameters;

import chat.octet.model.beans.LogitBias;
import chat.octet.model.components.criteria.StoppingCriteriaList;
import chat.octet.model.components.processor.LogitsProcessorList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;


/**
 * <p>Generate parameter</p>
 * For more information, please refer to
 * <a href="https://github.com/eoctet/llama-java/wiki/Llama-Java-parameters">Llama-Java-parameters</a>
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@Getter
@Builder
@ToString
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public final class GenerateParameter {

    //generate parameters

    /**
     * Adjust the randomness of the generated text (default: 0.8).
     */
    @Builder.Default
    private float temperature = 0.8f;

    /**
     * Control the repetition of token sequences in the generated text (default: 1.1).
     */
    @Builder.Default
    private float repeatPenalty = 1.1f;

    /**
     * Disable penalization for newline tokens when applying the repeat penalty
     */
    @Builder.Default
    private boolean penalizeNl = true;

    /**
     * Repeat alpha frequency penalty (default: 0.0, 0.0 = disabled)
     */
    @Builder.Default
    private float frequencyPenalty = 0.0f;

    /**
     * Repeat alpha presence penalty (default: 0.0, 0.0 = disabled)")
     */
    @Builder.Default
    private float presencePenalty = 0.0f;

    /**
     * <b>TOP-K Sampling</b><br/>
     * Limit the next token selection to the K most probable tokens (default: 40).
     */
    @Builder.Default
    private int topK = 40;

    /**
     * <b>TOP-P Sampling</b><br/>
     * Limit the next token selection to a subset of tokens with a cumulative probability above a threshold P
     * (default: 0.9).
     */
    @Builder.Default
    private float topP = 0.90f;

    /**
     * <b>Tail Free Sampling (TFS)</b><br/>
     * Enable tail free sampling with parameter z (default: 1.0, 1.0 = disabled).
     */
    @Builder.Default
    private float tsf = 1.0f;

    /**
     * <b>Typical Sampling</b><br/>
     * Enable typical sampling sampling with parameter p (default: 1.0, 1.0 = disabled).
     */
    @Builder.Default
    private float typical = 1.0f;

    /**
     * <b>Min P Sampling</b><br/>
     * Sets a minimum base probability threshold for token selection (default: 0.05).
     */
    @Builder.Default
    private float minP = 0.05f;

    /**
     * <b>Mirostat Sampling</b><br/>
     * Enable Mirostat sampling, controlling perplexity during text generation
     * (default: 0, 0 = disabled, 1 = Mirostat, 2 = Mirostat 2.0).
     */
    @Builder.Default
    private MirostatMode mirostatMode = MirostatMode.DISABLED;

    /**
     * <b>Mirostat Sampling</b><br/>
     * Set the Mirostat learning rate, parameter eta (default: 0.1).
     */
    @Builder.Default
    private float mirostatETA = 0.1f;

    /**
     * <b>Mirostat Sampling</b><br/>
     * Set the Mirostat target entropy, parameter tau (default: 5.0).
     */
    @Builder.Default
    private float mirostatTAU = 5.0f;

    /**
     * <b>Dynamic Temperature Sampling</b><br/>
     * Dynamic temperature range.
     * The final temperature will be in the range of (temperature - dynatemp_range) and (temperature + dynatemp_range)
     * (default: 0.0, 0.0 = disabled).
     */
    @Builder.Default
    private float dynatempRange = 0f;

    /**
     * <b>Dynamic Temperature Sampling</b><br/>
     * Dynamic temperature exponent (default: 1.0).
     */
    @Builder.Default
    private float dynatempExponent = 1.0f;

    /**
     * Specify a grammar (defined inline or in a file) to constrain model output to a specific format.
     * For example, you could force the model to output JSON or to speak only in emojis
     */
    private String grammarRules;

    /**
     * Maximum new token generation size  (default: 512).
     */
    @Builder.Default
    private int maxNewTokenSize = 512;

    /**
     * Print the prompt before generating text.
     */
    @Builder.Default
    private boolean verbosePrompt = false;

    /**
     * logits processor list.
     */
    @JsonIgnore
    @Builder.Default
    private LogitsProcessorList logitsProcessorList = new LogitsProcessorList();

    /**
     * stopping criteria list.
     */
    @JsonIgnore
    @Builder.Default
    private StoppingCriteriaList stoppingCriteriaList = new StoppingCriteriaList();

    /**
     * Maximum number of tokens to keep in the last_n_tokens deque.
     */
    @Builder.Default
    private int lastTokensSize = 64;

    /**
     * If true, special tokens are rendered in the output.
     */
    @Builder.Default
    private boolean special = false;

    /**
     * Adjust the probability distribution of words.
     */
    private LogitBias logitBias;

    /**
     * Control the stop word list for generating stops, with values that can be text or token IDs.
     */
    private String[] stoppingWord;

    //infill

    /**
     * Enable infill mode for the model.
     */
    @Builder.Default
    private boolean infill = false;

    /**
     * Use Suffix/Prefix/Middle pattern for infill (instead of Prefix/Suffix/Middle) as some models prefer this.
     */
    @Builder.Default
    private boolean spmFill = false;

    /**
     * Specify a prefix token in fill mode,
     * If not specified, read from the model by default.
     */
    private String prefixToken;

    /**
     * Specify a suffix token in fill mode,
     * If not specified, read from the model by default.
     */
    private String suffixToken;

    /**
     * Specify a middle token in fill mode,
     * If not specified, read from the model by default.
     */
    private String middleToken;

    //chat session

    /**
     * If enabled, each chat conversation will be stored in the session cache.
     * (Only supports chat mode)
     */
    @Builder.Default
    private boolean sessionCache = false;

    /**
     * Cache the system prompt in the session and does not update them again.
     * (Only supports chat mode)
     */
    @Builder.Default
    private boolean promptCache = false;

    /**
     * Specify user nickname, default: User.
     */
    @Builder.Default
    @Setter
    private String user = "User";

    /**
     * Specify bot nickname, default: Assistant.
     */
    @Builder.Default
    private String assistant = "Assistant";

    /**
     * User session id.
     */
    @Builder.Default
    @Setter
    @JsonIgnore
    private String session = "";

    /**
     * Mirostat sampling mode define
     */
    public enum MirostatMode {
        DISABLED, V1, V2
    }

}
