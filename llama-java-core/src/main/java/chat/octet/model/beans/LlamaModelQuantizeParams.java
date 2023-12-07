package chat.octet.model.beans;

import lombok.ToString;

/**
 * Llama model quantize params entity
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@ToString
public class LlamaModelQuantizeParams {
    /**
     * number of threads to use for quantizing, if <=0 will use std::thread::hardware_concurrency()
     */
    public int thread;
    /**
     * quantize to this llama_ftype
     */
    public int modelFileType;
    /**
     * allow quantizing non-f32/f16 tensors
     */
    public boolean allowRequantize;
    /**
     * quantize output.weight
     */
    public boolean quantizeOutputTensor;
    /**
     * only copy tensors - ftype, allow_requantize and quantize_output_tensor are ignored
     */
    public boolean onlyCopy;
    /**
     * disable k-quant mixtures and quantize all tensors to the same type
     */
    public boolean pure;

}
