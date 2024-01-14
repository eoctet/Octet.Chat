package chat.octet.model.parameters;

import chat.octet.model.beans.LlamaContextParams;
import chat.octet.model.beans.LlamaModelParams;
import chat.octet.model.enums.LlamaRoPEScalingType;
import chat.octet.model.enums.ModelType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import javax.annotation.Nullable;

/**
 * Llama model parameters
 *
 * @author <a href="https://github.com/eoctet">William</a>
 * @see LlamaModelParams
 * @see LlamaContextParams
 */
@Getter
@Builder
@ToString
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ModelParameter {

    /**
     * Llama model path
     */
    private String modelPath;

    /**
     * Llama model name
     */
    private String modelName;

    /**
     * Llama model type (default model type: Llama2).
     */
    @Builder.Default
    private String modelType = ModelType.LLAMA2.name();

    /**
     * option allows you to set the size of the prompt context used by the LLaMA models during text generation.
     * (default: 512)
     */
    @Builder.Default
    private int contextSize = 512;

    /**
     * When using multiple GPUs this option controls which GPU is used for small tensors for which the overhead of
     * splitting the computation across all GPUs is not worthwhile.
     */
    @Nullable
    private Integer mainGpu;

    /**
     * Number of layers to offload to GPU (-ngl). If -1, all layers are offloaded.
     */
    @Builder.Default
    private int gpuLayers = 0;

    /**
     * Set the random number generator (RNG) seed (default: -1, -1 = random seed).
     */
    @Builder.Default
    private int seed = -1;

    /**
     * Return logits for all tokens, not just the last token.
     */
    @Builder.Default
    private boolean logitsAll = false;

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
     * Embedding mode only.
     */
    @Builder.Default
    private boolean embedding = false;

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
     * Set the batch size for prompt processing (default: 512).
     */
    @Builder.Default
    private int batchSize = 512;

    /**
     * Maximum number of tokens to keep in the last_n_tokens deque.
     */
    @Builder.Default
    private int lastTokensSize = 64;

    /**
     * Optional model to use as a base for the layers modified by the LoRA adapter.
     */
    @Nullable
    private String loraBase;

    /**
     * Apply a LoRA (Low-Rank Adaptation) adapter to the model (implies --no-mmap).
     */
    @Nullable
    private String loraPath;

    /**
     * apply LoRA adapter with user defined scaling S (implies --no-mmap).
     */
    private float loraScale;

    /**
     * When using multiple GPUs this option controls how large tensors should be split across all GPUs.
     */
    @Nullable
    private float[] tensorSplit;

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
     * Grouped-query attention. Must be 8 for llama-2 70b.
     */
    @Nullable
    private Integer gqa;

    /**
     * default is 1e-5, 5e-6 is a good value for llama-2 models.
     */
    @Nullable
    private Float rmsNormEps;

    /**
     * If true, use experimental mul_mat_q kernels.
     */
    @Builder.Default
    private boolean mulMatQ = true;

    /**
     * Print verbose output to stderr.
     */
    @Builder.Default
    private boolean verbose = false;

    /**
     * RoPE scaling type, from `enum llama_rope_scaling_type`.
     *
     * @see LlamaRoPEScalingType
     */
    @Builder.Default
    private int ropeScalingType = LlamaRoPEScalingType.LLAMA_ROPE_SCALING_UNSPECIFIED.getType();

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
     * whether to offload the KQV ops (including the KV cache) to GPU.
     */
    @Builder.Default
    private boolean offloadKqv = true;

    /**
     * how to split the model across multiple GPUs (default: 1).
     * <p></p>
     * LLAMA_SPLIT_NONE    = 0 (single GPU)
     * LLAMA_SPLIT_LAYER   = 1 (split layers and KV across GPUs)
     * LLAMA_SPLIT_ROW     = 2 (split rows across GPUs)
     */
    @Builder.Default
    private int splitMode = 1;

}
