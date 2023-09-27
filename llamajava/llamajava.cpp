//
// Created by William W on 2023/9/13.
//
#include "llama.h"
#include "llamajava.h"
#include <vector>

//Global ref
llama_model *model = nullptr;
llama_context *llama_ctx = nullptr;

static bool init_native = false;

//Class ModelException:
jclass MODEL_EXCEPTION;

//Class LlamaContextParams:
jclass LLAMA_CONTEXT_PARAMS_CLASS;
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

//Class Metrics
jclass METRICS_CLASS;
jfieldID FIELD_START_TIME_MS;
jfieldID FIELD_END_TIME_MS;
jfieldID FIELD_LOAD_TIME_MS;
jfieldID FIELD_SAMPLE_TIME_MS;
jfieldID FIELD_PROMPT_EVAL_TIME_MS;
jfieldID FIELD_EVAL_TIME_MS;
jfieldID FIELD_SAMPLE_COUNT;
jfieldID FIELD_PROMPT_EVAL_COUNT;
jfieldID FIELD_EVAL_COUNT;

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

jobject GetLlamaTimingsToMetrics(JNIEnv *env, struct llama_timings timings) {
    jclass metrics_class = env->FindClass("chat/octet/model/beans/Metrics");
    jmethodID method_init = env->GetMethodID(metrics_class, "<init>", "()V");
    jobject jMetrics = env->NewObject(metrics_class, method_init);

    env->SetDoubleField(jMetrics, FIELD_START_TIME_MS, timings.t_start_ms);
    env->SetDoubleField(jMetrics, FIELD_END_TIME_MS, timings.t_end_ms);
    env->SetDoubleField(jMetrics, FIELD_LOAD_TIME_MS, timings.t_load_ms);
    env->SetDoubleField(jMetrics, FIELD_SAMPLE_TIME_MS, timings.t_sample_ms);
    env->SetDoubleField(jMetrics, FIELD_PROMPT_EVAL_TIME_MS, timings.t_p_eval_ms);
    env->SetDoubleField(jMetrics, FIELD_EVAL_TIME_MS, timings.t_eval_ms);
    env->SetIntField(jMetrics, FIELD_SAMPLE_COUNT, timings.n_sample);
    env->SetIntField(jMetrics, FIELD_PROMPT_EVAL_COUNT, timings.n_p_eval);
    env->SetIntField(jMetrics, FIELD_EVAL_COUNT, timings.n_eval);
    return jMetrics;
}

/*
* Class:     chat_octet_model_LlamaService
* Method:    initNative
*/
JNIEXPORT void JNICALL Java_chat_octet_model_LlamaService_initNative
        (JNIEnv *env, jclass thisClass) {
    if (init_native) {
        return;
    }

    //Class ModelException
    MODEL_EXCEPTION = env->FindClass("chat/octet/model/exceptions/ModelException");

    //Class LlamaContextParams
    LLAMA_CONTEXT_PARAMS_CLASS = env->FindClass("chat/octet/model/beans/LlamaContextParams");
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

    //Class Metrics
    METRICS_CLASS = env->FindClass("chat/octet/model/beans/Metrics");
    FIELD_START_TIME_MS = env->GetFieldID(METRICS_CLASS, "startTimeMs", "D");
    FIELD_END_TIME_MS = env->GetFieldID(METRICS_CLASS, "endTimeMs", "D");
    FIELD_LOAD_TIME_MS = env->GetFieldID(METRICS_CLASS, "loadTimeMs", "D");
    FIELD_SAMPLE_TIME_MS = env->GetFieldID(METRICS_CLASS, "sampleTimeMs", "D");
    FIELD_PROMPT_EVAL_TIME_MS = env->GetFieldID(METRICS_CLASS, "promptEvalTimeMs", "D");
    FIELD_EVAL_TIME_MS = env->GetFieldID(METRICS_CLASS, "evalTimeMs", "D");
    FIELD_SAMPLE_COUNT = env->GetFieldID(METRICS_CLASS, "sampleCount", "I");
    FIELD_PROMPT_EVAL_COUNT = env->GetFieldID(METRICS_CLASS, "promptEvalCount", "I");
    FIELD_EVAL_COUNT = env->GetFieldID(METRICS_CLASS, "evalCount", "I");

    init_native = true;
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
    model = llama_load_model_from_file(ToCString(env, modelPath), params);

    if (!model) {
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
    llama_ctx = llama_new_context_with_model(model, params);

    if (!llama_ctx) {
        env->ThrowNew(MODEL_EXCEPTION, "Create llama context failed");
    }
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    release
 */
JNIEXPORT void JNICALL Java_chat_octet_model_LlamaService_release
        (JNIEnv *env, jclass thisClass) {
    if (model) {
        llama_free_model(model);
        model = nullptr;
    }
    if (llama_ctx) {
        llama_free(llama_ctx);
        llama_ctx = nullptr;
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
    return llama_n_vocab(llama_ctx);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getContextSize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getContextSize
        (JNIEnv *env, jclass thisClass) {
    return llama_n_ctx(llama_ctx);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getEmbeddingSize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getEmbeddingSize
        (JNIEnv *env, jclass thisClass) {
    return llama_n_embd(llama_ctx);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getVocabType
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getVocabType
        (JNIEnv *env, jclass thisClass) {
    return llama_vocab_type(llama_ctx);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getModelVocabSize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getModelVocabSize
        (JNIEnv *env, jclass thisClass) {
    return llama_model_n_vocab(model);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getModelContextSize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getModelContextSize
        (JNIEnv *env, jclass thisClass) {
    return llama_model_n_ctx(model);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getModelEmbeddingSize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getModelEmbeddingSize
        (JNIEnv *env, jclass thisClass) {
    return llama_model_n_embd(model);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    loadLoraModelFromFile
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_loadLoraModelFromFile
        (JNIEnv *env, jclass thisClass, jstring loraPath, jstring baseModelPath,
         jint threads) {
    if (!model) {
        env->ThrowNew(MODEL_EXCEPTION, "llama model cannot be null");
        return -1;
    }
    return llama_model_apply_lora_from_file(model, ToCString(env, loraPath), ToCString(env, baseModelPath),
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

    int code = llama_eval(llama_ctx, tokens, nTokens, nPast, threads);
    env->ReleaseIntArrayElements(tokensArrays, tokens, 0);
    return code;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getLogits
 */
JNIEXPORT jfloatArray JNICALL Java_chat_octet_model_LlamaService_getLogits
        (JNIEnv *env, jclass thisClass) {
    float *logits = llama_get_logits(llama_ctx);
    const int vocab_size = llama_n_vocab(llama_ctx);

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
    float *embeddings = llama_get_embeddings(llama_ctx);
    const int embd_size = llama_n_embd(llama_ctx);

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
    return ToJString(env, llama_token_get_text(llama_ctx, token));
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenScore
 */
JNIEXPORT jfloat JNICALL Java_chat_octet_model_LlamaService_getTokenScore
        (JNIEnv *env, jclass thisClass, jint token) {
    return llama_token_get_score(llama_ctx, token);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenType
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenType
        (JNIEnv *env, jclass thisClass, jint token) {
    return llama_token_get_type(llama_ctx, token);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenBOS
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenBOS
        (JNIEnv *env, jclass thisClass) {
    return llama_token_bos(llama_ctx);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenEOS
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenEOS
        (JNIEnv *env, jclass thisClass) {
    return llama_token_eos(llama_ctx);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenNL
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenNL
        (JNIEnv *env, jclass thisClass) {
    return llama_token_nl(llama_ctx);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    tokenize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_tokenize
        (JNIEnv *env, jclass thisClass, jbyteArray buf, jint bufferLength,
         jintArray tokensArrays,
         jint maxTokens, jboolean addBos) {
    llama_token *tokens = (llama_token *) env->GetIntArrayElements(tokensArrays, JNI_FALSE);

    jbyte *buffer = new jbyte[bufferLength];
    env->GetByteArrayRegion(buf, 0, bufferLength, buffer);
    const char *text = (char *) buffer;

    int code = llama_tokenize(llama_ctx, text, bufferLength, tokens, maxTokens, ToCBool(addBos));
    env->ReleaseIntArrayElements(tokensArrays, tokens, 0);
    env->ReleaseByteArrayElements(buf, buffer, 0);
    return code;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    tokenizeWithModel
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_tokenizeWithModel
        (JNIEnv *env, jclass thisClass, jbyteArray buf, jint bufferLength,
         jintArray tokensArrays,
         jint maxTokens, jboolean addBos) {
    llama_token *tokens = (llama_token *) env->GetIntArrayElements(tokensArrays, JNI_FALSE);

    jbyte *buffer = new jbyte[bufferLength];
    env->GetByteArrayRegion(buf, 0, bufferLength, buffer);
    const char *text = (char *) buffer;

    int code = llama_tokenize_with_model(model, text, bufferLength, tokens, maxTokens, ToCBool(addBos));
    env->ReleaseIntArrayElements(tokensArrays, tokens, 0);
    env->ReleaseByteArrayElements(buf, buffer, 0);
    return code;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenToPiece
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenToPiece
        (JNIEnv *env, jclass thisClass, jint token, jbyteArray buf, jint bufferLength) {
    jbyte *buffer = new jbyte[bufferLength];
    int size = llama_token_to_piece(llama_ctx, token, (char *) buffer, bufferLength);
    env->ReleaseByteArrayElements(buf, buffer, 0);
    return size;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenToPieceWithModel
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenToPieceWithModel
        (JNIEnv *env, jclass thisClass, jint token, jbyteArray buf, jint bufferLength) {
    jbyte *buffer = new jbyte[bufferLength];
    int size = llama_token_to_piece_with_model(model, token, (char *) buffer, bufferLength);
    env->ReleaseByteArrayElements(buf, buffer, 0);
    return size;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getSamplingMetrics
 */
JNIEXPORT jobject JNICALL Java_chat_octet_model_LlamaService_getSamplingMetrics
        (JNIEnv *env, jclass thisClass, jboolean reset) {
    struct llama_timings timings = llama_get_timings(llama_ctx);
    jobject jMetrics = GetLlamaTimingsToMetrics(env, timings);
    if (reset) {
        llama_reset_timings(llama_ctx);
    }
    return jMetrics;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getSystemInfo
 */
JNIEXPORT jstring JNICALL Java_chat_octet_model_LlamaService_getSystemInfo
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

    float *logits = env->GetFloatArrayElements(jLogits, JNI_FALSE);
    const int n_vocab = llama_n_vocab(llama_ctx);
    const int token_nl = llama_token_nl(llama_ctx);
    const float nl_logit = logits[token_nl];
    const int32_t top_k = topK <= 0 ? n_vocab : topK;

    std::vector<llama_token_data> candidates;
    candidates.reserve(n_vocab);
    for (llama_token token_id = 0; token_id < n_vocab; token_id++) {
        llama_token_data data = {token_id, logits[token_id], 0.0f};
        candidates.emplace_back(data);
    }
    llama_token_data_array candidates_p = {candidates.data(), candidates.size(), false};

    llama_token *lastTokens = (llama_token *) env->GetIntArrayElements(lastTokensArray, JNI_FALSE);

    //repetition penalty
    llama_sample_repetition_penalty(llama_ctx, &candidates_p, lastTokens, lastTokensSize, penalty);
    llama_sample_frequency_and_presence_penalties(llama_ctx, &candidates_p, lastTokens, lastTokensSize,
                                                  alphaFrequency,
                                                  alphaPresence);

    if (!penalizeNL) {
        candidates_p.data[token_nl].logit = nl_logit;
    }

    llama_token token;
    if (temperature <= 0) {
        token = llama_sample_token_greedy(llama_ctx, &candidates_p);
    } else {
        if (mirostatMode == 1) {
            const int mirostatM = 100;
            static float mirostatMu = 2.0f * mirostatTAU;
            llama_sample_temperature(llama_ctx, &candidates_p, temperature);
            token = llama_sample_token_mirostat(llama_ctx, &candidates_p, mirostatTAU, mirostatETA, mirostatM,
                                                &mirostatMu);
        } else if (mirostatMode == 2) {
            static float mirostatMu = 2.0f * mirostatTAU;
            llama_sample_temperature(llama_ctx, &candidates_p, temperature);
            token = llama_sample_token_mirostat_v2(llama_ctx, &candidates_p, mirostatTAU, mirostatETA,
                                                   &mirostatMu);
        } else {
            llama_sample_top_k(llama_ctx, &candidates_p, top_k, 1);
            llama_sample_tail_free(llama_ctx, &candidates_p, tsf, 1);
            llama_sample_typical(llama_ctx, &candidates_p, typical, 1);
            llama_sample_top_p(llama_ctx, &candidates_p, topP, 1);
            llama_sample_temperature(llama_ctx, &candidates_p, temperature);
            token = llama_sample_token(llama_ctx, &candidates_p);
        }
    }
    //clear all resources
    env->ReleaseIntArrayElements(lastTokensArray, lastTokens, 0);
    env->ReleaseFloatArrayElements(jLogits, logits, 0);
    return token;
}
