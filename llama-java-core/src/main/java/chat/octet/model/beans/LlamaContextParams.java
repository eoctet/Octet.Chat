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
     * if true, use experimental mul_mat_q kernels.
     */
    public boolean mulMatQ;
    /**
     * use fp16 for KV cache.
     */
    public boolean f16KV;
    /**
     * the llama_eval() call computes all logits, not just the last one.
     */
    public boolean logitsAll;
    /**
     * embedding mode only.
     */
    public boolean embedding;

}
