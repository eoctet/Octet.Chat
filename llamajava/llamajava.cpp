//
// Created by William W on 2023/9/13.
//
#include "llama.h"

#include "llamajava.h"

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
JNIEXPORT jobject JNICALL Java_chat_octet_model_LlamaService_loadLlamaModelFromFile
        (JNIEnv *env, jclass thisClass, jstring modelPath, jobject llamaContextParams) {
    struct llama_context_params params = GetLlamaContextParams(env, llamaContextParams);

    llama_model *model = llama_load_model_from_file(ToCString(env, modelPath), params);
    if (!model) {
        return nullptr;
    }
    jobject llama_model_pointer = NewObjectPointer(env, model);
    return llama_model_pointer;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    createNewContextWithModel
 */
JNIEXPORT jobject JNICALL Java_chat_octet_model_LlamaService_createNewContextWithModel
        (JNIEnv *env, jclass thisClass, jobject llama_model_pointer, jobject llamaContextParams) {
    llama_model *model = (llama_model *) GetObjectPointer(env, llama_model_pointer);

    llama_context_params params = GetLlamaContextParams(env, llamaContextParams);

    llama_context *context = llama_new_context_with_model(model, params);

    if (!context) {
        return nullptr;
    }
    jobject llama_context_pointer = NewObjectPointer(env, context);
    return llama_context_pointer;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    releaseLlamaModel
 */
JNIEXPORT void JNICALL Java_chat_octet_model_LlamaService_releaseLlamaModel
        (JNIEnv *env, jclass thisClass, jobject llama_model_pointer) {
    llama_model *model = (llama_model *) GetObjectPointer(env, llama_model_pointer);
    llama_free_model(model);
    env->DeleteLocalRef(llama_model_pointer);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    releaseLlamaContext
 */
JNIEXPORT void JNICALL Java_chat_octet_model_LlamaService_releaseLlamaContext
        (JNIEnv *env, jclass thisClass, jobject llama_context_pointer) {
    llama_context *context = (llama_context *) GetObjectPointer(env, llama_context_pointer);
    llama_free(context);
    env->DeleteLocalRef(llama_context_pointer);
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
        (JNIEnv *env, jclass thisClass, jobject llama_context_pointer) {
    llama_context *context = (llama_context *) GetObjectPointer(env, llama_context_pointer);
    return llama_n_vocab(context);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getContextSize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getContextSize
        (JNIEnv *env, jclass thisClass, jobject llama_context_pointer) {
    llama_context *context = (llama_context *) GetObjectPointer(env, llama_context_pointer);
    return llama_n_ctx(context);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getEmbeddingSize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getEmbeddingSize
        (JNIEnv *env, jclass thisClass, jobject llama_context_pointer) {
    llama_context *context = (llama_context *) GetObjectPointer(env, llama_context_pointer);
    return llama_n_embd(context);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getVocabType
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getVocabType
        (JNIEnv *env, jclass thisClass, jobject llama_context_pointer) {
    llama_context *context = (llama_context *) GetObjectPointer(env, llama_context_pointer);
    return llama_vocab_type(context);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getModelVocabSize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getModelVocabSize
        (JNIEnv *env, jclass thisClass, jobject llama_model_pointer) {
    llama_model *model = (llama_model *) GetObjectPointer(env, llama_model_pointer);
    return llama_model_n_vocab(model);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getModelContextSize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getModelContextSize
        (JNIEnv *env, jclass thisClass, jobject llama_model_pointer) {
    llama_model *model = (llama_model *) GetObjectPointer(env, llama_model_pointer);
    return llama_model_n_ctx(model);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getModelEmbeddingSize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getModelEmbeddingSize
        (JNIEnv *env, jclass thisClass, jobject llama_model_pointer) {
    llama_model *model = (llama_model *) GetObjectPointer(env, llama_model_pointer);
    return llama_model_n_embd(model);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    loadLoraModelFromFile
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_loadLoraModelFromFile
        (JNIEnv *env, jclass thisClass, jobject llama_model_pointer, jstring loraPath, jstring baseModelPath,
         jint threads) {
    llama_model *model = (llama_model *) GetObjectPointer(env, llama_model_pointer);
    return llama_model_apply_lora_from_file(model, ToCString(env, loraPath), ToCString(env, baseModelPath),
                                            threads);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    evaluate
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_evaluate
        (JNIEnv *env, jclass thisClass, jobject llama_context_pointer, jintArray tokensArrays, jint nTokens, jint nPast,
         jint threads) {
    llama_context *context = (llama_context *) GetObjectPointer(env, llama_context_pointer);
    llama_token *tokens = (llama_token *) env->GetIntArrayElements(tokensArrays, JNI_FALSE);

    int code = llama_eval(context, tokens, nTokens, nPast, threads);
    env->ReleaseIntArrayElements(tokensArrays, tokens, 0);

    return code;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getLogits
 */
JNIEXPORT jfloatArray JNICALL Java_chat_octet_model_LlamaService_getLogits
        (JNIEnv *env, jclass thisClass, jobject llama_context_pointer) {
    llama_context *context = (llama_context *) GetObjectPointer(env, llama_context_pointer);

    float *logits = llama_get_logits(context);
    int vocab_size = llama_n_vocab(context);

    jfloatArray arrays = env->NewFloatArray(vocab_size);
    env->SetFloatArrayRegion(arrays, 0, vocab_size, logits);

    return arrays;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getEmbeddings
 */
JNIEXPORT jfloatArray JNICALL Java_chat_octet_model_LlamaService_getEmbeddings
        (JNIEnv *env, jclass thisClass, jobject llama_context_pointer) {
    llama_context *context = (llama_context *) GetObjectPointer(env, llama_context_pointer);

    float *embeddings = llama_get_embeddings(context);
    int embd_size = llama_n_embd(context);

    jfloatArray arrays = env->NewFloatArray(embd_size);
    env->SetFloatArrayRegion(arrays, 0, embd_size, embeddings);

    return arrays;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenText
 */
JNIEXPORT jstring JNICALL Java_chat_octet_model_LlamaService_getTokenText
        (JNIEnv *env, jclass thisClass, jobject llama_context_pointer, jint token) {
    llama_context *context = (llama_context *) GetObjectPointer(env, llama_context_pointer);
    jstring result = ToJString(env, llama_token_get_text(context, token));
    return result;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenScore
 */
JNIEXPORT jfloat JNICALL Java_chat_octet_model_LlamaService_getTokenScore
        (JNIEnv *env, jclass thisClass, jobject llama_context_pointer, jint token) {
    llama_context *context = (llama_context *) GetObjectPointer(env, llama_context_pointer);
    return llama_token_get_score(context, token);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenType
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenType
        (JNIEnv *env, jclass thisClass, jobject llama_context_pointer, jint token) {
    llama_context *context = (llama_context *) GetObjectPointer(env, llama_context_pointer);
    return llama_token_get_type(context, token);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenBOS
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenBOS
        (JNIEnv *env, jclass thisClass, jobject llama_context_pointer) {
    llama_context *context = (llama_context *) GetObjectPointer(env, llama_context_pointer);
    return llama_token_bos(context);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenEOS
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenEOS
        (JNIEnv *env, jclass thisClass, jobject llama_context_pointer) {
    llama_context *context = (llama_context *) GetObjectPointer(env, llama_context_pointer);
    return llama_token_eos(context);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenNL
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenNL
        (JNIEnv *env, jclass thisClass, jobject llama_context_pointer) {
    llama_context *context = (llama_context *) GetObjectPointer(env, llama_context_pointer);
    return llama_token_nl(context);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    tokenize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_tokenize
        (JNIEnv *env, jclass thisClass, jobject llama_context_pointer, jbyteArray buf, jint textLength, jintArray tokensArrays,
         jint maxTokens, jboolean addBos) {
    llama_context *context = (llama_context *) GetObjectPointer(env, llama_context_pointer);
    llama_token *tokens = (llama_token *) env->GetIntArrayElements(tokensArrays, JNI_FALSE);

    jsize len = env->GetArrayLength(buf);
    jbyte *buffer = new jbyte[len];
    env->GetByteArrayRegion(buf, 0, len, buffer);
    char * text = (char *) buffer;
    
    int t = llama_tokenize(context, text, textLength, tokens, maxTokens, ToCBool(addBos));
    env->ReleaseIntArrayElements(tokensArrays, tokens, 0);
    env->ReleaseByteArrayElements(buf, buffer, 0);

    return t;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    tokenizeWithModel
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_tokenizeWithModel
        (JNIEnv *env, jclass thisClass, jobject llama_model_pointer, jbyteArray buf, jint textLength, jintArray tokensArrays,
         jint maxTokens, jboolean addBos) {
    llama_model *model = (llama_model *) GetObjectPointer(env, llama_model_pointer);
    llama_token *tokens = (llama_token *) env->GetIntArrayElements(tokensArrays, JNI_FALSE);

    jsize len = env->GetArrayLength(buf);
    jbyte *buffer = new jbyte[len];
    env->GetByteArrayRegion(buf, 0, len, buffer);
    char * text = (char *) buffer;

    int t = llama_tokenize_with_model(model, text, textLength, tokens, maxTokens, ToCBool(addBos));
    env->ReleaseIntArrayElements(tokensArrays, tokens, 0);
    env->ReleaseByteArrayElements(buf, buffer, 0);

    return t;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenToPiece
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenToPiece
        (JNIEnv *env, jclass thisClass, jobject llama_context_pointer, jint token, jbyteArray buf, jint length) {
    llama_context *context = (llama_context *) GetObjectPointer(env, llama_context_pointer);

    jsize len = env->GetArrayLength(buf);
    jbyte *buffer = new jbyte[len];

    int size = llama_token_to_piece(context, token, (char *) buffer, length);
    env->ReleaseByteArrayElements(buf, buffer, 0);

    return size;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenToPieceWithModel
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenToPieceWithModel
        (JNIEnv *env, jclass thisClass, jobject llama_model_pointer, jint token, jbyteArray buf, jint length) {
    llama_model *model = (llama_model *) GetObjectPointer(env, llama_model_pointer);

    jsize len = env->GetArrayLength(buf);
    jbyte *buffer = new jbyte[len];

    int size = llama_token_to_piece_with_model(model, token, (char *) buffer, length);
    env->ReleaseByteArrayElements(buf, buffer, 0);

    return size;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    printTimings
 */
JNIEXPORT void JNICALL Java_chat_octet_model_LlamaService_printTimings
        (JNIEnv *env, jclass thisClass, jobject llama_context_pointer) {
    llama_context *context = (llama_context *) GetObjectPointer(env, llama_context_pointer);
    llama_print_timings(context);
    llama_reset_timings(context);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    printSystemInfo
 */
JNIEXPORT jstring JNICALL Java_chat_octet_model_LlamaService_printSystemInfo
        (JNIEnv *env, jclass thisClass) {
    const char *system_info = llama_print_system_info();
    jstring info = env->NewStringUTF(system_info);
    return info;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    samplingWithGreedy
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_samplingWithGreedy
        (JNIEnv *env,
         jclass thisClass,
         jobject llama_context_pointer,
         jfloatArray jLogits,
         jintArray lastTokensArray,
         jint lastTokensSize,
         jfloat penalty,
         jfloat alphaFrequency,
         jfloat alphaPresence,
         jboolean penalizeNL) {
    //parse Java data types
    llama_context *context = (llama_context *) GetObjectPointer(env, llama_context_pointer);
    llama_token *lastTokens = (llama_token *) env->GetIntArrayElements(lastTokensArray, JNI_FALSE);

    //create token candidates
    float *logits = env->GetFloatArrayElements(jLogits, JNI_FALSE);

    int tokenNL = llama_token_nl(context);
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

    //step 1: repetition_penalty
    llama_sample_repetition_penalty(context, candidates, lastTokens, lastTokensSize, penalty);

    //step 2: frequency_and_presence_penalties
    llama_sample_frequency_and_presence_penalties(context, candidates, lastTokens, lastTokensSize, alphaFrequency,
                                                  alphaPresence);

    //step 3: process penalize_nl
    if(!penalizeNL) {
        candidates->data[tokenNL].logit = defaultNlLogit;
    }

    //step 4: llama_sample_token_greedy
    int token = llama_sample_token_greedy(context, candidates);

    //clear all resources
    env->ReleaseIntArrayElements(lastTokensArray, lastTokens, 0);
    env->ReleaseFloatArrayElements(jLogits, logits, 0);

    return token;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    samplingWithMirostatV1
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_samplingWithMirostatV1
        (JNIEnv *env,
         jclass thisClass,
         jobject llama_context_pointer,
         jfloatArray jLogits,
         jintArray lastTokensArray,
         jint lastTokensSize,
         jfloat penalty,
         jfloat alphaFrequency,
         jfloat alphaPresence,
         jboolean penalizeNL,
         jfloat temperature,
         jfloat mirostatTAU,
         jfloat mirostatETA,
         jint mirostatM,
         jobject mirostatMu) {
    //parse Java data types
    llama_context *context = (llama_context *) GetObjectPointer(env, llama_context_pointer);
    llama_token *lastTokens = (llama_token *) env->GetIntArrayElements(lastTokensArray, JNI_FALSE);

    //create token candidates
    float *logits = env->GetFloatArrayElements(jLogits, JNI_FALSE);

    int tokenNL = llama_token_nl(context);
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

    //step 1: repetition_penalty
    llama_sample_repetition_penalty(context, candidates, lastTokens, lastTokensSize, penalty);

    //step 2: frequency_and_presence_penalties
    llama_sample_frequency_and_presence_penalties(context, candidates, lastTokens, lastTokensSize, alphaFrequency,
                                                  alphaPresence);

    //step 3: process penalize_nl
    if(!penalizeNL) {
        candidates->data[tokenNL].logit = defaultNlLogit;
    }

    //step 4: llama_sample_temperature
    llama_sample_temperature(context, candidates, temperature);

    //step 5: llama_sample_token_mirostat
    int token = llama_sample_token_mirostat(context, candidates, mirostatTAU, mirostatETA, mirostatM,
                                            (float *) mirostatMu);

    //clear all resources
    env->ReleaseIntArrayElements(lastTokensArray, lastTokens, 0);
    env->ReleaseFloatArrayElements(jLogits, logits, 0);

    return token;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    samplingWithMirostatV2
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_samplingWithMirostatV2
        (JNIEnv *env,
         jclass thisClass,
         jobject llama_context_pointer,
         jfloatArray jLogits,
         jintArray lastTokensArray,
         jint lastTokensSize,
         jfloat penalty,
         jfloat alphaFrequency,
         jfloat alphaPresence,
         jboolean penalizeNL,
         jfloat temperature,
         jfloat mirostatTAU,
         jfloat mirostatETA,
         jobject mirostatMu) {
    //parse Java data types
    llama_context *context = (llama_context *) GetObjectPointer(env, llama_context_pointer);
    llama_token *lastTokens = (llama_token *) env->GetIntArrayElements(lastTokensArray, JNI_FALSE);

    //create token candidates
    float *logits = env->GetFloatArrayElements(jLogits, JNI_FALSE);

    int tokenNL = llama_token_nl(context);
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

    //step 1: repetition_penalty
    llama_sample_repetition_penalty(context, candidates, lastTokens, lastTokensSize, penalty);

    //step 2: frequency_and_presence_penalties
    llama_sample_frequency_and_presence_penalties(context, candidates, lastTokens, lastTokensSize, alphaFrequency,
                                                  alphaPresence);

    //step 3: process penalize_nl
    if(!penalizeNL) {
        candidates->data[tokenNL].logit = defaultNlLogit;
    }

    //step 4: llama_sample_temperature
    llama_sample_temperature(context, candidates, temperature);

    //step 5: llama_sample_token_mirostat_v2
    int token = llama_sample_token_mirostat_v2(context, candidates, mirostatTAU, mirostatETA, (float *) mirostatMu);

    //clear all resources
    env->ReleaseIntArrayElements(lastTokensArray, lastTokens, 0);
    env->ReleaseFloatArrayElements(jLogits, logits, 0);

    return token;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    sampling
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_sampling
        (JNIEnv *env,
         jclass thisClass,
         jobject llama_context_pointer,
         jfloatArray jLogits,
         jintArray lastTokensArray,
         jint lastTokensSize,
         jfloat penalty,
         jfloat alphaFrequency,
         jfloat alphaPresence,
         jboolean penalizeNL,
         jfloat temperature,
         jint topK,
         jfloat topP,
         jfloat tsf,
         jfloat typicalP,
         jint minKeep) {
    //parse Java data types
    llama_context *context = (llama_context *) GetObjectPointer(env, llama_context_pointer);
    llama_token *lastTokens = (llama_token *) env->GetIntArrayElements(lastTokensArray, JNI_FALSE);

    //create token candidates
    float *logits = env->GetFloatArrayElements(jLogits, JNI_FALSE);

    int tokenNL = llama_token_nl(context);
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

    //step 1: repetition_penalty
    llama_sample_repetition_penalty(context, candidates, lastTokens, lastTokensSize, penalty);

    //step 2: frequency_and_presence_penalties
    llama_sample_frequency_and_presence_penalties(context, candidates, lastTokens, lastTokensSize, alphaFrequency,
                                                  alphaPresence);

    //step 3: process penalize_nl
    if(!penalizeNL) {
        candidates->data[tokenNL].logit = defaultNlLogit;
    }

    //step 4: llama_sample_top_k
    llama_sample_top_k(context, candidates, topK, minKeep);

    //step 5: llama_sample_tail_free
    llama_sample_tail_free(context, candidates, tsf, minKeep);

    //step 6: llama_sample_typical
    llama_sample_typical(context, candidates, typicalP, minKeep);

    //step 7: llama_sample_top_p
    llama_sample_top_p(context, candidates, topP, minKeep);

    //step 8: llama_sample_temperature
    llama_sample_temperature(context, candidates, temperature);

    //step 9: llama_sample_token
    int token = llama_sample_token(context, candidates);

    //clear all resources
    env->ReleaseIntArrayElements(lastTokensArray, lastTokens, 0);
    env->ReleaseFloatArrayElements(jLogits, logits, 0);

    return token;
}
