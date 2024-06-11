//
// Created by William W on 2023/9/13.
//
#include "llama.h"
#include "llamajava.h"
#include "common/grammar-parser.h"
#include <vector>
#include <string>
#include <iostream>

//Global define
llama_model *model = nullptr;
llama_context *llama_ctx = nullptr;

llama_context_params llama_ctx_params;

//Grammar
llama_grammar *grammar = nullptr;

//JNI init native status
static bool init_native = false;

//Class name
static const char *model_exception = "chat/octet/model/exceptions/ModelException";
static const char *decode_exception = "chat/octet/model/exceptions/DecodeException";

//Class LlamaContextParams:
jclass LLAMA_CONTEXT_PARAMS_CLASS;
jfieldID FIELD_SEED;
jfieldID FIELD_CTX;
jfieldID FIELD_BATCH;
jfieldID FIELD_UBATCH;
jfieldID FIELD_SEQ_MAX;
jfieldID FIELD_THREADS;
jfieldID FIELD_THREADS_BATCH;
jfieldID FIELD_ROPE_SCALING_TYPE;
jfieldID FIELD_POOLING_TYPE;
jfieldID FIELD_YARN_EXT_FACTOR;
jfieldID FIELD_YARN_ATTN_FACTOR;
jfieldID FIELD_YARN_BETA_FAST;
jfieldID FIELD_YARN_BETA_SLOW;
jfieldID FIELD_YARN_ORIG_CTX;
jfieldID FIELD_DEFRAG_THOLD;
jfieldID FIELD_ROPE_FREQ_BASE;
jfieldID FIELD_ROPE_FREQ_SCALE;
jfieldID FIELD_DATA_TYPE_K;
jfieldID FIELD_DATA_TYPE_V;
jfieldID FIELD_LOGITS_ALL;
jfieldID FIELD_EMBEDDING;
jfieldID FIELD_OFFLOAD_KQV;
jfieldID FIELD_FLASH_ATTN;
jfieldID FIELD_ABORT_CALLBACK;
jfieldID FIELD_ABORT_CALLBACK_DATA;

//Class LlamaModelParams:
jclass LLAMA_MODEL_PARAMS_CLASS;
jfieldID FIELD_GPU_LAYERS;
jfieldID FIELD_SPLIT_MODE;
jfieldID FIELD_MAIN_GPU;
jfieldID FIELD_TENSOR_SPLIT;
jfieldID FIELD_VOCAB_ONLY;
jfieldID FIELD_USE_MMAP;
jfieldID FIELD_USE_MLOCK;
jfieldID FIELD_CHECK_TENSORS;

//Class LlamaModelQuantizeParams:
jclass LLAMA_MODEL_QUANTIZE_PARAMS_CLASS;
jfieldID FIELD_THREAD;
jfieldID FIELD_MODEL_FILE_TYPE;
jfieldID FIELD_OUTPUT_TENSOR_TYPE;
jfieldID FIELD_TOKEN_EMBEDDING_TYPE;
jfieldID FIELD_ALLOW_REQUANTIZE;
jfieldID FIELD_QUANTIZE_OUTPUT_TENSOR;
jfieldID FIELD_ONLY_COPY;
jfieldID FIELD_PURE;
jfieldID FIELD_KEEP_SPLIT;
jfieldID FIELD_IMATRIX;
jfieldID FIELD_KV_OVERRIDES;

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

static jstring ToJString(JNIEnv *env, const char *value) {
    return env->NewStringUTF(value);
}

static const char *ToCString(JNIEnv *env, jstring value) {
    return env->GetStringUTFChars(value, JNI_FALSE);
}

static jboolean ToJBoolean(bool value) {
    return value ? JNI_TRUE : JNI_FALSE;
}

static bool ToCBool(jboolean value) {
    return value == JNI_TRUE;
}

static void ThrowByName(JNIEnv *env, const char *name, const char *msg) {
    jclass cls = env->FindClass(name);
    if (cls) {
        env->ThrowNew(cls, msg);
    }
    env->DeleteLocalRef(cls);
}

static struct llama_model_params GetLlamaModelParams(JNIEnv *env, jobject jllama_model_params) {
    float *tensor_split = nullptr;
    jfloatArray arrays_data = (jfloatArray) env->GetObjectField(jllama_model_params, FIELD_TENSOR_SPLIT);
    if (arrays_data != nullptr) {
        tensor_split = env->GetFloatArrayElements(arrays_data, JNI_FALSE);
    }

    struct llama_model_params params = {
            /*.n_gpu_layers                =*/ env->GetIntField(jllama_model_params, FIELD_GPU_LAYERS),
            /*.split_mode                  =*/ static_cast<enum llama_split_mode>(env->GetIntField(jllama_model_params, FIELD_SPLIT_MODE)),
            /*.main_gpu                    =*/ env->GetIntField(jllama_model_params, FIELD_MAIN_GPU),
            /*.tensor_split                =*/ tensor_split,
            /*.rpc_servers                 =*/ nullptr,
            /*.progress_callback           =*/ nullptr,
            /*.progress_callback_user_data =*/ nullptr,
            /*.kv_overrides                =*/ nullptr,
            /*.vocab_only                  =*/ ToCBool(env->GetBooleanField(jllama_model_params, FIELD_VOCAB_ONLY)),
            /*.use_mmap                    =*/ ToCBool(env->GetBooleanField(jllama_model_params, FIELD_USE_MMAP)),
            /*.use_mlock                   =*/ ToCBool(env->GetBooleanField(jllama_model_params, FIELD_USE_MLOCK)),
            /*.check_tensors               =*/ ToCBool(env->GetBooleanField(jllama_model_params, FIELD_CHECK_TENSORS)),
    };
    if (arrays_data != nullptr) {
        env->ReleaseFloatArrayElements(arrays_data, tensor_split, 0);
    }
    return params;
}

static struct llama_context_params GetLlamaContextParams(JNIEnv *env, jobject jllama_context_params) {
    struct llama_context_params params = {
            /*.seed                        =*/ (uint32_t) env->GetIntField(jllama_context_params, FIELD_SEED),
            /*.n_ctx                       =*/ (uint32_t) env->GetIntField(jllama_context_params, FIELD_CTX),
            /*.n_batch                     =*/ (uint32_t) env->GetIntField(jllama_context_params, FIELD_BATCH),
            /*.n_ubatch                    =*/ (uint32_t) env->GetIntField(jllama_context_params, FIELD_UBATCH),
            /*.n_seq_max                   =*/ (uint32_t) env->GetIntField(jllama_context_params, FIELD_SEQ_MAX),
            /*.n_threads                   =*/ (uint32_t) env->GetIntField(jllama_context_params, FIELD_THREADS),
            /*.n_threads_batch             =*/ (uint32_t) env->GetIntField(jllama_context_params, FIELD_THREADS_BATCH),
            /*.rope_scaling_type           =*/ static_cast<enum llama_rope_scaling_type>(env->GetIntField(jllama_context_params, FIELD_ROPE_SCALING_TYPE)),
            /*.pooling_type                =*/ static_cast<enum llama_pooling_type>(env->GetIntField(jllama_context_params, FIELD_POOLING_TYPE)),
            /*.rope_freq_base              =*/ env->GetFloatField(jllama_context_params, FIELD_ROPE_FREQ_BASE),
            /*.rope_freq_scale             =*/ env->GetFloatField(jllama_context_params, FIELD_ROPE_FREQ_SCALE),
            /*.yarn_ext_factor             =*/ env->GetFloatField(jllama_context_params, FIELD_YARN_EXT_FACTOR),
            /*.yarn_attn_factor            =*/ env->GetFloatField(jllama_context_params, FIELD_YARN_ATTN_FACTOR),
            /*.yarn_beta_fast              =*/ env->GetFloatField(jllama_context_params, FIELD_YARN_BETA_FAST),
            /*.yarn_beta_slow              =*/ env->GetFloatField(jllama_context_params, FIELD_YARN_BETA_SLOW),
            /*.yarn_orig_ctx               =*/ (uint32_t) env->GetIntField(jllama_context_params, FIELD_YARN_ORIG_CTX),
            /*.defrag_thold                =*/ env->GetFloatField(jllama_context_params, FIELD_DEFRAG_THOLD),
            /*.cb_eval                     =*/ nullptr,
            /*.cb_eval_user_data           =*/ nullptr,
            /*.type_k                      =*/ static_cast<enum ggml_type>(env->GetIntField(jllama_context_params, FIELD_DATA_TYPE_K)),
            /*.type_v                      =*/ static_cast<enum ggml_type>(env->GetIntField(jllama_context_params, FIELD_DATA_TYPE_V)),
            /*.logits_all                  =*/ ToCBool(env->GetBooleanField(jllama_context_params, FIELD_LOGITS_ALL)),
            /*.embeddings                  =*/ ToCBool(env->GetBooleanField(jllama_context_params, FIELD_EMBEDDING)),
            /*.offload_kqv                 =*/ ToCBool(env->GetBooleanField(jllama_context_params, FIELD_OFFLOAD_KQV)),
            /*.flash_attn                  =*/ ToCBool(env->GetBooleanField(jllama_context_params, FIELD_FLASH_ATTN)),
            /*.abort_callback              =*/ nullptr,
            /*.abort_callback_data         =*/ nullptr,
    };
    return params;
}

static jobject GetLlamaTimingsToMetrics(JNIEnv *env, struct llama_timings timings) {
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
    env->DeleteLocalRef(metrics_class);
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

    //Class LlamaContextParams
    LLAMA_CONTEXT_PARAMS_CLASS = env->FindClass("chat/octet/model/beans/LlamaContextParams");
    FIELD_SEED = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "seed", "I");
    FIELD_CTX = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "ctx", "I");
    FIELD_BATCH = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "batch", "I");
    FIELD_UBATCH = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "ubatch", "I");
    FIELD_SEQ_MAX = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "seqMax", "I");
    FIELD_THREADS = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "threads", "I");
    FIELD_THREADS_BATCH = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "threadsBatch", "I");
    FIELD_ROPE_SCALING_TYPE = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "ropeScalingType", "I");
    FIELD_POOLING_TYPE = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "poolingType", "I");
    FIELD_YARN_EXT_FACTOR = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "yarnExtFactor", "F");
    FIELD_YARN_ATTN_FACTOR = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "yarnAttnFactor", "F");
    FIELD_YARN_BETA_FAST = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "yarnBetaFast", "F");
    FIELD_YARN_BETA_SLOW = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "yarnBetaSlow", "F");
    FIELD_YARN_ORIG_CTX = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "yarnOrigCtx", "I");
    FIELD_DEFRAG_THOLD = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "defragThold", "F");
    FIELD_ROPE_FREQ_BASE = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "ropeFreqBase", "F");
    FIELD_ROPE_FREQ_SCALE = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "ropeFreqScale", "F");
    FIELD_DATA_TYPE_K = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "dataTypeK", "I");
    FIELD_DATA_TYPE_V = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "dataTypeV", "I");
    FIELD_LOGITS_ALL = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "logitsAll", "Z");
    FIELD_EMBEDDING = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "embedding", "Z");
    FIELD_OFFLOAD_KQV = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "offloadKqv", "Z");
    FIELD_FLASH_ATTN = env->GetFieldID(LLAMA_CONTEXT_PARAMS_CLASS, "flashAttn", "Z");

    //Class LlamaContextParams
    LLAMA_MODEL_PARAMS_CLASS = env->FindClass("chat/octet/model/beans/LlamaModelParams");
    FIELD_GPU_LAYERS = env->GetFieldID(LLAMA_MODEL_PARAMS_CLASS, "gpuLayers", "I");
    FIELD_SPLIT_MODE = env->GetFieldID(LLAMA_MODEL_PARAMS_CLASS, "splitMode", "I");
    FIELD_MAIN_GPU = env->GetFieldID(LLAMA_MODEL_PARAMS_CLASS, "mainGpu", "I");
    FIELD_TENSOR_SPLIT = env->GetFieldID(LLAMA_MODEL_PARAMS_CLASS, "tensorSplit", "[F");
    FIELD_VOCAB_ONLY = env->GetFieldID(LLAMA_MODEL_PARAMS_CLASS, "vocabOnly", "Z");
    FIELD_USE_MMAP = env->GetFieldID(LLAMA_MODEL_PARAMS_CLASS, "mmap", "Z");
    FIELD_USE_MLOCK = env->GetFieldID(LLAMA_MODEL_PARAMS_CLASS, "mlock", "Z");
    FIELD_CHECK_TENSORS = env->GetFieldID(LLAMA_MODEL_PARAMS_CLASS, "checkTensors", "Z");

    //Class LlamaModelQuantizeParams
    LLAMA_MODEL_QUANTIZE_PARAMS_CLASS = env->FindClass("chat/octet/model/beans/LlamaModelQuantizeParams");
    FIELD_THREAD = env->GetFieldID(LLAMA_MODEL_QUANTIZE_PARAMS_CLASS, "thread", "I");
    FIELD_MODEL_FILE_TYPE = env->GetFieldID(LLAMA_MODEL_QUANTIZE_PARAMS_CLASS, "modelFileType", "I");
    FIELD_OUTPUT_TENSOR_TYPE = env->GetFieldID(LLAMA_MODEL_QUANTIZE_PARAMS_CLASS, "outputTensorType", "I");
    FIELD_TOKEN_EMBEDDING_TYPE = env->GetFieldID(LLAMA_MODEL_QUANTIZE_PARAMS_CLASS, "tokenEmbeddingType", "I");
    FIELD_ALLOW_REQUANTIZE = env->GetFieldID(LLAMA_MODEL_QUANTIZE_PARAMS_CLASS, "allowRequantize", "Z");
    FIELD_QUANTIZE_OUTPUT_TENSOR = env->GetFieldID(LLAMA_MODEL_QUANTIZE_PARAMS_CLASS, "quantizeOutputTensor", "Z");
    FIELD_ONLY_COPY = env->GetFieldID(LLAMA_MODEL_QUANTIZE_PARAMS_CLASS, "onlyCopy", "Z");
    FIELD_PURE = env->GetFieldID(LLAMA_MODEL_QUANTIZE_PARAMS_CLASS, "pure", "Z");
    FIELD_KEEP_SPLIT = env->GetFieldID(LLAMA_MODEL_QUANTIZE_PARAMS_CLASS, "keepSplit", "Z");

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
* Method:    getLlamaModelDefaultParams
*/
JNIEXPORT jobject JNICALL Java_chat_octet_model_LlamaService_getLlamaModelDefaultParams
        (JNIEnv *env, jclass thisClass) {
    llama_model_params defaults = llama_model_default_params();

    jclass llama_model_params_class = env->FindClass("chat/octet/model/beans/LlamaModelParams");
    jmethodID method_init_model_params = env->GetMethodID(llama_model_params_class, "<init>", "()V");
    jobject llama_model_params = env->NewObject(llama_model_params_class, method_init_model_params);

    env->SetIntField(llama_model_params, FIELD_GPU_LAYERS, defaults.n_gpu_layers);
    env->SetIntField(llama_model_params, FIELD_SPLIT_MODE, defaults.split_mode);
    env->SetIntField(llama_model_params, FIELD_MAIN_GPU, defaults.main_gpu);
    env->SetBooleanField(llama_model_params, FIELD_VOCAB_ONLY, ToJBoolean(defaults.vocab_only));
    env->SetBooleanField(llama_model_params, FIELD_USE_MMAP, ToJBoolean(defaults.use_mmap));
    env->SetBooleanField(llama_model_params, FIELD_USE_MLOCK, ToJBoolean(defaults.use_mlock));
    env->SetBooleanField(llama_model_params, FIELD_CHECK_TENSORS, ToJBoolean(defaults.check_tensors));
    env->DeleteLocalRef(llama_model_params_class);
    return llama_model_params;
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
    env->SetIntField(llama_context_params, FIELD_UBATCH, defaults.n_ubatch);
    env->SetIntField(llama_context_params, FIELD_SEQ_MAX, defaults.n_seq_max);
    env->SetIntField(llama_context_params, FIELD_THREADS, defaults.n_threads);
    env->SetIntField(llama_context_params, FIELD_THREADS_BATCH, defaults.n_threads_batch);
    env->SetIntField(llama_context_params, FIELD_ROPE_SCALING_TYPE, defaults.rope_scaling_type);
    env->SetIntField(llama_context_params, FIELD_POOLING_TYPE, defaults.pooling_type);
    env->SetFloatField(llama_context_params, FIELD_YARN_EXT_FACTOR, defaults.yarn_ext_factor);
    env->SetFloatField(llama_context_params, FIELD_YARN_ATTN_FACTOR, defaults.yarn_attn_factor);
    env->SetFloatField(llama_context_params, FIELD_YARN_BETA_FAST, defaults.yarn_beta_fast);
    env->SetFloatField(llama_context_params, FIELD_YARN_BETA_SLOW, defaults.yarn_beta_slow);
    env->SetIntField(llama_context_params, FIELD_YARN_ORIG_CTX, defaults.yarn_orig_ctx);
    env->SetFloatField(llama_context_params, FIELD_DEFRAG_THOLD, defaults.defrag_thold);
    env->SetFloatField(llama_context_params, FIELD_ROPE_FREQ_BASE, defaults.rope_freq_base);
    env->SetFloatField(llama_context_params, FIELD_ROPE_FREQ_SCALE, defaults.rope_freq_scale);
    env->SetIntField(llama_context_params, FIELD_DATA_TYPE_K, defaults.type_k);
    env->SetIntField(llama_context_params, FIELD_DATA_TYPE_V, defaults.type_v);
    env->SetBooleanField(llama_context_params, FIELD_LOGITS_ALL, ToJBoolean(defaults.logits_all));
    env->SetBooleanField(llama_context_params, FIELD_EMBEDDING, ToJBoolean(defaults.embeddings));
    env->SetBooleanField(llama_context_params, FIELD_OFFLOAD_KQV, ToJBoolean(defaults.offload_kqv));
    env->SetBooleanField(llama_context_params, FIELD_FLASH_ATTN, ToJBoolean(defaults.flash_attn));
    env->DeleteLocalRef(llama_context_params_class);
    return llama_context_params;
}

/*
* Class:     chat_octet_model_LlamaService
* Method:    getLlamaModelQuantizeDefaultParams
*/
JNIEXPORT jobject JNICALL Java_chat_octet_model_LlamaService_getLlamaModelQuantizeDefaultParams
        (JNIEnv *env, jclass thisClass) {
    llama_model_quantize_params defaults = llama_model_quantize_default_params();

    jclass llama_model_quantize_params_class = env->FindClass("chat/octet/model/beans/LlamaModelQuantizeParams");
    jmethodID method_init_quantize_params = env->GetMethodID(llama_model_quantize_params_class, "<init>", "()V");
    jobject llama_model_quantize_params = env->NewObject(llama_model_quantize_params_class, method_init_quantize_params);

    env->SetIntField(llama_model_quantize_params, FIELD_THREAD, defaults.nthread);
    env->SetIntField(llama_model_quantize_params, FIELD_MODEL_FILE_TYPE, defaults.ftype);
    env->SetIntField(llama_model_quantize_params, FIELD_OUTPUT_TENSOR_TYPE, defaults.output_tensor_type);
    env->SetIntField(llama_model_quantize_params, FIELD_TOKEN_EMBEDDING_TYPE, defaults.token_embedding_type);
    env->SetBooleanField(llama_model_quantize_params, FIELD_ALLOW_REQUANTIZE, ToJBoolean(defaults.allow_requantize));
    env->SetBooleanField(llama_model_quantize_params, FIELD_QUANTIZE_OUTPUT_TENSOR, ToJBoolean(defaults.quantize_output_tensor));
    env->SetBooleanField(llama_model_quantize_params, FIELD_ONLY_COPY, ToJBoolean(defaults.only_copy));
    env->SetBooleanField(llama_model_quantize_params, FIELD_PURE, ToJBoolean(defaults.pure));
    env->SetBooleanField(llama_model_quantize_params, FIELD_KEEP_SPLIT, ToJBoolean(defaults.keep_split));
    env->DeleteLocalRef(llama_model_quantize_params_class);
    return llama_model_quantize_params;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    llamaBackendInit
 */
JNIEXPORT void JNICALL Java_chat_octet_model_LlamaService_llamaBackendInit
        (JNIEnv *env, jclass thisClass) {
    llama_backend_init();
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    llamaNumaInit
 */
JNIEXPORT void JNICALL Java_chat_octet_model_LlamaService_llamaNumaInit
        (JNIEnv *env, jclass thisClass, jint numa_strategy) {
    llama_numa_init(static_cast<enum ggml_numa_strategy>(numa_strategy));
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
        (JNIEnv *env, jclass thisClass, jstring modelPath, jobject jllama_model_params) {
    struct llama_model_params params = GetLlamaModelParams(env, jllama_model_params);
    model = llama_load_model_from_file(ToCString(env, modelPath), params);

    if (model == nullptr) {
        ThrowByName(env, model_exception, "Load model failed");
    }
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    createNewContextWithModel
 */
JNIEXPORT void JNICALL Java_chat_octet_model_LlamaService_createNewContextWithModel
        (JNIEnv *env, jclass thisClass, jobject jllama_context_params) {
    struct llama_context_params params = GetLlamaContextParams(env, jllama_context_params);
    llama_ctx = llama_new_context_with_model(model, params);
    llama_ctx_params = params;

    if (llama_ctx == nullptr) {
        ThrowByName(env, model_exception, "Create llama context failed");
    }
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    release
 */
JNIEXPORT void JNICALL Java_chat_octet_model_LlamaService_release
        (JNIEnv *env, jclass thisClass) {
    if (grammar != nullptr) {
        llama_grammar_free(grammar);
        grammar = nullptr;
    }
    if (model != nullptr) {
        llama_free_model(model);
        model = nullptr;
    }
    if (llama_ctx != nullptr) {
        llama_free(llama_ctx);
        llama_ctx = nullptr;
    }
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    isMmapSupported
 */
JNIEXPORT jboolean JNICALL Java_chat_octet_model_LlamaService_isMmapSupported
        (JNIEnv *env, jclass thisClass) {
    return ToJBoolean(llama_supports_mmap());
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    isMlockSupported
 */
JNIEXPORT jboolean JNICALL Java_chat_octet_model_LlamaService_isMlockSupported
        (JNIEnv *env, jclass thisClass) {
    return ToJBoolean(llama_supports_mlock());
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    isGpuOffloadSupported
 */
JNIEXPORT jboolean JNICALL Java_chat_octet_model_LlamaService_isGpuOffloadSupported
        (JNIEnv *env, jclass thisClass) {
    return ToJBoolean(llama_supports_gpu_offload());
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getVocabSize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getVocabSize
        (JNIEnv *env, jclass thisClass) {
    return llama_n_vocab(model);
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
 * Method:    loadLoraModelFromFile
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_loadLoraModelFromFile
        (JNIEnv *env, jclass thisClass, jstring lora_path, jfloat scale, jstring base_model_path,
         jint threads) {
    if (model == nullptr) {
        ThrowByName(env, model_exception, "llama model cannot be null");
        return -1;
    }
    return llama_model_apply_lora_from_file(model, ToCString(env, lora_path), scale, ToCString(env, base_model_path),
                                            threads);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getLogits
 */
JNIEXPORT jfloatArray JNICALL Java_chat_octet_model_LlamaService_getLogits
        (JNIEnv *env, jclass thisClass, jint index) {
    int n_ctx = llama_ctx_params.n_ctx;
    if (index < 0 || index > n_ctx) {
        std::string msg = "Invalid index, range 0 to " + std::to_string(n_ctx);
        ThrowByName(env, model_exception, msg.c_str());
        return nullptr;
    }
    float *logits;
    if (llama_ctx_params.logits_all) {
        logits = llama_get_logits_ith(llama_ctx, index);
    } else {
        logits = llama_get_logits(llama_ctx);
    }
    const int vocab_size = llama_n_vocab(model);
    jfloatArray arrays = env->NewFloatArray(vocab_size);
    env->SetFloatArrayRegion(arrays, 0, vocab_size, logits);
    return arrays;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getEmbedding
 */
JNIEXPORT jfloatArray JNICALL Java_chat_octet_model_LlamaService_getEmbedding
        (JNIEnv *env, jclass thisClass) {
    float *embeddings = llama_get_embeddings(llama_ctx);
    const int embd_size = llama_n_embd(model);

    jfloatArray arrays = env->NewFloatArray(embd_size);
    env->SetFloatArrayRegion(arrays, 0, embd_size, embeddings);
    return arrays;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenAttr
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenAttr
        (JNIEnv *env, jclass thisClass, jint token) {
    return llama_token_get_attr(model, token);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenBOS
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenBOS
        (JNIEnv *env, jclass thisClass) {
    return llama_token_bos(model);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenEOS
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenEOS
        (JNIEnv *env, jclass thisClass) {
    return llama_token_eos(model);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    tokenize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_tokenize
        (JNIEnv *env, jclass thisClass, jbyteArray buf, jint buffer_length,
         jintArray tokens_arrays,
         jint maxTokens, jboolean addBos, jboolean specialTokens) {
    llama_token *tokens = (llama_token *) env->GetIntArrayElements(tokens_arrays, JNI_FALSE);

    jbyte *buffer = new jbyte[buffer_length];
    env->GetByteArrayRegion(buf, 0, buffer_length, buffer);
    const char *text = (char *) buffer;

    int code = llama_tokenize(model, text, buffer_length, tokens, maxTokens, ToCBool(addBos), ToCBool(specialTokens));
    env->ReleaseIntArrayElements(tokens_arrays, (jint *)tokens, 0);
    env->ReleaseByteArrayElements(buf, buffer, 0);
    return code;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    tokenToPiece
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_tokenToPiece
        (JNIEnv *env, jclass thisClass, jint token, jbyteArray buf, jint buffer_length, jboolean special) {
    jbyte *buffer = new jbyte[buffer_length];
    int size = llama_token_to_piece(model, token, (char *) buffer, buffer_length, ToCBool(special));
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
         jfloatArray jlogits,
         jintArray last_tokens_array,
         jint last_tokens_size,
         jfloat penalty,
         jfloat alpha_frequency,
         jfloat alpha_presence,
         jboolean penalize_nl,
         jint mirostat_mode,
         jfloat mirostat_tau,
         jfloat mirostat_eta,
         jfloat temperature,
         jint top_k,
         jfloat top_p,
         jfloat tsf,
         jfloat typical,
         jfloat min_p,
         jfloat dynatemp_range,
         jfloat dynatemp_exponent,
         jint sequence_id,
         jint past_token_size) {

    float *logits = env->GetFloatArrayElements(jlogits, JNI_FALSE);
    const int n_vocab = llama_n_vocab(model);
    const int token_nl = llama_token_nl(model);
    const float nl_logit = logits[token_nl];
    const int32_t final_top_k = top_k <= 0 ? n_vocab : top_k;

    std::vector<llama_token_data> candidates;
    candidates.reserve(n_vocab);
    for (llama_token token_id = 0; token_id < n_vocab; token_id++) {
        llama_token_data data = {token_id, logits[token_id], 0.0f};
        candidates.emplace_back(data);
    }
    llama_token_data_array candidates_p = {candidates.data(), candidates.size(), false};

    if (last_tokens_array != nullptr) {
        llama_token *last_tokens = (llama_token *) env->GetIntArrayElements(last_tokens_array, JNI_FALSE);

        //repetition penalty
        llama_sample_repetition_penalties(llama_ctx,
                                          &candidates_p,
                                          last_tokens,
                                          last_tokens_size,
                                          penalty,
                                          alpha_frequency,
                                          alpha_presence);
        env->ReleaseIntArrayElements(last_tokens_array, (jint *)last_tokens, 0);
    }

    if (!penalize_nl) {
        candidates_p.data[token_nl].logit = nl_logit;
    }

    if (grammar != nullptr) {
        llama_sample_grammar(llama_ctx, &candidates_p, grammar);
    }

    llama_token token;
    if (temperature <= 0) {
        token = llama_sample_token_greedy(llama_ctx, &candidates_p);
    } else {
        if (mirostat_mode == 1) {
            const int mirostat_m = 100;
            static float final_mirostat_mu = 2.0f * mirostat_tau;
            llama_sample_temp(llama_ctx, &candidates_p, temperature);
            token = llama_sample_token_mirostat(llama_ctx, &candidates_p, mirostat_tau, mirostat_eta, mirostat_m,
                                                &final_mirostat_mu);
        } else if (mirostat_mode == 2) {
            static float final_mirostat_mu = 2.0f * mirostat_tau;
            llama_sample_temp(llama_ctx, &candidates_p, temperature);
            token = llama_sample_token_mirostat_v2(llama_ctx, &candidates_p, mirostat_tau, mirostat_eta,
                                                   &final_mirostat_mu);
        } else {
            llama_sample_top_k(llama_ctx, &candidates_p, final_top_k, 1);
            llama_sample_tail_free(llama_ctx, &candidates_p, tsf, 1);
            llama_sample_typical(llama_ctx, &candidates_p, typical, 1);
            llama_sample_top_p(llama_ctx, &candidates_p, top_p, 1);
            llama_sample_min_p(llama_ctx, &candidates_p, min_p, 1);
            if (dynatemp_range > 0) {
                float dynatemp_min = std::max(0.0f, temperature - dynatemp_range);
                float dynatemp_max = std::max(0.0f, temperature + dynatemp_range);
                llama_sample_entropy(llama_ctx, &candidates_p, dynatemp_min, dynatemp_max, dynatemp_exponent);
            } else {
                llama_sample_temp(llama_ctx, &candidates_p, temperature);
            }
            token = llama_sample_token(llama_ctx, &candidates_p);
        }
    }

    if (grammar != nullptr) {
        llama_grammar_accept_token(llama_ctx, grammar, token);
    }

    int decode_status = 0;
    if (token != llama_token_eos(model)) {
        //decode the next new token
        int default_n_seq_max = 1;
        llama_batch batch = llama_batch_init(1, 0, default_n_seq_max);
        batch.token[0] = token;
        batch.pos[0] = past_token_size;
        batch.n_seq_id[0] = default_n_seq_max;
        batch.seq_id[0][0] = sequence_id;
        batch.logits[0] = true;
        batch.n_tokens = 1;
        decode_status = llama_decode(llama_ctx, batch);
        llama_batch_free(batch);
    }

    //clear all resources
    env->ReleaseFloatArrayElements(jlogits, logits, 0);

    //check decode status
    if (decode_status != 0) {
        std::string msg = "Failed to decode, return code: " + std::to_string(decode_status);
        ThrowByName(env, decode_exception, msg.c_str());
    }
    return token;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    loadLlamaGrammar
 */
JNIEXPORT jboolean JNICALL Java_chat_octet_model_LlamaService_loadLlamaGrammar
        (JNIEnv *env, jclass thisClass, jstring grammar_rules_text) {
    if (grammar_rules_text == nullptr) {
        return false;
    }
    if (grammar != nullptr) {
        llama_grammar_free(grammar);
    }
    const char *grammar_chars = env->GetStringUTFChars(grammar_rules_text, JNI_FALSE);
    grammar_parser::parse_state parsed_grammar = grammar_parser::parse(grammar_chars);

    jboolean status = false;
    if (!parsed_grammar.rules.empty()) {
        std::vector<const llama_grammar_element *> grammar_rules(parsed_grammar.c_rules());
        grammar = llama_grammar_init(grammar_rules.data(), grammar_rules.size(), parsed_grammar.symbol_ids.at("root"));
        status = true;
    }
    env->ReleaseStringUTFChars(grammar_rules_text, grammar_chars);
    return status;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    batchDecode
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_batchDecode
        (JNIEnv *env, jclass thisClass, jint sequence_id, jintArray tokens_arrays, jint input_length,
         jint past_token_size) {
    llama_token *tokens = (llama_token *) env->GetIntArrayElements(tokens_arrays, JNI_FALSE);
    //copy tokens to vector
    std::vector<llama_token> src_tokens;
    src_tokens.reserve(input_length);
    for (int i = 0; i < input_length; i++) {
        src_tokens.emplace_back(tokens[i]);
    }

    //batch decode
    int past_tokens = past_token_size;
    int decode_status = 0;
    int default_n_seq_max = 1;
    int n_batch = llama_ctx_params.n_batch;
    while (past_tokens < input_length) {
        int decode_size = input_length - past_tokens;
        if (decode_size > n_batch) {
            decode_size = n_batch;
        }
        int end_index = decode_size + past_tokens;
        std::vector<llama_token> batch_tokens(src_tokens.begin() + past_tokens, src_tokens.begin() + end_index);

        llama_batch batch = llama_batch_init(decode_size, 0, default_n_seq_max);
        batch.n_tokens = decode_size;
        for (int32_t i = 0; i < batch.n_tokens; i++) {
            batch.token[i] = batch_tokens[i];
            batch.pos[i] = i + past_tokens;
            batch.n_seq_id[i] = default_n_seq_max;
            batch.seq_id[i][0] = sequence_id;
            batch.logits[i] = false;
        }

        if (llama_ctx_params.logits_all) {
            //set logits for the last token of the prompt
            if (input_length == end_index) {
                batch.logits[batch.n_tokens - 1] = true;
            }
        } else {
            batch.logits = nullptr;
        }

        decode_status = llama_decode(llama_ctx, batch);
        llama_batch_free(batch);
        if (decode_status != 0) {
            break;
        }
        past_tokens += decode_size;
    }
    //clear all resources
    env->ReleaseIntArrayElements(tokens_arrays, (jint *)tokens, 0);
    return decode_status;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    clearCache
 */
JNIEXPORT void JNICALL Java_chat_octet_model_LlamaService_clearCache
        (JNIEnv *env, jclass thisClass, jint sequence_id, jint pos_start, jint pos_end) {
    llama_kv_cache_seq_rm(llama_ctx, sequence_id, pos_start, pos_end);
}

/*
 * Class:     chat_octet_model_LlamaServicen
 * Method:    llamaModelQuantize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_llamaModelQuantize
        (JNIEnv *env, jclass thisClass, jstring source_model_file_path, jstring output_model_file_path, jobject quantize_params) {

    struct llama_model_quantize_params params = {
        /*.nthread                     =*/ env->GetIntField(quantize_params, FIELD_THREAD),
        /*.ftype                       =*/ static_cast<enum llama_ftype>(env->GetIntField(quantize_params, FIELD_MODEL_FILE_TYPE)),
        /*.output_tensor_type          =*/ static_cast<enum ggml_type>(env->GetIntField(quantize_params, FIELD_OUTPUT_TENSOR_TYPE)),
        /*.token_embedding_type        =*/ static_cast<enum ggml_type>(env->GetIntField(quantize_params, FIELD_TOKEN_EMBEDDING_TYPE)),
        /*.allow_requantize            =*/ ToCBool(env->GetBooleanField(quantize_params, FIELD_ALLOW_REQUANTIZE)),
        /*.quantize_output_tensor      =*/ ToCBool(env->GetBooleanField(quantize_params, FIELD_QUANTIZE_OUTPUT_TENSOR)),
        /*.only_copy                   =*/ ToCBool(env->GetBooleanField(quantize_params, FIELD_ONLY_COPY)),
        /*.pure                        =*/ ToCBool(env->GetBooleanField(quantize_params, FIELD_PURE)),
        /*.keep_split                  =*/ ToCBool(env->GetBooleanField(quantize_params, FIELD_KEEP_SPLIT)),
        /*.imatrix                     =*/ nullptr,
        /*.kv_overrides                =*/ nullptr,
    };

   return llama_model_quantize(ToCString(env, source_model_file_path), ToCString(env, output_model_file_path), &params);
}
