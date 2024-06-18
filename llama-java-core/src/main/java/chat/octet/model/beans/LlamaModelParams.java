package chat.octet.model.beans;

import lombok.ToString;

/**
 * Llama model params entity
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@ToString
public class LlamaModelParams {
    /**
     * number of layers to store in VRAM.
     */
    public int gpuLayers;
    /**
     * how to split the model across multiple GPUs.
     * <p></p>
     * LLAMA_SPLIT_NONE    = 0 (single GPU)
     * LLAMA_SPLIT_LAYER   = 1 (split layers and KV across GPUs)
     * LLAMA_SPLIT_ROW     = 2 (split rows across GPUs)
     */
    public int splitMode;
    /**
     * the GPU that is used for scratch and small tensors.
     */
    public int mainGpu;
    /**
     * how to split layers across multiple GPUs (size: LLAMA_MAX_DEVICES).
     */
    public float[] tensorSplit;
    /**
     * only load the vocabulary, no weights.
     */
    public boolean vocabOnly;
    /**
     * use mmap if possible.
     */
    public boolean mmap;
    /**
     * force system to keep model in RAM.
     */
    public boolean mlock;
    /**
     * validate model tensor data.
     */
    public boolean checkTensors;
    /**
     * Attempt one of the below optimization strategies that may help on some NUMA systems.
     */
    public int numaStrategy;

}
