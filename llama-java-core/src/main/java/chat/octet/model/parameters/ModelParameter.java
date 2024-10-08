package chat.octet.model.parameters;

import chat.octet.model.beans.LlamaContextParams;
import chat.octet.model.beans.LlamaModelParams;
import chat.octet.model.components.prompt.ChatTemplateFormatter;
import chat.octet.model.enums.*;
import chat.octet.model.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

/**
 * <p>Llama model parameters</p>
 * For more information, please refer to
 * <a href="https://github.com/eoctet/llama-java/wiki/Llama-Java-parameters">Llama-Java-parameters</a>
 *
 * @author <a href="https://github.com/eoctet">William</a>
 * @see LlamaModelParams
 * @see LlamaContextParams
 */
@Getter
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ModelParameter {

    //Basic parameters

    /**
     * Llama model path
     */
    @Setter
    private String modelPath;

    /**
     * Optional model to use as a base for the layers modified by the LoRA adapter.
     */
    @Setter
    private String loraBase;

    /**
     * Apply a LoRA (Low-Rank Adaptation) adapter to the model (implies --no-mmap).
     */
    @Setter
    private String loraPath;

    /**
     * apply LoRA adapter with user defined scaling S (implies --no-mmap).
     */
    private float loraScale;

    /**
     * Print verbose output to stderr.
     */
    @Builder.Default
    private boolean verbose = false;

    /**
     * Specify a chat formatter to use for the chat interface.
     */
    private ChatTemplateFormatter chatTemplateFormatter;

    /**
     * Attempt one of the below optimization strategies that may help on some NUMA systems (default: disabled).
     *
     * @see LlamaNumaStrategy
     */
    @Builder.Default
    private int numaStrategy = LlamaNumaStrategy.NUMA_STRATEGY_DISABLED.getType();

    //Context parameters

    /**
     * Set the random number generator (RNG) seed (default: -1, -1 = random seed).
     */
    @Builder.Default
    private int seed = -1;

    /**
     * option allows you to set the size of the prompt context used by the LLaMA models during text generation.
     * (default: 512)
     */
    @Builder.Default
    private int contextSize = 512;

    /**
     * Set the batch size for prompt processing (default: 2048).
     */
    @Builder.Default
    private int batchSize = 2048;

    /**
     * Physical maximum batch size (default: 512).
     */
    @Builder.Default
    private int ubatch = 512;

    /**
     * Max number of sequences (default: 1).
     */
    @Builder.Default
    private int seqMax = 1;

    /**
     * Set the number of threads used for generation (single token).
     */
    @Builder.Default
    private int threads = 4;

    /**
     * Set the number of threads used for prompt and batch processing (multiple tokens).
     */
    @Builder.Default
    private int threadsBatch = 4;

    /**
     * RoPE scaling type.
     *
     * @see LlamaRoPEScalingType
     */
    @Builder.Default
    private int ropeScalingType = LlamaRoPEScalingType.LLAMA_ROPE_SCALING_UNSPECIFIED.getType();

    /**
     * Pooling type for embeddings, use model default if unspecified. Options are none(0), mean(1), cls(2).
     *
     * @see LlamaPoolingType
     */
    @Builder.Default
    private int poolingType = LlamaPoolingType.LLAMA_POOLING_TYPE_UNSPECIFIED.getType();

    /**
     * Attention type for embeddings, use model default if unspecified.
     *
     * @see LlamaAttentionType
     */
    @Builder.Default
    private int attentionType = LlamaAttentionType.LLAMA_ATTENTION_TYPE_UNSPECIFIED.getType();

    /**
     * Base frequency for RoPE sampling.
     */
    @Builder.Default
    private float ropeFreqBase = 0;

    /**
     * Scale factor for RoPE sampling.
     */
    @Builder.Default
    private float ropeFreqScale = 0;

    /**
     * YaRN extrapolation mix factor, NaN = from model.
     */
    @Builder.Default
    private float yarnExtFactor = -1.0f;

    /**
     * YaRN magnitude scaling factor.
     */
    @Builder.Default
    private float yarnAttnFactor = 1.0f;

    /**
     * YaRN low correction dim.
     */
    @Builder.Default
    private float yarnBetaFast = 32.0f;

    /**
     * YaRN high correction dim.
     */
    @Builder.Default
    private float yarnBetaSlow = 1.0f;

    /**
     * YaRN original context size.
     */
    private int yarnOrigCtx;

    /**
     * KV cache defragmentation threshold (default: -1.0, < 0 = disabled).
     */
    @Builder.Default
    private float defragThold = -1.0f;

    /**
     * Return logits for all tokens, not just the last token.
     */
    @Builder.Default
    private boolean logitsAll = false;

    /**
     * Embedding mode only.
     */
    @Builder.Default
    private boolean embedding = false;

    /**
     * whether to offload the KQV ops (including the KV cache) to GPU.
     */
    @Builder.Default
    private boolean offloadKqv = true;

    /**
     * Enable flash attention (default: disabled).
     */
    @Builder.Default
    private boolean flashAttn = false;

    //Model parameters

    /**
     * Number of layers to offload to GPU (-ngl). If -1, all layers are offloaded.
     */
    @Builder.Default
    private int gpuLayers = 0;

    /**
     * how to split the model across multiple GPUs (default: 1).
     *
     * @see LlamaSplitMode
     */
    @Builder.Default
    private int splitMode = LlamaSplitMode.LLAMA_SPLIT_MODE_LAYER.getType();

    /**
     * When using multiple GPUs this option controls which GPU is used for small tensors for which the overhead of
     * splitting the computation across all GPUs is not worthwhile.
     */
    @Builder.Default
    private int mainGpu = 0;

    /**
     * When using multiple GPUs this option controls how large tensors should be split across all GPUs.
     */
    private float[] tensorSplit;

    /**
     * Only load the vocabulary no weights.
     */
    @Builder.Default
    private boolean vocabOnly = false;

    /**
     * use mmap if possible (slower load but may reduce pageouts if not using mlock).
     */
    @Builder.Default
    private boolean mmap = true;

    /**
     * Lock the model in memory, preventing it from being swapped out when memory-mapped.
     */
    @Builder.Default
    private boolean mlock = false;

    /**
     * Validate model tensor data (default: disabled).
     */
    @Builder.Default
    private boolean checkTensors = false;


    @Override
    public String toString() {
        return JsonUtils.toJson(this);
    }
}
