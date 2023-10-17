package chat.octet.model;


import chat.octet.model.beans.LlamaContextParams;
import chat.octet.model.beans.LlamaModelParams;
import chat.octet.model.beans.Metrics;
import chat.octet.model.enums.LlamaTokenType;
import chat.octet.model.exceptions.DecodeException;
import chat.octet.model.exceptions.ModelException;
import chat.octet.model.parameters.GenerateParameter;
import chat.octet.model.utils.Platform;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

/**
 * Llama.cpp API
 * <p>C++ source: llamajava.h, llamajava.cpp</p>
 *
 * @author <a href="https://github.com/eoctet">William</a>
 * @since b1345
 */
public class LlamaService {

    static {
        System.load(Platform.LIB_RESOURCE_PATH);
        initNative();
        llamaBackendInit(true);
    }

    /**
     * initial JNI context.
     */
    public static native void initNative();

    /**
     * Get llama model default params.
     *
     * @return LlamaModelParams
     */
    public static native LlamaModelParams getLlamaModelDefaultParams();

    /**
     * Get llama context default params.
     *
     * @return LlamaContextParams
     */
    public static native LlamaContextParams getLlamaContextDefaultParams();

    /**
     * Initialize the llama + ggml backend
     * If numa is true, use NUMA optimizations Call once at the start of the program.
     *
     * @param numa Use NUMA optimizations.
     */
    public static native void llamaBackendInit(boolean numa);

    /**
     * Call once at the end of the program.
     * NOTE: currently only used for MPI.
     */
    public static native void llamaBackendFree();

    /**
     * Load Llama model from file.
     *
     * @param modelPath Llama model file path.
     * @param params    Llama model params.
     * @see LlamaModelParams
     */
    public static native void loadLlamaModelFromFile(String modelPath, LlamaModelParams params) throws ModelException;

    /**
     * Create new context with model.
     *
     * @param params Llama context params.
     * @see LlamaContextParams
     */
    public static native void createNewContextWithModel(LlamaContextParams params) throws ModelException;

    /**
     * Close model and release all resources.
     */
    public static native void release();

    /**
     * Check whether MMAP is supported.
     *
     * @return boolean
     */
    public static native boolean isMmapSupported();

    /**
     * Check whether MLOCK is supported.
     *
     * @return boolean
     */
    public static native boolean isMlockSupported();

    /**
     * Get model context size.
     *
     * @return int
     */
    public static native int getContextSize();

    /**
     * Apply a LoRA adapter to a loaded model
     * path_base_model is the path to a higher quality model to use as a base for
     * the layers modified by the adapter. Can be NULL to use the current loaded model.
     * The model needs to be reloaded before applying a new adapter, otherwise the adapter
     * will be applied on top of the previous one.
     *
     * @param loraPath      LoRA adapter file path.
     * @param loraScale     LoRA scale.
     * @param baseModelPath Base model file path.
     * @param threads       Thread number.
     * @return int, Returns 0 on success, else failed.
     */
    public static native int loadLoraModelFromFile(String loraPath, float loraScale, String baseModelPath, int threads) throws ModelException;

    /**
     * Get Logits based on index, and the default index must be 0.
     *
     * @param index index
     * @return float[], Returns one-dimensional float array.
     */
    public static native float[] getLogits(int index);

    /**
     * Get Embeddings
     *
     * @return float[], Returns embedding float array of the text.
     */
    public static native float[] getEmbeddings();

    /**
     * Get token type code.
     *
     * @param token Token id.
     * @return int
     */
    public static native int getTokenType(int token);

    /**
     * Get special BOS token.
     *
     * @return int, Returns token id.
     */
    public static native int getTokenBOS();

    /**
     * Get special EOS token.
     *
     * @return int, Returns token id.
     */
    public static native int getTokenEOS();

    /**
     * Convert the provided text into tokens.
     * The tokens pointer must be large enough to hold the resulting tokens.
     * Returns the number of tokens on success, no more than n_max_tokens.
     *
     * @param buf          Text byte buffer.
     * @param bufferLength Text byte buffer length.
     * @param tokens       Empty token arrays, Used to receive the returned tokens.
     * @param maxTokens    Max token size, by default is context size.
     * @param addBos       Add special BOS token.
     * @return int, Returns a negative number on failure, else the number of tokens that would have been returned.
     */
    public static native int tokenize(byte[] buf, int bufferLength, int[] tokens, int maxTokens, boolean addBos);

    /**
     * Convert the token id to text piece.
     *
     * @param token        Token id.
     * @param buf          Input byte buffer.
     * @param bufferLength Input byte buffer length.
     * @return int, Returns byte buffer length of the piece.
     */
    public static native int tokenToPiece(int token, byte[] buf, int bufferLength);

    /**
     * Get sampling metrics
     *
     * @param reset Reset the counter when finished.
     * @return Metrics
     * @see Metrics
     */
    public static native Metrics getSamplingMetrics(boolean reset);

    /**
     * Get system parameter information.
     *
     * @return String
     */
    public static native String getSystemInfo();

    /**
     * Inference sampling the next token.
     *
     * @param logits         User-defined logits, Adjustments can be made via LogitsProcessor.
     * @param lastTokens     <code>last_tokens</code> Last token array.
     * @param lastTokensSize <code>last_tokens_size</code> Last token array size.
     * @param penalty        <code>repeat-penalty</code> Control the repetition of token sequences in the generated text.
     * @param alphaFrequency <code>frequency-penalty</code> Repeat alpha frequency penalty.
     * @param alphaPresence  <code>presence-penalty</code> Repeat alpha presence penalty.
     * @param penalizeNL     <code>no-penalize-nl</code> Disable penalization for newline tokens when applying the repeat penalty.
     * @param mirostatMode   <code>Mirostat Sampling</code> Use Mirostat sampling, controlling perplexity during text generation.
     * @param mirostatTAU    <code>mirostat-ent</code> Set the Mirostat target entropy.
     * @param mirostatETA    <code>mirostat-lr</code> Set the Mirostat learning rate.
     * @param temperature    <code>temperature</code> Adjust the randomness of the generated text.
     * @param topK           <code>TOP-K Sampling</code> Limit the next token selection to the K most probable tokens.
     * @param topP           <code>TOP-P Sampling</code> Limit the next token selection to a subset of tokens with a cumulative probability above a threshold P.
     * @param tsf            <code>Tail Free Sampling (TFS)</code> Enable tail free sampling with parameter z.
     * @param typical        <code>Typical Sampling</code> Enable typical sampling sampling with parameter p.
     * @param sequenceId     Generation sequence id.
     * @param pastTokenSize  Past token size.
     * @return int, Returns the sampled token id.
     * @see GenerateParameter
     */
    public static native int sampling(float[] logits, int[] lastTokens, int lastTokensSize, float penalty, float alphaFrequency, float alphaPresence, boolean penalizeNL, int mirostatMode, float mirostatTAU, float mirostatETA, float temperature, int topK, float topP, float tsf, float typical, int sequenceId, int pastTokenSize) throws DecodeException;

    /**
     * Load llama grammar by rules.
     *
     * @param grammarRules Grammar rules.
     * @return boolean, Returns true on success, else failed.
     */
    public static native boolean loadLlamaGrammar(String grammarRules);

    /**
     * Batch decoding.
     *
     * @param sequenceId    Specify a unique generation sequence id.
     * @param tokens        Arrays of tokens that need to be decoding.
     * @param inputLength   Input context length.
     * @param pastTokenSize Past token size.
     * @return int, Returns 0 on success, else failed.
     */
    public static native int batchDecode(int sequenceId, int[] tokens, int inputLength, int pastTokenSize);

    /**
     * Clear cache in K-V sequences.
     *
     * @param sequenceId Generation sequence id.
     * @param posStart   Start position.
     * @param posEnd     End position.
     */
    public static native void clearCache(int sequenceId, int posStart, int posEnd);

    /**
     * Clear cache in K-V sequences.
     *
     * @param sequenceId Generation sequence id.
     */
    public static void clearCache(int sequenceId) {
        clearCache(sequenceId, 0, getContextSize());
    }

    /**
     * Convert the provided text into tokens.
     *
     * @param text   Input text.
     * @param addBos Add special BOS token.
     * @return Returns a negative number on failure, else the number of tokens that would have been returned.
     */
    public static int[] tokenize(String text, boolean addBos) {
        Preconditions.checkNotNull(text, "Text cannot be null");
        int[] tokens = new int[getContextSize()];
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        int nextTokens = tokenize(textBytes, textBytes.length, tokens, getContextSize(), addBos);
        if (nextTokens < 0) {
            throw new ModelException(MessageFormat.format("Failed to tokenize: {0}, next_tokens: {1}", text, nextTokens));
        }
        return ArrayUtils.subarray(tokens, 0, nextTokens);
    }

    /**
     * Get token type define.
     *
     * @param token Token id.
     * @return LlamaTokenType
     * @see LlamaTokenType
     */
    public static LlamaTokenType getLlamaTokenType(int token) {
        return LlamaTokenType.valueOfType(getTokenType(token));
    }

}
