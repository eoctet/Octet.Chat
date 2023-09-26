package chat.octet.model;


import chat.octet.model.beans.LlamaContext;
import chat.octet.model.beans.LlamaContextParams;
import chat.octet.model.beans.LlamaModel;

public class LlamaService {

    static {
        System.load(Platform.LIB_RESOURCE_PATH);
        initLocal();
        llamaBackendInit(true);
    }

    public static native void initLocal();

    public static native LlamaContextParams getLlamaContextDefaultParams();

    public static native void llamaBackendInit(boolean numa);

    public static native void llamaBackendFree();

    public static native LlamaModel loadLlamaModelFromFile(String modelPath, LlamaContextParams params);

    public static native LlamaContext createNewContextWithModel(LlamaModel model, LlamaContextParams params);

    public static native void releaseLlamaModel(LlamaModel model);

    public static native void releaseLlamaContext(LlamaContext ctx);

    public static native int getMaxDevices();

    public static native boolean isMmapSupported();

    public static native boolean isMlockSupported();

    public static native int getVocabSize(LlamaContext ctx);

    public static native int getContextSize(LlamaContext ctx);

    public static native int getEmbeddingSize(LlamaContext ctx);

    public static native int getVocabType(LlamaContext ctx);

    public static native int getModelVocabSize(LlamaModel model);

    public static native int getModelContextSize(LlamaModel model);

    public static native int getModelEmbeddingSize(LlamaModel model);

    public static native int loadLoraModelFromFile(LlamaModel model, String loraPath, String baseModelPath, int threads);

    public static native int evaluate(LlamaContext ctx, int[] tokens, int nTokens, int nPast, int threads);

    public static native float[] getLogits(LlamaContext ctx);

    public static native float[] getEmbeddings(LlamaContext ctx);

    public static native String getTokenText(LlamaContext ctx, int token);

    public static native float getTokenScore(LlamaContext ctx, int token);

    public static native int getTokenType(LlamaContext ctx, int token);

    public static native int getTokenBOS(LlamaContext ctx);

    public static native int getTokenEOS(LlamaContext ctx);

    public static native int getTokenNL(LlamaContext ctx);

    public static native int tokenize(LlamaContext ctx, byte[] buf, int textLength, int[] tokens, int maxTokens, boolean addBos);

    public static native int tokenizeWithModel(LlamaModel model, byte[] buf, int textLength, int[] tokens, int maxTokens, boolean addBos);

    public static native int getTokenToPiece(LlamaContext ctx, int token, byte[] buf, int length);

    public static native int getTokenToPieceWithModel(LlamaModel model, int token, byte[] buf, int length);

    public static native void printTimings(LlamaContext ctx);

    public static native String printSystemInfo();

    public static native int sampling(LlamaContext ctx, float[] logits, int[] lastTokens, int lastTokensSize, float penalty, float alphaFrequency, float alphaPresence, boolean penalizeNL, int mirostatMode, float mirostatTAU, float mirostatETA, float temperature, int topK, float topP, float tsf, float typical);

}
