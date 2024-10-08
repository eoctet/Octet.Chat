package chat.octet.model;


import chat.octet.model.beans.LlamaContextParams;
import chat.octet.model.beans.LlamaModelParams;
import chat.octet.model.beans.LlamaModelQuantizeParams;
import chat.octet.model.beans.Metrics;
import chat.octet.model.enums.LlamaSpecialTokenType;
import chat.octet.model.enums.LlamaTokenAttr;
import chat.octet.model.enums.ModelFileType;
import chat.octet.model.exceptions.DecodeException;
import chat.octet.model.exceptions.ModelException;
import chat.octet.model.parameters.GenerateParameter;
import chat.octet.model.utils.Platform;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.Optional;

/**
 * Llama.cpp API
 * <p>C++ source: llamajava.h, llamajava.cpp</p>
 *
 * @author <a href="https://github.com/eoctet">William</a>
 * @since b3091 20240611
 */
@Slf4j
public class LlamaService {

    static {
        System.load(Platform.LIB_RESOURCE_PATH);

        String logLevel = Optional.ofNullable(System.getProperty("octet.llama.log")).orElse("info");
        if ("debug".equalsIgnoreCase(logLevel)) {
            setLogLevel(0);
        } else if ("warn".equalsIgnoreCase(logLevel)) {
            setLogLevel(2);
        } else if ("error".equalsIgnoreCase(logLevel)) {
            setLogLevel(3);
        } else {
            setLogLevel(1);
        }
    }

    /**
     * initial JNI context.
     */
    public static native void setLogLevel(int logLevel);

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
     * Get llama model quantize default params.
     *
     * @return LlamaModelQuantizeParams
     */
    public static native LlamaModelQuantizeParams getLlamaModelQuantizeDefaultParams();

    /**
     * Call once at the end of the program.
     * NOTE: currently only used for MPI.
     */
    public static native void llamaBackendFree();

    /**
     * Load Llama model from file.
     *
     * @param modelPath     Llama model file path.
     * @param modelParams   Llama model params.
     * @param contextParams Llama context params.
     * @see LlamaModelParams
     * @see LlamaContextParams
     */
    public static native void loadLlamaModelFromFile(String modelPath, LlamaModelParams modelParams, LlamaContextParams contextParams) throws ModelException;

    /**
     * Close model and release all resources.
     */
    public static native void release();

    /**
     * Check whether mmap is supported.
     *
     * @return boolean
     */
    public static native boolean isMmapSupported();

    /**
     * Check whether mlock is supported.
     *
     * @return boolean
     */
    public static native boolean isMlockSupported();

    /**
     * Check whether gpu_offload is supported.
     *
     * @return boolean
     */
    public static native boolean isGpuOffloadSupported();

    /**
     * Get model vocab size.
     *
     * @return int
     */
    public static native int getVocabSize();

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
     * Get embedding
     *
     * @return float[], Returns embedding float array.
     */
    public static native float[] getEmbedding();

    /**
     * Get token type code.
     *
     * @param token Token id.
     * @return int
     */
    public static native int getTokenAttr(int token);

    /**
     * Get token type define.
     *
     * @param token Token id.
     * @return LlamaTokenAttr
     * @see LlamaTokenAttr
     */
    public static LlamaTokenAttr getLlamaTokenAttr(int token) {
        return LlamaTokenAttr.valueOfType(getTokenAttr(token));
    }

    /**
     * Convert the provided text into tokens.
     * The tokens pointer must be large enough to hold the resulting tokens.
     * Returns the number of tokens on success, no more than n_max_tokens.
     *
     * @param buf          Text byte buffer.
     * @param bufferLength Text byte buffer length.
     * @param tokens       Empty token arrays, Used to receive the returned tokens.
     * @param maxTokens    Max token size, by default is context size.
     * @param addSpecial   Add special BOS token.
     * @param parseSpecial Allow tokenizing special and/or control tokens which otherwise are not exposed and treated as plaintext. Does not insert a leading space.
     * @return int, Returns a negative number on failure, else the number of tokens that would have been returned.
     */
    public static native int tokenize(byte[] buf, int bufferLength, int[] tokens, int maxTokens, boolean addSpecial, boolean parseSpecial);

    /**
     * Convert the token id to text piece.
     *
     * @param token        Token id.
     * @param buf          Input byte buffer.
     * @param bufferLength Input byte buffer length.
     * @param lstripLength User can skip up to 'lstrip' leading spaces before copying.
     * @param special      If true, special tokens are rendered in the output.
     * @return int, Returns byte buffer length of the piece.
     */
    public static native int tokenToPiece(int token, byte[] buf, int bufferLength, int lstripLength, boolean special);

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
     * @param logits           User-defined logits, Adjustments can be made via LogitsProcessor.
     * @param lastTokens       Last token array.
     * @param lastTokensSize   Last token array size.
     * @param penalty          Control the repetition of token sequences in the generated text.
     * @param alphaFrequency   Repeat alpha frequency penalty.
     * @param alphaPresence    Repeat alpha presence penalty.
     * @param penalizeNL       Disable penalization for newline tokens when applying the repeat penalty.
     * @param mirostatMode     <b>Mirostat Sampling</b> Use Mirostat sampling, controlling perplexity during text generation.
     * @param mirostatTAU      <b>Mirostat Sampling</b> Set the Mirostat target entropy.
     * @param mirostatETA      <b>Mirostat Sampling</b> Set the Mirostat learning rate.
     * @param temperature      Adjust the randomness of the generated text.
     * @param topK             <b>TOP-K Sampling</b> Limit the next token selection to the K most probable tokens.
     * @param topP             <b>TOP-P Sampling</b> Limit the next token selection to a subset of tokens with a cumulative probability above a threshold P.
     * @param tsf              <b>Tail Free Sampling (TFS)</b> Enable tail free sampling with parameter z.
     * @param typical          <b>Typical Sampling</b> Enable typical sampling sampling with parameter p.
     * @param minP             <b>MIN-P Sampling</b> Sets a minimum base probability threshold for token selection.
     * @param dynatempRange    <b>Dynamic Temperature Sampling</b> Dynamic temperature range.
     * @param dynatempExponent <b>Dynamic Temperature Sampling</b> Dynamic temperature exponent.
     * @param sequenceId       Generation sequence id.
     * @param pastTokenSize    Past token size.
     * @return int, Returns the sampled token id.
     * @see GenerateParameter
     */
    public static native int sampling(float[] logits, int[] lastTokens, int lastTokensSize, float penalty, float alphaFrequency, float alphaPresence, boolean penalizeNL, int mirostatMode, float mirostatTAU, float mirostatETA, float temperature, int topK, float topP, float tsf, float typical, float minP, float dynatempRange, float dynatempExponent, int sequenceId, int pastTokenSize) throws DecodeException;

    /**
     * Inference sampling the next token.
     *
     * @param generateParams generation parameter.
     * @param logits         User-defined logits, Adjustments can be made via LogitsProcessor.
     * @param lastTokens     Last token array.
     * @param sequenceId     Generation sequence id.
     * @param pastTokenSize  Past token size.
     * @return int, Returns the sampled token id.
     * @see GenerateParameter
     */
    public static int sampling(GenerateParameter generateParams, float[] logits, int[] lastTokens, int sequenceId, int pastTokenSize) throws DecodeException {
        return sampling(
                logits,
                lastTokens,
                generateParams.getLastTokensSize(),
                generateParams.getRepeatPenalty(),
                generateParams.getFrequencyPenalty(),
                generateParams.getPresencePenalty(),
                generateParams.isPenalizeNl(),
                generateParams.getMirostatMode().ordinal(),
                generateParams.getMirostatTAU(),
                generateParams.getMirostatETA(),
                generateParams.getTemperature(),
                generateParams.getTopK(),
                generateParams.getTopP(),
                generateParams.getTsf(),
                generateParams.getTypical(),
                generateParams.getMinP(),
                generateParams.getDynatempRange(),
                generateParams.getDynatempExponent(),
                sequenceId,
                pastTokenSize
        );
    }

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
     * Quantize the model.
     *
     * @param sourceModelFilePath Source model file path.
     * @param outputModelFilePath Output model file path.
     * @param params              Quantize parameters.
     * @return int, Returns 0 on success, else failed.
     */
    public static native int llamaModelQuantize(String sourceModelFilePath, String outputModelFilePath, LlamaModelQuantizeParams params);

    /**
     * Quantize the model.
     *
     * @param sourceModelFilePath Source model file path.
     * @param outputModelFilePath Output model file path.
     * @param modelFileType       Model file type.
     * @return int, Returns 0 on success, else failed.
     * @see ModelFileType
     */
    public static int llamaModelQuantize(String sourceModelFilePath, String outputModelFilePath, ModelFileType modelFileType) {
        if (!Files.exists(new File(sourceModelFilePath).toPath())) {
            throw new ModelException("Source model file is not exists, please check the file path");
        }
        LlamaModelQuantizeParams defaultParams = LlamaService.getLlamaModelQuantizeDefaultParams();
        defaultParams.modelFileType = modelFileType.getType();
        return llamaModelQuantize(sourceModelFilePath, outputModelFilePath, defaultParams);
    }

    /**
     * Convert the provided text into tokens.
     *
     * @param text         Input text.
     * @param addSpecial   Add special BOS token.
     * @param parseSpecial Allow tokenizing special and/or control tokens which otherwise are not exposed and treated as plaintext. Does not insert a leading space.
     * @return Returns a negative number on failure, else the number of tokens that would have been returned.
     */
    public static int[] tokenize(String text, boolean addSpecial, boolean parseSpecial) {
        Preconditions.checkNotNull(text, "Text cannot be null");
        int[] tokens = new int[getContextSize()];
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        int nextTokens = tokenize(textBytes, textBytes.length, tokens, getContextSize(), addSpecial, parseSpecial);
        if (nextTokens < 0) {
            throw new ModelException(MessageFormat.format("Failed to tokenize: {0}, next_tokens: {1}", text, nextTokens));
        }
        return ArrayUtils.subarray(tokens, 0, nextTokens);
    }

    /**
     * Retrieves the metadata information of the llama model based on the given key.
     * This native method is used to obtain specific information about the model, which is identified by the key parameter.
     * The information obtained may include model structure, parameters, version, etc.
     *
     * @param key The key used to identify the specific metadata information to retrieve.
     * @return String, The metadata information corresponding to the key. Returns a String type,
     * which may be a description of the model, the model's version number, or other information.
     */
    public static native String llamaModelMeta(String key);

    /**
     * Get special token id by type.
     *
     * @param llamaSpecialTokenType Special token type.
     * @return Token id.
     * @see LlamaSpecialTokenType
     */
    public static native int getSpecialToken(int llamaSpecialTokenType);

    /**
     * Get BOS token id.
     *
     * @return Token id.
     */
    public static int getBosToken() {
        return getSpecialToken(LlamaSpecialTokenType.TOKEN_BOS.getType());
    }

    /**
     * Get EOS token id.
     *
     * @return Token id.
     */
    public static int getEosToken() {
        return getSpecialToken(LlamaSpecialTokenType.TOKEN_EOS.getType());
    }

    /**
     * Get EOT token id.
     *
     * @return Token id.
     */
    public static int getEotToken() {
        return getSpecialToken(LlamaSpecialTokenType.TOKEN_EOT.getType());
    }

    /**
     * Get prefix token id.
     *
     * @return Token id.
     */
    public static int getPrefixToken() {
        int prefixTokenId = getSpecialToken(LlamaSpecialTokenType.TOKEN_PREFIX.getType());
        if (prefixTokenId == -1) {
            log.error("Prefix token is not found in model, please set prefix token in generate parameter.");
        }
        return prefixTokenId;
    }

    /**
     * Get suffix token id.
     *
     * @return Token id.
     */
    public static int getSuffixToken() {
        int suffixTokenId = getSpecialToken(LlamaSpecialTokenType.TOKEN_SUFFIX.getType());
        if (suffixTokenId == -1) {
            log.error("Suffix token is not found in model, please set suffix token in generate parameter.");
        }
        return suffixTokenId;
    }

    /**
     * Get middle token id.
     *
     * @return Token id.
     */
    public static int getMiddleToken() {
        int middleTokenId = getSpecialToken(LlamaSpecialTokenType.TOKEN_MIDDLE.getType());
        if (middleTokenId == -1) {
            log.error("Middle token is not found in model, please set middle token in generate parameter.");
        }
        return middleTokenId;
    }


    /**
     * Whether to add BOS token.
     *
     * @return boolean
     */
    public static boolean addBosToken() {
        return Boolean.parseBoolean(Optional.ofNullable(llamaModelMeta("tokenizer.ggml.add_bos_token")).orElse("false"));
    }

    /**
     * Whether to add EOS token.
     *
     * @return boolean
     */
    public static boolean addEosToken() {
        return Boolean.parseBoolean(Optional.ofNullable(llamaModelMeta("tokenizer.ggml.add_eos_token")).orElse("false"));
    }

    /**
     * Whether to add space prefix.
     *
     * @return boolean
     */
    public static boolean addSpacePrefix() {
        return Boolean.parseBoolean(Optional.ofNullable(llamaModelMeta("tokenizer.ggml.add_space_prefix")).orElse("false"));
    }

    /**
     * Whether the token is the end of generation.
     *
     * @param tokenId Token id.
     * @return boolean
     */
    public static boolean isEndOfGeneration(int tokenId) {
        return tokenId != -1 && (tokenId == getEosToken() || tokenId == getEotToken());
    }

}
