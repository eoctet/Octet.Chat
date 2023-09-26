//
// Created by William W on 2023/9/13.
//
#include "llama.h"

#include "llamajava.h"

//Global ref
llama_model *llama_model_global;
llama_context *llama_context_global;

jclass MODEL_EXCEPTION;

//Class LlamaContextParams:
jclass LLAMA_CONTEXT_PARAMS_CLASS;
jmethodID METHOD_INIT_CONTEXT_PARAMS;
jfieldID FIELD_SEED;
jfieldID FIELD_CTX;
jfieldID FIELD_BATCH;
jfieldID FIELD_GPU_LAYERS;
jfieldID FIELD_MAIN_GPU;
jfieldID FIELD_ROPE_FREQ_BASE;
jfieldID FIELD_ROPE_FREQ_SCALE;
jfieldID FIELD_LOW_VRAM;
jfieldID FIELD_MUL_MAT_Q;
jfieldID FIELD_F16_KV;
jfieldID FIELD_LOGITS_ALL;
jfieldID FIELD_VOCAB_ONLY;
jfieldID FIELD_USE_MMAP;
jfieldID FIELD_USE_MLOCK;
jfieldID FIELD_EMBEDDING;

//
jstring ToJString(JNIEnv *env, const char *value) {
    return env->NewStringUTF(value);
}

const char *ToCString(JNIEnv *env, jstring value) {
    return env->GetStringUTFChars(value, JNI_FALSE);
}

jboolean ToJBoolean(bool value) {
    return value ? JNI_TRUE : JNI_FALSE;
}

bool ToCBool(jboolean value) {
    return value == JNI_TRUE;
}

void *GetObjectPointer(JNIEnv *env, jobject obj_pointer) {
    return env->GetDirectBufferAddress(obj_pointer);
}

jobject NewObjectPointer(JNIEnv *env, void *obj) {
    return env->NewDirectByteBuffer(obj, sizeof(obj));
}

struct llama_context_params GetLlamaContextParams(JNIEnv *env, jobject context_params) {
    llama_context_params params = {
            /*.seed                        =*/  (uint32_t) env->GetIntField(context_params, FIELD_SEED),
            /*.n_ctx                       =*/  env->GetIntField(context_params, FIELD_CTX),
            /*.n_batch                     =*/  env->GetIntField(context_params, FIELD_BATCH),
            /*.n_gpu_layers                =*/  env->GetIntField(context_params, FIELD_GPU_LAYERS),
            /*.main_gpu                    =*/  env->GetIntField(context_params, FIELD_MAIN_GPU),
            /*.tensor_split                =*/  nullptr,
            /*.rope_freq_base              =*/  env->GetFloatField(context_params, FIELD_ROPE_FREQ_BASE),
            /*.rope_freq_scale             =*/  env->GetFloatField(context_params, FIELD_ROPE_FREQ_SCALE),
            /*.progress_callback           =*/  nullptr,
            /*.progress_callback_user_data =*/  nullptr,
            /*.low_vram                    =*/  ToCBool(env->GetBooleanField(context_params, FIELD_LOW_VRAM)),
            /*.mul_mat_q                   =*/  ToCBool(env->GetBooleanField(context_params, FIELD_MUL_MAT_Q)),
            /*.f16_kv                      =*/  ToCBool(env->GetBooleanField(context_params, FIELD_F16_KV)),
            /*.logits_all                  =*/  ToCBool(env->GetBooleanField(context_params, FIELD_LOGITS_ALL)),
            /*.vocab_only                  =*/  ToCBool(env->GetBooleanField(context_params, FIELD_VOCAB_ONLY)),
            /*.use_mmap                    =*/  ToCBool(env->GetBooleanField(context_params, FIELD_USE_MMAP)),
            /*.use_mlock                   =*/  ToCBool(env->GetBooleanField(context_params, FIELD_USE_MLOCK)),
            /*.embedding                   =*/  ToCBool(env->GetBooleanField(context_params, FIELD_EMBEDDING))
    };
    return params;
}

/*
* Class:     chat_octet_model_LlamaService
* Method:    initLocal
*/
JNIEXPORT void JNICALL Java_chat_octet_model_LlamaService_initLocal
        (JNIEnv *env, jclass thisClass) {

    MODEL_EXCEPTION = env->FindClass("chat/octet/model/exceptions/ModelException");

    //Class LlamaContextParams
    LLAMA_CONTEXT_PARAMS_CLASS = env->FindClass("chat/octet/model/beans/LlamaContextParams");
    METHOD_INIT_CONTEXT_PARAMS = env->GetMethodID(LLAMA_CONTEXT_PARAMS_CLASS, "<init>", "()V");
    FIELD_SEED = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "seed", "I");
    FIELD_CTX = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "ctx", "I");
    FIELD_BATCH = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "batch", "I");
    FIELD_GPU_LAYERS = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "gpuLayers", "I");
    FIELD_MAIN_GPU = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "mainGpu", "I");
    FIELD_ROPE_FREQ_BASE = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "ropeFreqBase", "F");
    FIELD_ROPE_FREQ_SCALE = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "ropeFreqScale", "F");
    FIELD_LOW_VRAM = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "lowVram", "Z");
    FIELD_MUL_MAT_Q = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "mulMatQ", "Z");
    FIELD_F16_KV = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "f16KV", "Z");
    FIELD_LOGITS_ALL = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "logitsAll", "Z");
    FIELD_VOCAB_ONLY = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "vocabOnly", "Z");
    FIELD_USE_MMAP = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "mmap", "Z");
    FIELD_USE_MLOCK = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "mlock", "Z");
    FIELD_EMBEDDING = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "embedding", "Z");

}

/*
* Class:     chat_octet_model_LlamaService
* Method:    getLlamaContextDefaultParams
*/
JNIEXPORT jobject JNICALL Java_chat_octet_model_LlamaService_getLlamaContextDefaultParams
        (JNIEnv *env, jclass thisClass) {
    llama_context_params defaults = llama_context_default_params();

    //new llama_context_params object instance
    jclass llama_context_params_class = env->FindClass("chat/octet/model/beans/LlamaContextParams");
    jmethodID method_init_context_params = env->GetMethodID(llama_context_params_class, "<init>", "()V");
    jobject llama_context_params = env->NewObject(llama_context_params_class, method_init_context_params);

    //set values
    env->SetIntField(llama_context_params, FIELD_SEED, defaults.seed);
    env->SetIntField(llama_context_params, FIELD_CTX, defaults.n_ctx);
    env->SetIntField(llama_context_params, FIELD_BATCH, defaults.n_batch);
    env->SetIntField(llama_context_params, FIELD_GPU_LAYERS, defaults.n_gpu_layers);
    env->SetIntField(llama_context_params, FIELD_MAIN_GPU, defaults.main_gpu);
    env->SetFloatField(llama_context_params, FIELD_ROPE_FREQ_BASE, defaults.rope_freq_base);
    env->SetFloatField(llama_context_params, FIELD_ROPE_FREQ_SCALE, defaults.rope_freq_scale);
    env->SetBooleanField(llama_context_params, FIELD_LOW_VRAM, ToJBoolean(defaults.low_vram));
    env->SetBooleanField(llama_context_params, FIELD_MUL_MAT_Q, ToJBoolean(defaults.mul_mat_q));
    env->SetBooleanField(llama_context_params, FIELD_F16_KV, ToJBoolean(defaults.f16_kv));
    env->SetBooleanField(llama_context_params, FIELD_LOGITS_ALL, ToJBoolean(defaults.logits_all));
    env->SetBooleanField(llama_context_params, FIELD_VOCAB_ONLY, ToJBoolean(defaults.vocab_only));
    env->SetBooleanField(llama_context_params, FIELD_USE_MMAP, ToJBoolean(defaults.use_mmap));
    env->SetBooleanField(llama_context_params, FIELD_USE_MLOCK, ToJBoolean(defaults.use_mlock));
    env->SetBooleanField(llama_context_params, FIELD_EMBEDDING, ToJBoolean(defaults.embedding));

    return llama_context_params;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    llamaBackendInit
 */
JNIEXPORT void JNICALL Java_chat_octet_model_LlamaService_llamaBackendInit
        (JNIEnv *env, jclass thisClass, jboolean numa) {
    llama_backend_init(ToCBool(numa));
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    llamaBackendFree
 */
JNIEXPORT void JNICALL Java_chat_octet_model_LlamaService_llamaBackendFree
        (JNIEnv *env, jclass thisClass) {
    llama_backend_free();
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    loadLlamaModelFromFile
 */
JNIEXPORT void JNICALL Java_chat_octet_model_LlamaService_loadLlamaModelFromFile
        (JNIEnv *env, jclass thisClass, jstring modelPath, jobject llamaContextParams) {
    struct llama_context_params params = GetLlamaContextParams(env, llamaContextParams);
    llama_model_global = llama_load_model_from_file(ToCString(env, modelPath), params);

    if (!llama_model_global) {
        env->ThrowNew(MODEL_EXCEPTION, "Load model failed");
    }
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    createNewContextWithModel
 */
JNIEXPORT void JNICALL Java_chat_octet_model_LlamaService_createNewContextWithModel
        (JNIEnv *env, jclass thisClass, jobject llamaContextParams) {
    struct llama_context_params params = GetLlamaContextParams(env, llamaContextParams);
    llama_context_global = llama_new_context_with_model(llama_model_global, params);

    if (!llama_context_global) {
        env->ThrowNew(MODEL_EXCEPTION, "Create llama context failed");
    }
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    release
 */
JNIEXPORT void JNICALL Java_chat_octet_model_LlamaService_release
        (JNIEnv *env, jclass thisClass) {
    if (NULL != llama_model_global) {
        llama_free_model(llama_model_global);
    }
    if (NULL != llama_context_global) {
        llama_free(llama_context_global);
    }
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getMaxDevices
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getMaxDevices
        (JNIEnv *env, jclass thisClass) {
    return llama_max_devices();
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    isMmapSupported
 */
JNIEXPORT jboolean JNICALL Java_chat_octet_model_LlamaService_isMmapSupported
        (JNIEnv *env, jclass thisClass) {
    return ToJBoolean(llama_mmap_supported());
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    isMlockSupported
 */
JNIEXPORT jboolean JNICALL Java_chat_octet_model_LlamaService_isMlockSupported
        (JNIEnv *env, jclass thisClass) {
    return ToJBoolean(llama_mlock_supported());
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getVocabSize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getVocabSize
        (JNIEnv *env, jclass thisClass) {
    return llama_n_vocab(llama_context_global);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getContextSize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getContextSize
        (JNIEnv *env, jclass thisClass) {
    return llama_n_ctx(llama_context_global);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getEmbeddingSize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getEmbeddingSize
        (JNIEnv *env, jclass thisClass) {
    return llama_n_embd(llama_context_global);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getVocabType
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getVocabType
        (JNIEnv *env, jclass thisClass) {
    return llama_vocab_type(llama_context_global);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getModelVocabSize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getModelVocabSize
        (JNIEnv *env, jclass thisClass) {
    return llama_model_n_vocab(llama_model_global);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getModelContextSize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getModelContextSize
        (JNIEnv *env, jclass thisClass) {
    return llama_model_n_ctx(llama_model_global);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getModelEmbeddingSize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getModelEmbeddingSize
        (JNIEnv *env, jclass thisClass) {
    return llama_model_n_embd(llama_model_global);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    loadLoraModelFromFile
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_loadLoraModelFromFile
        (JNIEnv *env, jclass thisClass, jstring loraPath, jstring baseModelPath,
         jint threads) {
    if (!llama_model_global) {
        env->ThrowNew(MODEL_EXCEPTION, "llama model cannot be null");
        return -1;
    }
    return llama_model_apply_lora_from_file(llama_model_global, ToCString(env, loraPath), ToCString(env, baseModelPath),
                                            threads);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    evaluate
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_evaluate
        (JNIEnv *env, jclass thisClass, jintArray tokensArrays, jint nTokens, jint nPast,
         jint threads) {
    llama_token *tokens = (llama_token *) env->GetIntArrayElements(tokensArrays, JNI_FALSE);

    int code = llama_eval(llama_context_global, tokens, nTokens, nPast, threads);
    env->ReleaseIntArrayElements(tokensArrays, tokens, 0);

    return code;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getLogits
 */
JNIEXPORT jfloatArray JNICALL Java_chat_octet_model_LlamaService_getLogits
        (JNIEnv *env, jclass thisClass) {
    float *logits = llama_get_logits(llama_context_global);
    int vocab_size = llama_n_vocab(llama_context_global);

    jfloatArray arrays = env->NewFloatArray(vocab_size);
    env->SetFloatArrayRegion(arrays, 0, vocab_size, logits);

    return arrays;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getEmbeddings
 */
JNIEXPORT jfloatArray JNICALL Java_chat_octet_model_LlamaService_getEmbeddings
        (JNIEnv *env, jclass thisClass) {
    float *embeddings = llama_get_embeddings(llama_context_global);
    int embd_size = llama_n_embd(llama_context_global);

    jfloatArray arrays = env->NewFloatArray(embd_size);
    env->SetFloatArrayRegion(arrays, 0, embd_size, embeddings);

    return arrays;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenText
 */
JNIEXPORT jstring JNICALL Java_chat_octet_model_LlamaService_getTokenText
        (JNIEnv *env, jclass thisClass, jint token) {
    return ToJString(env, llama_token_get_text(llama_context_global, token));
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenScore
 */
JNIEXPORT jfloat JNICALL Java_chat_octet_model_LlamaService_getTokenScore
        (JNIEnv *env, jclass thisClass, jint token) {
    return llama_token_get_score(llama_context_global, token);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenType
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenType
        (JNIEnv *env, jclass thisClass, jint token) {
    return llama_token_get_type(llama_context_global, token);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenBOS
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenBOS
        (JNIEnv *env, jclass thisClass) {
    return llama_token_bos(llama_context_global);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenEOS
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenEOS
        (JNIEnv *env, jclass thisClass) {
    return llama_token_eos(llama_context_global);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenNL
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenNL
        (JNIEnv *env, jclass thisClass) {
    return llama_token_nl(llama_context_global);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    tokenize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_tokenize
        (JNIEnv *env, jclass thisClass, jbyteArray buf, jint textLength,
         jintArray tokensArrays,
         jint maxTokens, jboolean addBos) {
    llama_token *tokens = (llama_token *) env->GetIntArrayElements(tokensArrays, JNI_FALSE);

    jsize len = env->GetArrayLength(buf);
    jbyte *buffer = new jbyte[len];
    env->GetByteArrayRegion(buf, 0, len, buffer);
    char *text = (char *) buffer;

    int t = llama_tokenize(llama_context_global, text, textLength, tokens, maxTokens, ToCBool(addBos));
    env->ReleaseIntArrayElements(tokensArrays, tokens, 0);
    env->ReleaseByteArrayElements(buf, buffer, 0);

    return t;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    tokenizeWithModel
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_tokenizeWithModel
        (JNIEnv *env, jclass thisClass, jbyteArray buf, jint textLength,
         jintArray tokensArrays,
         jint maxTokens, jboolean addBos) {
    llama_token *tokens = (llama_token *) env->GetIntArrayElements(tokensArrays, JNI_FALSE);

    jsize len = env->GetArrayLength(buf);
    jbyte *buffer = new jbyte[len];
    env->GetByteArrayRegion(buf, 0, len, buffer);
    char *text = (char *) buffer;

    int t = llama_tokenize_with_model(llama_model_global, text, textLength, tokens, maxTokens, ToCBool(addBos));
    env->ReleaseIntArrayElements(tokensArrays, tokens, 0);
    env->ReleaseByteArrayElements(buf, buffer, 0);

    return t;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenToPiece
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenToPiece
        (JNIEnv *env, jclass thisClass, jint token, jbyteArray buf, jint length) {
    jsize len = env->GetArrayLength(buf);
    jbyte *buffer = new jbyte[len];

    int size = llama_token_to_piece(llama_context_global, token, (char *) buffer, length);
    env->ReleaseByteArrayElements(buf, buffer, 0);

    return size;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenToPieceWithModel
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenToPieceWithModel
        (JNIEnv *env, jclass thisClass, jint token, jbyteArray buf, jint length) {
    jsize len = env->GetArrayLength(buf);
    jbyte *buffer = new jbyte[len];

    int size = llama_token_to_piece_with_model(llama_model_global, token, (char *) buffer, length);
    env->ReleaseByteArrayElements(buf, buffer, 0);

    return size;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    printTimings
 */
JNIEXPORT void JNICALL Java_chat_octet_model_LlamaService_printTimings
        (JNIEnv *env, jclass thisClass) {
    llama_print_timings(llama_context_global);
    llama_reset_timings(llama_context_global);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    printSystemInfo
 */
JNIEXPORT jstring JNICALL Java_chat_octet_model_LlamaService_printSystemInfo
        (JNIEnv *env, jclass thisClass) {
    const char *system_info = llama_print_system_info();
    return env->NewStringUTF(system_info);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    sampling
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_sampling
        (JNIEnv *env,
         jclass thisClass,
         jfloatArray jLogits,
         jintArray lastTokensArray,
         jint lastTokensSize,
         jfloat penalty,
         jfloat alphaFrequency,
         jfloat alphaPresence,
         jboolean penalizeNL,
         jint mirostatMode,
         jfloat mirostatTAU,
         jfloat mirostatETA,
         jfloat temperature,
         jint topK,
         jfloat topP,
         jfloat tsf,
         jfloat typical) {
    //parse Java data types
    llama_token *lastTokens = (llama_token *) env->GetIntArrayElements(lastTokensArray, JNI_FALSE);

    //create token candidates
    float *logits = env->GetFloatArrayElements(jLogits, JNI_FALSE);

    int tokenNL = llama_token_nl(llama_context_global);
    float defaultNlLogit = logits[tokenNL];

    llama_token_data_array *candidates = new llama_token_data_array();

    int len = env->GetArrayLength(jLogits);
    candidates->data = new llama_token_data[len];
    candidates->size = len;
    candidates->sorted = false;

    for (int i = 0; i < len; ++i) {
        candidates->data[i].id = i;
        candidates->data[i].logit = logits[i];
        candidates->data[i].p = 0.0;
    }

    //repetition penalty
    llama_sample_repetition_penalty(llama_context_global, candidates, lastTokens, lastTokensSize, penalty);
    llama_sample_frequency_and_presence_penalties(llama_context_global, candidates, lastTokens, lastTokensSize,
                                                  alphaFrequency,
                                                  alphaPresence);

    if (!penalizeNL) {
        candidates->data[tokenNL].logit = defaultNlLogit;
    }

    int token;
    if (temperature == 0) {
        token = llama_sample_token_greedy(llama_context_global, candidates);
    } else {
        float mu = 2.0 * mirostatTAU;
        float *mirostatMu = &mu;
        if (mirostatMode == 1) {
            int mirostatM = 100;
            llama_sample_temperature(llama_context_global, candidates, temperature);
            token = llama_sample_token_mirostat(llama_context_global, candidates, mirostatTAU, mirostatETA, mirostatM,
                                                mirostatMu);
        } else if (mirostatMode == 2) {
            llama_sample_temperature(llama_context_global, candidates, temperature);
            token = llama_sample_token_mirostat_v2(llama_context_global, candidates, mirostatTAU, mirostatETA,
                                                   mirostatMu);
        } else {
            int top_k = topK <= 0 ? llama_n_vocab(llama_context_global) : topK;
            llama_sample_top_k(llama_context_global, candidates, top_k, 1);
            llama_sample_tail_free(llama_context_global, candidates, tsf, 1);
            llama_sample_typical(llama_context_global, candidates, typical, 1);
            llama_sample_top_p(llama_context_global, candidates, topP, 1);
            llama_sample_temperature(llama_context_global, candidates, temperature);
            token = llama_sample_token(llama_context_global, candidates);
        }
    }
    //clear all resources
    env->ReleaseIntArrayElements(lastTokensArray, lastTokens, 0);
    env->ReleaseFloatArrayElements(jLogits, logits, 0);

    return token;
}
