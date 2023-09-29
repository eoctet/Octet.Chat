package chat.octet.model.beans;

import lombok.ToString;

@ToString
public class LlamaContextParams {
    /**
     * RNG seed, -1 for random
     */
    public int seed;
    /**
     * text context
     */
    public int ctx;
    /**
     * prompt processing batch size
     */
    public int batch;
    /**
     * n_threads
     */
    public int threads;
    /**
     * n_threads_batch
     */
    public int threadsBatch;
    /**
     * RoPE base frequency
     */
    public float ropeFreqBase;
    /**
     * RoPE frequency scaling factor
     */
    public float ropeFreqScale;
    /**
     * if true, use experimental mul_mat_q kernels
     */
    public boolean mulMatQ;
    /**
     * use fp16 for KV cache
     */
    public boolean f16KV;
    /**
     * the llama_eval() call computes all logits, not just the last one
     */
    public boolean logitsAll;
    /**
     * embedding mode only
     */
    public boolean embedding;

}
