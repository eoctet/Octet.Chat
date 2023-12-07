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


}
