package chat.octet.model;


import chat.octet.model.beans.LlamaContextParams;
import chat.octet.model.beans.Metrics;
import chat.octet.model.exceptions.ModelException;
import chat.octet.model.parameters.GenerateParameter;
import chat.octet.model.utils.Platform;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

public class LlamaService {

    public static int batchSize;
    public static int threads;

    static {
        System.load(Platform.LIB_RESOURCE_PATH);
        initNative();
        llamaBackendInit(true);
    }

    public static native void initNative();

    public static native LlamaContextParams getLlamaContextDefaultParams();

    public static native void llamaBackendInit(boolean numa);

    @Deprecated
    public static native void llamaBackendFree();

    public static native void loadLlamaModelFromFile(String modelPath, LlamaContextParams params);

    public static native void createNewContextWithModel(LlamaContextParams params);

    public static native void release();

    public static native int getMaxDevices();

    public static native boolean isMmapSupported();

    public static native boolean isMlockSupported();

    public static native int getVocabSize();

    public static native int getContextSize();

    public static native int getEmbeddingSize();

    public static native int getVocabType();

    public static native int getModelVocabSize();

    public static native int getModelContextSize();

    public static native int getModelEmbeddingSize();

    public static native int loadLoraModelFromFile(String loraPath, String baseModelPath, int threads);

    public static native int evaluate(int[] tokens, int nTokens, int nPast, int threads);

    public static native float[] getLogits();

    public static native float[] getEmbeddings();

    public static native String getTokenText(int token);

    public static native float getTokenScore(int token);

    public static native int getTokenType(int token);

    public static native int getTokenBOS();

    public static native int getTokenEOS();

    public static native int getTokenNL();

    public static native int tokenize(byte[] buf, int bufferLength, int[] tokens, int maxTokens, boolean addBos);

    public static native int tokenizeWithModel(byte[] buf, int bufferLength, int[] tokens, int maxTokens, boolean addBos);

    public static native int getTokenToPiece(int token, byte[] buf, int bufferLength);

    public static native int getTokenToPieceWithModel(int token, byte[] buf, int bufferLength);

    public static native Metrics getSamplingMetrics(boolean reset);

    public static native String getSystemInfo();

    public static native int sampling(float[] logits, int[] lastTokens, int lastTokensSize, float penalty, float alphaFrequency, float alphaPresence, boolean penalizeNL, int mirostatMode, float mirostatTAU, float mirostatETA, float temperature, int topK, float topP, float tsf, float typical);


    public static float[] embedding(String text) {
        Preconditions.checkNotNull(text, "Text cannot be null");
        int[] tokens = tokenize(new String(text.getBytes(StandardCharsets.UTF_8)), true);
        evaluate(tokens, 0, tokens.length);
        return getEmbeddings();
    }

    public static int[] tokenize(String text, boolean addBos) {
        Preconditions.checkNotNull(text, "Text cannot be null");
        int[] tokens = new int[getContextSize()];
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        int nextTokens = tokenizeWithModel(textBytes, textBytes.length, tokens, getContextSize(), addBos);
        if (nextTokens < 0) {
            throw new ModelException(MessageFormat.format("failed to tokenize: {0}, next_tokens: {1}", text, nextTokens));
        }
        return ArrayUtils.subarray(tokens, 0, nextTokens);
    }

    public static int evaluate(int[] inputIds, int pastTokensSize, int inputLength) {
        int pastTokens = pastTokensSize;

        int evaluateTokenSize;
        int evaluateSize;
        for (evaluateTokenSize = 0; pastTokens < inputLength; evaluateTokenSize += evaluateSize) {
            evaluateSize = inputLength - pastTokens;
            if (evaluateSize > batchSize) {
                evaluateSize = batchSize;
            }

            int endIndex = evaluateSize + pastTokens;
            int[] batchTokens = ArrayUtils.subarray(inputIds, pastTokens, endIndex);
            int returnCode = evaluate(batchTokens, evaluateSize, pastTokens, threads);
            if (returnCode != 0) {
                throw new ModelException("Llama_eval returned " + returnCode);
            }
            pastTokens += evaluateSize;
        }
        return evaluateTokenSize;
    }

    public static int sampling(GenerateParameter generateParams, float[] logits, int[] inputIds, int inputLength, int lastTokensSize) {
        //int startIndex = Math.max(0, inputLength - getLastTokensSize());
        //int[] lastTokens = ArrayUtils.subarray(inputIds, startIndex, inputLength);
        int[] lastTokens = ArrayUtils.subarray(inputIds, 0, inputLength);
        return sampling(
                logits,
                lastTokens,
                lastTokensSize,
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
                generateParams.getTypical()
        );
    }
}
