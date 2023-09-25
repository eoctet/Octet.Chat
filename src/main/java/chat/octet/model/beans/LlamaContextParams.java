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
     * number of layers to store in VRAM
     */
    public int gpuLayers;
    /**
     * the GPU that is used for scratch and small tensors
     */
    public int mainGpu;
    /**
     * how to split layers across multiple GPUs (size: LLAMA_MAX_DEVICES)
     */
    public float[] tensorSplit;
    /**
     * RoPE base frequency
     */
    public float ropeFreqBase;
    /**
     * RoPE frequency scaling factor
     */
    public float ropeFreqScale;
    /**
     * if true, reduce VRAM usage at the cost of performance
     */
    public boolean lowVram;
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
     * only load the vocabulary, no weights
     */
    public boolean vocabOnly;
    /**
     * use mmap if possible
     */
    public boolean mmap;
    /**
     * force system to keep model in RAM
     */
    public boolean mlock;
    /**
     * embedding mode only
     */
    public boolean embedding;

}
