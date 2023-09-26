package chat.octet.model;


import chat.octet.model.beans.LlamaContextParams;

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

    public static native int tokenize(byte[] buf, int textLength, int[] tokens, int maxTokens, boolean addBos);

    public static native int tokenizeWithModel(byte[] buf, int textLength, int[] tokens, int maxTokens, boolean addBos);

    public static native int getTokenToPiece(int token, byte[] buf, int length);

    public static native int getTokenToPieceWithModel(int token, byte[] buf, int length);

    public static native void printTimings();

    public static native String printSystemInfo();

    public static native int sampling(float[] logits, int[] lastTokens, int lastTokensSize, float penalty, float alphaFrequency, float alphaPresence, boolean penalizeNL, int mirostatMode, float mirostatTAU, float mirostatETA, float temperature, int topK, float topP, float tsf, float typical);

}
