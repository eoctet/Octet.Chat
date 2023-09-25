package chat.octet.model.parameters;

import chat.octet.model.beans.LlamaContextParams;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import javax.annotation.Nullable;

/**
 * Llama model parameters
 *
 * @author william
 * @version 1.0
 * @see LlamaContextParams
 * <a href="https://github.com/ggerganov/llama.cpp/blob/master/examples/main/README.md">Llama cpp docs</a>.
 */
@Getter
@Builder
@ToString
@Jacksonized
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
     * <b>context-size</b><br/>
     * option allows you to set the size of the prompt context used by the LLaMA models during text generation.
     * (default: 512)
     */
    @Builder.Default
    private int contextSize = 512;

    /**
     * <b>main-gpu</b><br/>
     * When using multiple GPUs this option controls which GPU is used for small tensors for which the overhead of
     * splitting the computation across all GPUs is not worthwhile.
     */
    @Nullable
    private Integer mainGpu;

    /**
     * <b>n-gpu-layers</b><br/>
     * Number of layers to offload to GPU (-ngl). If -1, all layers are offloaded.
     */
    @Builder.Default
    private int gpuLayers = 0;

    /**
     * <b>seed</b><br/>
     * Set the random number generator (RNG) seed (default: -1, -1 = random seed).
     */
    @Builder.Default
    private int seed = -1;

    /**
     * <b>f16-kv</b><br/>
     * Use half-precision for key/value cache.
     */
    @Builder.Default
    private boolean f16KV = true;

    /**
     * <b>logits-all</b><br/>
     * Return logits for all tokens, not just the last token.
     */
    @Builder.Default
    private boolean logitsAll = false;

    /**
     * <b>vocab-only</b><br/>
     * Only load the vocabulary no weights.
     */
    @Builder.Default
    private boolean vocabOnly = false;

    /**
     * <b>mmap</b><br/>
     * use mmap if possible (slower load but may reduce pageouts if not using mlock).
     */
    @Builder.Default
    private boolean mmap = true;

    /**
     * <b>mlock</b><br/>
     * Lock the model in memory, preventing it from being swapped out when memory-mapped.
     */
    @Builder.Default
    private boolean mlock = false;

    /**
     * <b>embedding</b><br/>
     * Embedding mode only.
     */
    @Builder.Default
    private boolean embedding = false;

    /**
     * <b>threads</b><br/>
     * Set the number of threads to use during computation.
     */
    @Builder.Default
    private int threads = 8;

    /**
     * <b>batch-size</b><br/>
     * Set the batch size for prompt processing (default: 512).
     */
    @Builder.Default
    private int batchSize = 512;

    /**
     * <b>last-n-tokens-size</b><br/>
     * Maximum number of tokens to keep in the last_n_tokens deque.
     */
    @Builder.Default
    private int lastNTokensSize = 64;

    /**
     * <b>lora-base</b><br/>
     * Optional model to use as a base for the layers modified by the LoRA adapter.
     */
    @Nullable
    private String loraBase;

    /**
     * <b>lora</b><br/>
     * Apply a LoRA (Low-Rank Adaptation) adapter to the model (implies --no-mmap).
     */
    @Nullable
    private String loraPath;

    /**
     * <b>low-vram</b><br/>
     * Do not allocate a VRAM scratch buffer for holding temporary results.
     * Reduces VRAM usage at the cost of performance, particularly prompt processing speed. Requires cuBLAS.
     */
    @Builder.Default
    private boolean lowVram = false;

    /**
     * <b>tensor-split</b><br/>
     * When using multiple GPUs this option controls how large tensors should be split across all GPUs.
     */
    @Nullable
    private float[] tensorSplit;

    /**
     * <b>rope-freq-base</b><br/>
     * Base frequency for RoPE sampling.
     */
    @Builder.Default
    private float ropeFreqBase = 10000.0f;

    /**
     * <b>rope-freq-scale</b><br/>
     * Scale factor for RoPE sampling.
     */
    @Builder.Default
    private float ropeFreqScale = 1.0f;

    /**
     * <b>n-gqa</b><br/>
     * Grouped-query attention. Must be 8 for llama-2 70b.
     */
    @Nullable
    private Integer gqa;

    /**
     * <b>rms-norm-eps</b><br/>
     * default is 1e-5, 5e-6 is a good value for llama-2 models.
     */
    @Nullable
    private Float rmsNormEps;

    /**
     * <b>mul-mat-q</b><br/>
     * If true, use experimental mul_mat_q kernels.
     */
    @Nullable
    private Boolean mulMatQ;

    /**
     * <b>verbose</b><br/>
     * Print verbose output to stderr
     */
    @Builder.Default
    private boolean verbose = false;

}
