//
// Created by William W on 2023/9/13.
//
#include "llama.h"
#include "llamajava.h"
#include "common/grammar-parser.h"
#include <vector>
#include <string>
#include <iostream>
#include <stdio.h>
#include <stdbool.h>
#include <time.h>

//log level define
enum log_level_type {
    LOG_DEBUG = 0,
    LOG_INFO = 1,
    LOG_WARN = 2,
    LOG_ERROR = 3,
};

//global context
struct llama_java_context {
    llama_model *model;
    llama_context *llama_ctx;
    llama_context_params params;
    llama_grammar *grammar;
};

static jint JNI_VERSION = JNI_VERSION_1_8;
static jint DEFAULT_LOG_LEVEL = 1;

llama_java_context *main_ctx = nullptr;

static void JLog_Print(log_level_type level, int line, const char *fmt, ...) {
    if (static_cast<int>(level) < DEFAULT_LOG_LEVEL) {
        return;
    }
    va_list args;
    va_start(args, fmt);

    time_t rawtime;
    struct tm *timeinfo;
    char timestamp[80];
    time(&rawtime);
    timeinfo = localtime(&rawtime);
    strftime(timestamp, sizeof(timestamp), "%Y-%m-%d %H:%M:%S", timeinfo);

    fprintf(stderr, "%s [llama-java:%d] ", timestamp, line);

    switch (level) {
        case LOG_DEBUG:
            fputs("DEBUG ", stderr);
            break;
        case LOG_INFO:
            fputs("INFO ", stderr);
            break;
        case LOG_WARN:
            fputs("WARN ", stderr);
            break;
        case LOG_ERROR:
            fputs("ERROR ", stderr);
            break;
    }

    vfprintf(stderr, fmt, args);
    fputc('\n', stderr);
    va_end(args);
}

#define JLOG_INFO(...) JLog_Print(LOG_INFO, __LINE__, __VA_ARGS__);
#define JLOG_ERROR(...) JLog_Print(LOG_ERROR, __LINE__, __VA_ARGS__);
#define JLOG_DEBUG(...) JLog_Print(LOG_DEBUG, __LINE__, __VA_ARGS__);
#define JLOG_WARN(...) JLog_Print(LOG_WARN, __LINE__, __VA_ARGS__);

//Class exceptions
static jclass MODEL_EXCEPTION_CLASS;
static jclass DECODE_EXCEPTION_CLASS;
//Class LlamaContextParams
static jclass LLAMA_CONTEXT_PARAMS_CLASS;
static jmethodID MD_CONS_LLAMA_CONTEXT_PARAMS;
static jfieldID FIELD_SEED;
static jfieldID FIELD_CTX;
static jfieldID FIELD_BATCH;
static jfieldID FIELD_UBATCH;
static jfieldID FIELD_SEQ_MAX;
static jfieldID FIELD_THREADS;
static jfieldID FIELD_THREADS_BATCH;
static jfieldID FIELD_ROPE_SCALING_TYPE;
static jfieldID FIELD_POOLING_TYPE;
static jfieldID FIELD_YARN_EXT_FACTOR;
static jfieldID FIELD_YARN_ATTN_FACTOR;
static jfieldID FIELD_YARN_BETA_FAST;
static jfieldID FIELD_YARN_BETA_SLOW;
static jfieldID FIELD_YARN_ORIG_CTX;
static jfieldID FIELD_DEFRAG_THOLD;
static jfieldID FIELD_ROPE_FREQ_BASE;
static jfieldID FIELD_ROPE_FREQ_SCALE;
static jfieldID FIELD_DATA_TYPE_K;
static jfieldID FIELD_DATA_TYPE_V;
static jfieldID FIELD_LOGITS_ALL;
static jfieldID FIELD_EMBEDDING;
static jfieldID FIELD_OFFLOAD_KQV;
static jfieldID FIELD_FLASH_ATTN;
static jfieldID FIELD_ABORT_CALLBACK;
static jfieldID FIELD_ABORT_CALLBACK_DATA;
//Class LlamaModelParams
static jclass LLAMA_MODEL_PARAMS_CLASS;
static jmethodID MD_CONS_LLAMA_MODEL_PARAMS;
static jfieldID FIELD_GPU_LAYERS;
static jfieldID FIELD_SPLIT_MODE;
static jfieldID FIELD_MAIN_GPU;
static jfieldID FIELD_TENSOR_SPLIT;
static jfieldID FIELD_VOCAB_ONLY;
static jfieldID FIELD_USE_MMAP;
static jfieldID FIELD_USE_MLOCK;
static jfieldID FIELD_CHECK_TENSORS;
static jfieldID FIELD_NUMA_STRATEGY;
//Class LlamaModelQuantizeParams
static jclass LLAMA_MODEL_QUANTIZE_PARAMS_CLASS;
static jmethodID MD_CONS_LLAMA_MODEL_QUANTIZE_PARAMS;
static jfieldID FIELD_THREAD;
static jfieldID FIELD_MODEL_FILE_TYPE;
static jfieldID FIELD_OUTPUT_TENSOR_TYPE;
static jfieldID FIELD_TOKEN_EMBEDDING_TYPE;
static jfieldID FIELD_ALLOW_REQUANTIZE;
static jfieldID FIELD_QUANTIZE_OUTPUT_TENSOR;
static jfieldID FIELD_ONLY_COPY;
static jfieldID FIELD_PURE;
static jfieldID FIELD_KEEP_SPLIT;
static jfieldID FIELD_IMATRIX;
static jfieldID FIELD_KV_OVERRIDES;
//Class Metrics
static jclass METRICS_CLASS;
static jmethodID MD_CONS_METRICS;
static jfieldID FIELD_START_TIME_MS;
static jfieldID FIELD_END_TIME_MS;
static jfieldID FIELD_LOAD_TIME_MS;
static jfieldID FIELD_SAMPLE_TIME_MS;
static jfieldID FIELD_PROMPT_EVAL_TIME_MS;
static jfieldID FIELD_EVAL_TIME_MS;
static jfieldID FIELD_SAMPLE_COUNT;
static jfieldID FIELD_PROMPT_EVAL_COUNT;
static jfieldID FIELD_EVAL_COUNT;

static bool Check_Context_Is_Null(JNIEnv *env) {
    if (main_ctx == nullptr) {
        env->ThrowNew(MODEL_EXCEPTION_CLASS, "Model is not loaded, please load model first.");
        return true;
    }
    return false;
}

static jboolean To_JBoolean(bool value) {
    return value ? JNI_TRUE : JNI_FALSE;
}

static bool To_CBool(jboolean value) {
    return value == JNI_TRUE;
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    // Obtain the JNIEnv from the VM and confirm JNI_VERSION
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION) != JNI_OK) {
        return JNI_ERR;
    }

    jclass temp_local_class_ref;
    //Class ModelException
    temp_local_class_ref = env->FindClass("chat/octet/model/exceptions/ModelException");
    MODEL_EXCEPTION_CLASS = (jclass) env->NewGlobalRef(temp_local_class_ref);
    env->DeleteLocalRef(temp_local_class_ref);
    //Class DecodeException
    temp_local_class_ref = env->FindClass("chat/octet/model/exceptions/DecodeException");
    DECODE_EXCEPTION_CLASS = (jclass) env->NewGlobalRef(temp_local_class_ref);
    env->DeleteLocalRef(temp_local_class_ref);

    //Class LlamaContextParams
    temp_local_class_ref = env->FindClass("chat/octet/model/beans/LlamaContextParams");
    LLAMA_CONTEXT_PARAMS_CLASS = (jclass) env->NewGlobalRef(temp_local_class_ref);
    env->DeleteLocalRef(temp_local_class_ref);
    MD_CONS_LLAMA_CONTEXT_PARAMS = env->GetMethodID(LLAMA_CONTEXT_PARAMS_CLASS, "<init>", "()V");
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

    //Class LlamaModelParams
    temp_local_class_ref = env->FindClass("chat/octet/model/beans/LlamaModelParams");
    LLAMA_MODEL_PARAMS_CLASS = (jclass) env->NewGlobalRef(temp_local_class_ref);
    env->DeleteLocalRef(temp_local_class_ref);
    MD_CONS_LLAMA_MODEL_PARAMS = env->GetMethodID(LLAMA_MODEL_PARAMS_CLASS, "<init>", "()V");
    FIELD_GPU_LAYERS = env->GetFieldID(LLAMA_MODEL_PARAMS_CLASS, "gpuLayers", "I");
    FIELD_SPLIT_MODE = env->GetFieldID(LLAMA_MODEL_PARAMS_CLASS, "splitMode", "I");
    FIELD_MAIN_GPU = env->GetFieldID(LLAMA_MODEL_PARAMS_CLASS, "mainGpu", "I");
    FIELD_TENSOR_SPLIT = env->GetFieldID(LLAMA_MODEL_PARAMS_CLASS, "tensorSplit", "[F");
    FIELD_VOCAB_ONLY = env->GetFieldID(LLAMA_MODEL_PARAMS_CLASS, "vocabOnly", "Z");
    FIELD_USE_MMAP = env->GetFieldID(LLAMA_MODEL_PARAMS_CLASS, "mmap", "Z");
    FIELD_USE_MLOCK = env->GetFieldID(LLAMA_MODEL_PARAMS_CLASS, "mlock", "Z");
    FIELD_CHECK_TENSORS = env->GetFieldID(LLAMA_MODEL_PARAMS_CLASS, "checkTensors", "Z");
    FIELD_NUMA_STRATEGY = env->GetFieldID(LLAMA_MODEL_PARAMS_CLASS, "numaStrategy", "I");

    //Class LlamaModelQuantizeParams
    temp_local_class_ref = env->FindClass("chat/octet/model/beans/LlamaModelQuantizeParams");
    LLAMA_MODEL_QUANTIZE_PARAMS_CLASS = (jclass) env->NewGlobalRef(temp_local_class_ref);
    env->DeleteLocalRef(temp_local_class_ref);
    MD_CONS_LLAMA_MODEL_QUANTIZE_PARAMS = env->GetMethodID(LLAMA_MODEL_QUANTIZE_PARAMS_CLASS, "<init>", "()V");
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
    temp_local_class_ref = env->FindClass("chat/octet/model/beans/Metrics");
    METRICS_CLASS = (jclass) env->NewGlobalRef(temp_local_class_ref);
    env->DeleteLocalRef(temp_local_class_ref);
    MD_CONS_METRICS = env->GetMethodID(LLAMA_MODEL_QUANTIZE_PARAMS_CLASS, "<init>", "()V");
    FIELD_START_TIME_MS = env->GetFieldID(METRICS_CLASS, "startTimeMs", "D");
    FIELD_END_TIME_MS = env->GetFieldID(METRICS_CLASS, "endTimeMs", "D");
    FIELD_LOAD_TIME_MS = env->GetFieldID(METRICS_CLASS, "loadTimeMs", "D");
    FIELD_SAMPLE_TIME_MS = env->GetFieldID(METRICS_CLASS, "sampleTimeMs", "D");
    FIELD_PROMPT_EVAL_TIME_MS = env->GetFieldID(METRICS_CLASS, "promptEvalTimeMs", "D");
    FIELD_EVAL_TIME_MS = env->GetFieldID(METRICS_CLASS, "evalTimeMs", "D");
    FIELD_SAMPLE_COUNT = env->GetFieldID(METRICS_CLASS, "sampleCount", "I");
    FIELD_PROMPT_EVAL_COUNT = env->GetFieldID(METRICS_CLASS, "promptEvalCount", "I");
    FIELD_EVAL_COUNT = env->GetFieldID(METRICS_CLASS, "evalCount", "I");

    JLOG_INFO("Initialize local class variables completed.");
    // Return the JNI Version as required by method
    return JNI_VERSION;
}

void JNI_OnUnload(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION);
    // Destroy the global references
    env->DeleteGlobalRef(MODEL_EXCEPTION_CLASS);
    env->DeleteGlobalRef(DECODE_EXCEPTION_CLASS);
    env->DeleteGlobalRef(LLAMA_CONTEXT_PARAMS_CLASS);
    env->DeleteGlobalRef(LLAMA_MODEL_PARAMS_CLASS);
    env->DeleteGlobalRef(LLAMA_MODEL_QUANTIZE_PARAMS_CLASS);
    env->DeleteGlobalRef(METRICS_CLASS);

    JLOG_DEBUG("JNI_OnUnload called.");
}

/*
* Class:     chat_octet_model_LlamaService
* Method:    setLogLevel
*/
JNIEXPORT void JNICALL Java_chat_octet_model_LlamaService_setLogLevel
        (JNIEnv *env, jclass thisClass, jint log_level) {
    DEFAULT_LOG_LEVEL = log_level;
}

/*
* Class:     chat_octet_model_LlamaService
* Method:    getLlamaModelDefaultParams
*/
JNIEXPORT jobject JNICALL Java_chat_octet_model_LlamaService_getLlamaModelDefaultParams
        (JNIEnv *env, jclass thisClass) {
    llama_model_params defaults = llama_model_default_params();

    jobject llama_model_params = env->NewObject(LLAMA_MODEL_PARAMS_CLASS, MD_CONS_LLAMA_MODEL_PARAMS);
    env->SetIntField(llama_model_params, FIELD_GPU_LAYERS, defaults.n_gpu_layers);
    env->SetIntField(llama_model_params, FIELD_SPLIT_MODE, defaults.split_mode);
    env->SetIntField(llama_model_params, FIELD_MAIN_GPU, defaults.main_gpu);
    env->SetBooleanField(llama_model_params, FIELD_VOCAB_ONLY, To_JBoolean(defaults.vocab_only));
    env->SetBooleanField(llama_model_params, FIELD_USE_MMAP, To_JBoolean(defaults.use_mmap));
    env->SetBooleanField(llama_model_params, FIELD_USE_MLOCK, To_JBoolean(defaults.use_mlock));
    env->SetBooleanField(llama_model_params, FIELD_CHECK_TENSORS, To_JBoolean(defaults.check_tensors));
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
    jobject llama_context_params = env->NewObject(LLAMA_CONTEXT_PARAMS_CLASS, MD_CONS_LLAMA_CONTEXT_PARAMS);
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
    env->SetBooleanField(llama_context_params, FIELD_LOGITS_ALL, To_JBoolean(defaults.logits_all));
    env->SetBooleanField(llama_context_params, FIELD_EMBEDDING, To_JBoolean(defaults.embeddings));
    env->SetBooleanField(llama_context_params, FIELD_OFFLOAD_KQV, To_JBoolean(defaults.offload_kqv));
    env->SetBooleanField(llama_context_params, FIELD_FLASH_ATTN, To_JBoolean(defaults.flash_attn));
    return llama_context_params;
}

/*
* Class:     chat_octet_model_LlamaService
* Method:    getLlamaModelQuantizeDefaultParams
*/
JNIEXPORT jobject JNICALL Java_chat_octet_model_LlamaService_getLlamaModelQuantizeDefaultParams
        (JNIEnv *env, jclass thisClass) {
    llama_model_quantize_params defaults = llama_model_quantize_default_params();

    jobject llama_model_quantize_params = env->NewObject(LLAMA_MODEL_QUANTIZE_PARAMS_CLASS,
                                                         MD_CONS_LLAMA_MODEL_QUANTIZE_PARAMS);
    env->SetIntField(llama_model_quantize_params, FIELD_THREAD, defaults.nthread);
    env->SetIntField(llama_model_quantize_params, FIELD_MODEL_FILE_TYPE, defaults.ftype);
    env->SetIntField(llama_model_quantize_params, FIELD_OUTPUT_TENSOR_TYPE, defaults.output_tensor_type);
    env->SetIntField(llama_model_quantize_params, FIELD_TOKEN_EMBEDDING_TYPE, defaults.token_embedding_type);
    env->SetBooleanField(llama_model_quantize_params, FIELD_ALLOW_REQUANTIZE, To_JBoolean(defaults.allow_requantize));
    env->SetBooleanField(llama_model_quantize_params, FIELD_QUANTIZE_OUTPUT_TENSOR, To_JBoolean(defaults.quantize_output_tensor));
    env->SetBooleanField(llama_model_quantize_params, FIELD_ONLY_COPY, To_JBoolean(defaults.only_copy));
    env->SetBooleanField(llama_model_quantize_params, FIELD_PURE, To_JBoolean(defaults.pure));
    env->SetBooleanField(llama_model_quantize_params, FIELD_KEEP_SPLIT, To_JBoolean(defaults.keep_split));
    return llama_model_quantize_params;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    llamaBackendFree
 */
JNIEXPORT void JNICALL Java_chat_octet_model_LlamaService_llamaBackendFree
        (JNIEnv *env, jclass thisClass) {
    llama_backend_free();
    JLOG_INFO("Released backend resources.");
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    loadLlamaModelFromFile
 */
JNIEXPORT void JNICALL Java_chat_octet_model_LlamaService_loadLlamaModelFromFile
        (JNIEnv *env, jclass thisClass, jstring jmodel_path, jobject jllama_model_params,
         jobject jllama_context_params) {

    if (main_ctx != nullptr) {
        JLOG_WARN("Model already loaded, releasing resources.");
        llama_kv_cache_clear(main_ctx->llama_ctx);
        Java_chat_octet_model_LlamaService_release(env, thisClass);
        Java_chat_octet_model_LlamaService_llamaBackendFree(env, thisClass);
    }
    llama_backend_init();
    jint numa_strategy = env->GetIntField(jllama_model_params, FIELD_NUMA_STRATEGY);
    llama_numa_init(static_cast<enum ggml_numa_strategy>(numa_strategy));

    main_ctx = new llama_java_context();

    //init model
    float *tensor_split = nullptr;
    jfloatArray arrays_data = (jfloatArray) env->GetObjectField(jllama_model_params, FIELD_TENSOR_SPLIT);
    if (arrays_data != nullptr) {
        tensor_split = env->GetFloatArrayElements(arrays_data, JNI_FALSE);
    }

    struct llama_model_params model_params = {
            /*.n_gpu_layers                =*/ env->GetIntField(jllama_model_params, FIELD_GPU_LAYERS),
            /*.split_mode                  =*/ static_cast<enum llama_split_mode>(env->GetIntField(jllama_model_params, FIELD_SPLIT_MODE)),
            /*.main_gpu                    =*/ env->GetIntField(jllama_model_params, FIELD_MAIN_GPU),
            /*.tensor_split                =*/ tensor_split,
            /*.rpc_servers                 =*/ nullptr,
            /*.progress_callback           =*/ nullptr,
            /*.progress_callback_user_data =*/ nullptr,
            /*.kv_overrides                =*/ nullptr,
            /*.vocab_only                  =*/ To_CBool(env->GetBooleanField(jllama_model_params, FIELD_VOCAB_ONLY)),
            /*.use_mmap                    =*/ To_CBool(env->GetBooleanField(jllama_model_params, FIELD_USE_MMAP)),
            /*.use_mlock                   =*/ To_CBool(env->GetBooleanField(jllama_model_params, FIELD_USE_MLOCK)),
            /*.check_tensors               =*/ To_CBool(env->GetBooleanField(jllama_model_params, FIELD_CHECK_TENSORS)),
    };
    if (arrays_data != nullptr) {
        env->ReleaseFloatArrayElements(arrays_data, tensor_split, 0);
    }

    const char *model_path = env->GetStringUTFChars(jmodel_path, JNI_FALSE);
    llama_model *model = llama_load_model_from_file(model_path, model_params);

    if (model == nullptr) {
        env->ThrowNew(MODEL_EXCEPTION_CLASS, "Load model failed.");
        return;
    }
    main_ctx->model = model;
    JLOG_DEBUG("Successfully loaded model file %s.", model_path);

    //init llama context
    struct llama_context_params context_params = {
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
            /*.logits_all                  =*/ To_CBool(env->GetBooleanField(jllama_context_params, FIELD_LOGITS_ALL)),
            /*.embeddings                  =*/ To_CBool(env->GetBooleanField(jllama_context_params, FIELD_EMBEDDING)),
            /*.offload_kqv                 =*/ To_CBool(env->GetBooleanField(jllama_context_params, FIELD_OFFLOAD_KQV)),
            /*.flash_attn                  =*/ To_CBool(env->GetBooleanField(jllama_context_params, FIELD_FLASH_ATTN)),
            /*.abort_callback              =*/ nullptr,
            /*.abort_callback_data         =*/ nullptr,
    };

    llama_context *llama_ctx = llama_new_context_with_model(model, context_params);
    if (llama_ctx == nullptr) {
        env->ThrowNew(MODEL_EXCEPTION_CLASS, "Create llama context failed.");
        return;
    }
    main_ctx->llama_ctx = llama_ctx;
    main_ctx->params = context_params;

    JLOG_DEBUG("Successfully created llama context.");
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    release
 */
JNIEXPORT void JNICALL Java_chat_octet_model_LlamaService_release
        (JNIEnv *env, jclass thisClass) {
    if (main_ctx == nullptr) return;

    if (main_ctx->grammar != nullptr) {
        llama_grammar_free(main_ctx->grammar);
        main_ctx->grammar = nullptr;
        JLOG_INFO("Successfully released grammar.");
    }
    if (main_ctx->model != nullptr) {
        llama_free_model(main_ctx->model);
        main_ctx->model = nullptr;
        JLOG_INFO("Successfully released model.");
    }
    if (main_ctx->llama_ctx != nullptr) {
        llama_free(main_ctx->llama_ctx);
        main_ctx->llama_ctx = nullptr;
        JLOG_INFO("Successfully released llama context.");
    }
    main_ctx = nullptr;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    isMmapSupported
 */
JNIEXPORT jboolean JNICALL Java_chat_octet_model_LlamaService_isMmapSupported
        (JNIEnv *env, jclass thisClass) {
    return To_JBoolean(llama_supports_mmap());
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    isMlockSupported
 */
JNIEXPORT jboolean JNICALL Java_chat_octet_model_LlamaService_isMlockSupported
        (JNIEnv *env, jclass thisClass) {
    return To_JBoolean(llama_supports_mlock());
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    isGpuOffloadSupported
 */
JNIEXPORT jboolean JNICALL Java_chat_octet_model_LlamaService_isGpuOffloadSupported
        (JNIEnv *env, jclass thisClass) {
    return To_JBoolean(llama_supports_gpu_offload());
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getVocabSize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getVocabSize
        (JNIEnv *env, jclass thisClass) {
    if (Check_Context_Is_Null(env)) return -1;
    return llama_n_vocab(main_ctx->model);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getContextSize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getContextSize
        (JNIEnv *env, jclass thisClass) {
    if (Check_Context_Is_Null(env)) return -1;
    return llama_n_ctx(main_ctx->llama_ctx);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    loadLoraModelFromFile
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_loadLoraModelFromFile
        (JNIEnv *env, jclass thisClass, jstring lora_path, jfloat scale, jstring base_model_path,
         jint threads) {
    if (Check_Context_Is_Null(env)) return -1;
    int status = llama_model_apply_lora_from_file(main_ctx->model,
                                                  env->GetStringUTFChars(lora_path, JNI_FALSE),
                                                  scale,
                                                  env->GetStringUTFChars(base_model_path, JNI_FALSE),
                                                  threads
    );
    JLOG_DEBUG("Successfully loaded lora model, status: %d.", status);
    return status;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getLogits
 */
JNIEXPORT jfloatArray JNICALL Java_chat_octet_model_LlamaService_getLogits
        (JNIEnv *env, jclass thisClass, jint index) {
    if (Check_Context_Is_Null(env)) return nullptr;
    llama_context_params params = main_ctx->params;
    int n_ctx = params.n_ctx;
    if (index < 0 || index > n_ctx) {
        std::string msg = "Invalid index, range 0 to " + std::to_string(n_ctx);
        env->ThrowNew(MODEL_EXCEPTION_CLASS, msg.c_str());
        return nullptr;
    }
    float *logits;
    if (params.logits_all) {
        logits = llama_get_logits_ith(main_ctx->llama_ctx, index);
    } else {
        logits = llama_get_logits(main_ctx->llama_ctx);
    }
    const int vocab_size = llama_n_vocab(main_ctx->model);
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
    if (Check_Context_Is_Null(env)) return nullptr;
    float *embeddings = llama_get_embeddings(main_ctx->llama_ctx);
    const int embd_size = llama_n_embd(main_ctx->model);

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
    if (Check_Context_Is_Null(env)) return -1;
    return llama_token_get_attr(main_ctx->model, token);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenBOS
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenBOS
        (JNIEnv *env, jclass thisClass) {
    if (Check_Context_Is_Null(env)) return -1;
    return llama_token_bos(main_ctx->model);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getTokenEOS
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_getTokenEOS
        (JNIEnv *env, jclass thisClass) {
    if (Check_Context_Is_Null(env)) return -1;
    return llama_token_eos(main_ctx->model);
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    tokenize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_tokenize
        (JNIEnv *env, jclass thisClass, jbyteArray buf, jint buffer_length,
         jintArray tokens_arrays,
         jint maxTokens, jboolean addBos, jboolean specialTokens) {
    if (Check_Context_Is_Null(env)) return -1;
    llama_token *tokens = (llama_token *) env->GetIntArrayElements(tokens_arrays, JNI_FALSE);

    jbyte *buffer = new jbyte[buffer_length];
    env->GetByteArrayRegion(buf, 0, buffer_length, buffer);
    const char *text = (char *) buffer;

    int code = llama_tokenize(main_ctx->model, text, buffer_length, tokens, maxTokens, To_CBool(addBos),
                              To_CBool(specialTokens));
    env->ReleaseIntArrayElements(tokens_arrays, (jint *) tokens, 0);
    env->ReleaseByteArrayElements(buf, buffer, 0);
    return code;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    tokenToPiece
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_tokenToPiece
        (JNIEnv *env, jclass thisClass, jint token, jbyteArray buf, jint buffer_length, jboolean special) {
    if (Check_Context_Is_Null(env)) return -1;

    jbyte *buffer = new jbyte[buffer_length];
    int size = llama_token_to_piece(main_ctx->model, token, (char *) buffer, buffer_length, To_CBool(special));
    env->ReleaseByteArrayElements(buf, buffer, 0);
    return size;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    getSamplingMetrics
 */
JNIEXPORT jobject JNICALL Java_chat_octet_model_LlamaService_getSamplingMetrics
        (JNIEnv *env, jclass thisClass, jboolean reset) {
    if (Check_Context_Is_Null(env)) return nullptr;

    struct llama_timings timings = llama_get_timings(main_ctx->llama_ctx);

    jobject jMetrics = env->NewObject(METRICS_CLASS, MD_CONS_METRICS);
    env->SetDoubleField(jMetrics, FIELD_START_TIME_MS, timings.t_start_ms);
    env->SetDoubleField(jMetrics, FIELD_END_TIME_MS, timings.t_end_ms);
    env->SetDoubleField(jMetrics, FIELD_LOAD_TIME_MS, timings.t_load_ms);
    env->SetDoubleField(jMetrics, FIELD_SAMPLE_TIME_MS, timings.t_sample_ms);
    env->SetDoubleField(jMetrics, FIELD_PROMPT_EVAL_TIME_MS, timings.t_p_eval_ms);
    env->SetDoubleField(jMetrics, FIELD_EVAL_TIME_MS, timings.t_eval_ms);
    env->SetIntField(jMetrics, FIELD_SAMPLE_COUNT, timings.n_sample);
    env->SetIntField(jMetrics, FIELD_PROMPT_EVAL_COUNT, timings.n_p_eval);
    env->SetIntField(jMetrics, FIELD_EVAL_COUNT, timings.n_eval);

    if (reset) {
        llama_reset_timings(main_ctx->llama_ctx);
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

    if (Check_Context_Is_Null(env)) return -1;

    float *logits = env->GetFloatArrayElements(jlogits, JNI_FALSE);
    const int n_vocab = llama_n_vocab(main_ctx->model);
    const int token_nl = llama_token_nl(main_ctx->model);
    const float nl_logit = logits[token_nl];
    const int32_t final_top_k = top_k <= 0 ? n_vocab : top_k;

    std::vector <llama_token_data> candidates;
    candidates.reserve(n_vocab);
    for (llama_token token_id = 0; token_id < n_vocab; token_id++) {
        llama_token_data data = {token_id, logits[token_id], 0.0f};
        candidates.emplace_back(data);
    }
    llama_token_data_array candidates_p = {candidates.data(), candidates.size(), false};

    if (last_tokens_array != nullptr) {
        llama_token *last_tokens = (llama_token *) env->GetIntArrayElements(last_tokens_array, JNI_FALSE);

        //repetition penalty
        llama_sample_repetition_penalties(main_ctx->llama_ctx,
                                          &candidates_p,
                                          last_tokens,
                                          last_tokens_size,
                                          penalty,
                                          alpha_frequency,
                                          alpha_presence);
        env->ReleaseIntArrayElements(last_tokens_array, (jint *) last_tokens, 0);
    }

    if (!penalize_nl) {
        candidates_p.data[token_nl].logit = nl_logit;
    }

    if (main_ctx->grammar != nullptr) {
        llama_sample_grammar(main_ctx->llama_ctx, &candidates_p, main_ctx->grammar);
    }

    llama_token token;
    if (temperature <= 0) {
        token = llama_sample_token_greedy(main_ctx->llama_ctx, &candidates_p);
    } else {
        if (mirostat_mode == 1) {
            const int mirostat_m = 100;
            static float final_mirostat_mu = 2.0f * mirostat_tau;
            llama_sample_temp(main_ctx->llama_ctx, &candidates_p, temperature);
            token = llama_sample_token_mirostat(main_ctx->llama_ctx, &candidates_p, mirostat_tau, mirostat_eta,
                                                mirostat_m,
                                                &final_mirostat_mu);
        } else if (mirostat_mode == 2) {
            static float final_mirostat_mu = 2.0f * mirostat_tau;
            llama_sample_temp(main_ctx->llama_ctx, &candidates_p, temperature);
            token = llama_sample_token_mirostat_v2(main_ctx->llama_ctx, &candidates_p, mirostat_tau, mirostat_eta,
                                                   &final_mirostat_mu);
        } else {
            llama_sample_top_k(main_ctx->llama_ctx, &candidates_p, final_top_k, 1);
            llama_sample_tail_free(main_ctx->llama_ctx, &candidates_p, tsf, 1);
            llama_sample_typical(main_ctx->llama_ctx, &candidates_p, typical, 1);
            llama_sample_top_p(main_ctx->llama_ctx, &candidates_p, top_p, 1);
            llama_sample_min_p(main_ctx->llama_ctx, &candidates_p, min_p, 1);
            if (dynatemp_range > 0) {
                float dynatemp_min = std::max(0.0f, temperature - dynatemp_range);
                float dynatemp_max = std::max(0.0f, temperature + dynatemp_range);
                llama_sample_entropy(main_ctx->llama_ctx, &candidates_p, dynatemp_min, dynatemp_max, dynatemp_exponent);
            } else {
                llama_sample_temp(main_ctx->llama_ctx, &candidates_p, temperature);
            }
            token = llama_sample_token(main_ctx->llama_ctx, &candidates_p);
        }
    }

    if (main_ctx->grammar != nullptr) {
        llama_grammar_accept_token(main_ctx->llama_ctx, main_ctx->grammar, token);
    }

    int decode_status = 0;
    if (token != llama_token_eos(main_ctx->model)) {
        //decode the next new token
        int default_n_seq_max = 1;
        llama_batch batch = llama_batch_init(1, 0, default_n_seq_max);
        batch.token[0] = token;
        batch.pos[0] = past_token_size;
        batch.n_seq_id[0] = default_n_seq_max;
        batch.seq_id[0][0] = sequence_id;
        batch.logits[0] = true;
        batch.n_tokens = 1;
        decode_status = llama_decode(main_ctx->llama_ctx, batch);
        llama_batch_free(batch);
    }

    //clear all resources
    env->ReleaseFloatArrayElements(jlogits, logits, 0);

    //check decode status
    if (decode_status != 0) {
        std::string msg = "Failed to decode, return code: " + std::to_string(decode_status);
        env->ThrowNew(DECODE_EXCEPTION_CLASS, msg.c_str());
    }
    return token;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    loadLlamaGrammar
 */
JNIEXPORT jboolean JNICALL Java_chat_octet_model_LlamaService_loadLlamaGrammar
        (JNIEnv *env, jclass thisClass, jstring grammar_rules_text) {
    if (Check_Context_Is_Null(env)) return false;
    if (grammar_rules_text == nullptr) return false;

    if (main_ctx->grammar != nullptr) {
        llama_grammar_free(main_ctx->grammar);
        main_ctx->grammar = nullptr;
        JLOG_DEBUG("Grammar is already loaded, reset it first.");
    }
    const char *grammar_chars = env->GetStringUTFChars(grammar_rules_text, JNI_FALSE);
    grammar_parser::parse_state parsed_grammar = grammar_parser::parse(grammar_chars);

    jboolean status = false;
    if (!parsed_grammar.rules.empty()) {
        std::vector<const llama_grammar_element *> grammar_rules(parsed_grammar.c_rules());
        main_ctx->grammar = llama_grammar_init(grammar_rules.data(), grammar_rules.size(),
                                               parsed_grammar.symbol_ids.at("root"));
        status = true;
        JLOG_DEBUG("Grammar rules loaded, rules count: %d.", grammar_rules.size());
    } else {
        JLOG_WARN("Grammar rules is empty, No rules loaded.");
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
    if (Check_Context_Is_Null(env)) return -1;

    llama_token *tokens = (llama_token *) env->GetIntArrayElements(tokens_arrays, JNI_FALSE);
    //copy tokens to vector
    std::vector <llama_token> src_tokens;
    src_tokens.reserve(input_length);
    for (int i = 0; i < input_length; i++) {
        src_tokens.emplace_back(tokens[i]);
    }

    //batch decode
    llama_context_params params = main_ctx->params;
    int past_tokens = past_token_size;
    int decode_status = 0;
    int default_n_seq_max = 1;
    int n_batch = params.n_batch;
    JLOG_DEBUG("Start batch decoding, sequence id: %d, input length: %d, past token size: %d, batch decoding size: %d.",
               sequence_id, input_length, past_tokens, (input_length - past_tokens));

    while (past_tokens < input_length) {
        int decode_size = input_length - past_tokens;
        if (decode_size > n_batch) {
            decode_size = n_batch;
        }
        int end_index = decode_size + past_tokens;
        std::vector <llama_token> batch_tokens(src_tokens.begin() + past_tokens, src_tokens.begin() + end_index);

        llama_batch batch = llama_batch_init(decode_size, 0, default_n_seq_max);
        batch.n_tokens = decode_size;
        for (int32_t i = 0; i < batch.n_tokens; i++) {
            batch.token[i] = batch_tokens[i];
            batch.pos[i] = i + past_tokens;
            batch.n_seq_id[i] = default_n_seq_max;
            batch.seq_id[i][0] = sequence_id;
            batch.logits[i] = false;
        }

        if (params.logits_all) {
            //set logits for the last token of the prompt
            if (input_length == end_index) {
                batch.logits[batch.n_tokens - 1] = true;
            }
        } else {
            batch.logits = nullptr;
        }

        decode_status = llama_decode(main_ctx->llama_ctx, batch);
        llama_batch_free(batch);
        if (decode_status != 0) {
            break;
        }
        past_tokens += decode_size;
    }
    //clear all resources
    env->ReleaseIntArrayElements(tokens_arrays, (jint *) tokens, 0);
    JLOG_DEBUG("Finish batch decoding, sequence id: %d.", sequence_id);
    return decode_status;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    clearCache
 */
JNIEXPORT void JNICALL Java_chat_octet_model_LlamaService_clearCache
        (JNIEnv *env, jclass thisClass, jint sequence_id, jint pos_start, jint pos_end) {
    if (Check_Context_Is_Null(env)) return;

    llama_kv_cache_seq_rm(main_ctx->llama_ctx, sequence_id, pos_start, pos_end);
    JLOG_DEBUG("KV cache removed, sequence id: %d, pos start: %d, pos end: %d.", sequence_id, pos_start, pos_end);
}

/*
 * Class:     chat_octet_model_LlamaServicen
 * Method:    llamaModelQuantize
 */
JNIEXPORT jint JNICALL Java_chat_octet_model_LlamaService_llamaModelQuantize
        (JNIEnv *env, jclass thisClass, jstring source_model_file_path, jstring output_model_file_path,
         jobject quantize_params) {

    struct llama_model_quantize_params params = {
            /*.nthread                     =*/ env->GetIntField(quantize_params, FIELD_THREAD),
            /*.ftype                       =*/ static_cast<enum llama_ftype>(env->GetIntField(quantize_params, FIELD_MODEL_FILE_TYPE)),
            /*.output_tensor_type          =*/ static_cast<enum ggml_type>(env->GetIntField(quantize_params, FIELD_OUTPUT_TENSOR_TYPE)),
            /*.token_embedding_type        =*/ static_cast<enum ggml_type>(env->GetIntField(quantize_params, FIELD_TOKEN_EMBEDDING_TYPE)),
            /*.allow_requantize            =*/ To_CBool(env->GetBooleanField(quantize_params, FIELD_ALLOW_REQUANTIZE)),
            /*.quantize_output_tensor      =*/ To_CBool(env->GetBooleanField(quantize_params, FIELD_QUANTIZE_OUTPUT_TENSOR)),
            /*.only_copy                   =*/ To_CBool(env->GetBooleanField(quantize_params, FIELD_ONLY_COPY)),
            /*.pure                        =*/ To_CBool(env->GetBooleanField(quantize_params, FIELD_PURE)),
            /*.keep_split                  =*/ To_CBool(env->GetBooleanField(quantize_params, FIELD_KEEP_SPLIT)),
            /*.imatrix                     =*/ nullptr,
            /*.kv_overrides                =*/ nullptr,
    };
    const char *fname_inp = env->GetStringUTFChars(source_model_file_path, JNI_FALSE);
    const char *fname_out = env->GetStringUTFChars(output_model_file_path, JNI_FALSE);
    int status = llama_model_quantize(fname_inp, fname_out, &params);
    JLOG_DEBUG("Quantize model finished, source model file path: %s, output model file path: %s, status: %d.",
               source_model_file_path, output_model_file_path, status);
    return status;
}

/*
 * Class:     chat_octet_model_LlamaService
 * Method:    llamaModelMeta
 */
JNIEXPORT jstring JNICALL Java_chat_octet_model_LlamaService_llamaModelMeta
        (JNIEnv *env, jclass thisClass, jstring data_key) {
    if (Check_Context_Is_Null(env)) return nullptr;

    std::vector<char> meta_data_buffer(2048, 0);
    const char *d_key = env->GetStringUTFChars(data_key, JNI_FALSE);
    int32_t res = llama_model_meta_val_str(main_ctx->model, d_key, meta_data_buffer.data(), meta_data_buffer.size());
    if (res <= 0) {
        JLOG_ERROR("Cannot find the meta data key: %s.", d_key);
        return nullptr;
    }
    std::string meta_data_str(meta_data_buffer.data(), meta_data_buffer.size());
    return env->NewStringUTF(meta_data_str.c_str());
}
