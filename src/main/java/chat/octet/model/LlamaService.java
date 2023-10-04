package chat.octet.model;


import chat.octet.model.beans.LlamaContextParams;
import chat.octet.model.beans.LlamaModelParams;
import chat.octet.model.beans.Metrics;
import chat.octet.model.exceptions.ModelException;
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
 * @since b1317
 */
public class LlamaService {

    static {
        System.load(Platform.LIB_RESOURCE_PATH);
        initNative();
        llamaBackendInit(true);
    }

    public static native void initNative();

    public static native LlamaModelParams getLlamaModelDefaultParams();

    public static native LlamaContextParams getLlamaContextDefaultParams();

    public static native void llamaBackendInit(boolean numa);

    public static native void llamaBackendFree();

    public static native void loadLlamaModelFromFile(String modelPath, LlamaModelParams params) throws ModelException;

    public static native void createNewContextWithModel(LlamaContextParams params) throws ModelException;

    public static native void release();

    public static native int getMaxDevices();

    public static native boolean isMmapSupported();

    public static native boolean isMlockSupported();

    public static native int getVocabSize();

    public static native int getContextSize();

    public static native int getEmbeddingSize();

    public static native int getVocabType();

    public static native int loadLoraModelFromFile(String loraPath, float loraScale, String baseModelPath, int threads) throws ModelException;

    public static native float[] getLogits(int index);

    public static native float[] getEmbeddings();

    public static native String getTokenText(int token);

    public static native float getTokenScore(int token);

    public static native int getTokenType(int token);

    public static native int getTokenBOS();

    public static native int getTokenEOS();

    public static native int getTokenNL();

    public static native int tokenize(byte[] buf, int bufferLength, int[] tokens, int maxTokens, boolean addBos);

    public static native int tokenToPiece(int token, byte[] buf, int bufferLength);

    public static native Metrics getSamplingMetrics(boolean reset);

    public static native String getSystemInfo();

    public static native int sampling(float[] logits, int[] lastTokens, int lastTokensSize, float penalty, float alphaFrequency, float alphaPresence, boolean penalizeNL, int mirostatMode, float mirostatTAU, float mirostatETA, float temperature, int topK, float topP, float tsf, float typical, int sequenceId, int pastTokens) throws ModelException;

    public static native boolean loadLlamaGrammar(String grammarRules);

    public static native int batchDecode(int[] tokens) throws ModelException;

    public static native void clearCache(int sequenceId, int posStart, int posEnd);

    public static void clearCache(int sequenceId) {
        clearCache(sequenceId, 0, getContextSize());
    }

    public static int[] tokenize(String text, boolean addBos) {
        Preconditions.checkNotNull(text, "Text cannot be null");
        int[] tokens = new int[getContextSize()];
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        int nextTokens = tokenize(textBytes, textBytes.length, tokens, getContextSize(), addBos);
        if (nextTokens < 0) {
            throw new ModelException(MessageFormat.format("failed to tokenize: {0}, next_tokens: {1}", text, nextTokens));
        }
        return ArrayUtils.subarray(tokens, 0, nextTokens);
    }

}
