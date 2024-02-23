package chat.octet.model.beans;

import chat.octet.model.enums.LlamaRoPEScalingType;
import lombok.ToString;

/**
 * Llama context params entity
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@ToString
public class LlamaContextParams {
    /**
     * RNG seed, -1 for random.
     */
    public int seed;
    /**
     * text context size.
     */
    public int ctx;
    /**
     * prompt processing batch size.
     */
    public int batch;
    /**
     * number of threads used for generation.
     */
    public int threads;
    /**
     * number of threads used for prompt and batch processing.
     */
    public int threadsBatch;
    /**
     * RoPE scaling type, from `enum llama_rope_scaling_type`.
     *
     * @see LlamaRoPEScalingType
     */
    public int ropeScalingType;
    /**
     * YaRN extrapolation mix factor, NaN = from model.
     */
    public float yarnExtFactor;
    /**
     * YaRN magnitude scaling factor.
     */
    public float yarnAttnFactor;
    /**
     * YaRN low correction dim.
     */
    public float yarnBetaFast;
    /**
     * YaRN high correction dim.
     */
    public float yarnBetaSlow;
    /**
     * YaRN original context size.
     */
    public int yarnOrigCtx;
    /**
     * RoPE base frequency.
     */
    public float ropeFreqBase;
    /**
     * RoPE frequency scaling factor.
     */
    public float ropeFreqScale;
    /**
     * data type for K cache.
     */
    public int dataTypeK;
    /**
     * data type for V cache.
     */
    public int dataTypeV;
    /**
     * if true, use experimental mul_mat_q kernels.
     */
    public boolean mulMatQ;
    /**
     * the llama_eval() call computes all logits, not just the last one.
     */
    public boolean logitsAll;
    /**
     * embedding mode only.
     */
    public boolean embedding;
    /**
     * whether to offload the KQV ops (including the KV cache) to GPU.
     */
    public boolean offloadKqv;
    /**
     * whether to pool (sum) embedding results by sequence id (ignored if no pooling layer).
     */
    public boolean doPooling;

}
