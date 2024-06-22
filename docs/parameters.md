## Llama Java parameters

The following is a list of all the parameters involved in this project.

> [!NOTE]
> Other reference
>
documents: <a href="https://huggingface.co/docs/transformers/main_classes/text_generation#transformers.GenerationConfig">
> Transformers docs</a>.

### Model parameters

- **Basic parameters**

| Parameter     | Default | Description                                                                                                                           |
|---------------|---------|---------------------------------------------------------------------------------------------------------------------------------------|
| model_path    | /       | Llama model path.                                                                                                                     |
| lora_base     | /       | Optional model to use as a base for the layers modified by the LoRA adapter.                                                          |
| lora_path     | /       | Apply a LoRA (Low-Rank Adaptation) adapter to the model (implies --no-mmap).                                                          |
| lora_scale    | 0.0     | apply LoRA adapter with user defined scaling S (implies --no-mmap).                                                                   |
| verbose       | false   | Print verbose output to stderr.                                                                                                       |
| numa_strategy | 0       | Attempt one of the below optimization strategies that may help on some NUMA systems (default: disabled). `enum` **LlamaNumaStrategy** |

- **Context parameters**

| Parameter         | Default | Description                                                                                              |
|-------------------|---------|----------------------------------------------------------------------------------------------------------|
| seed              | -1      | Set the random number generator seed.                                                                    |
| context_size      | 512     | Option allows you to set the size of the prompt context used by the LLaMA models during text generation. |
| batch_size        | 2048    | Set the batch size for prompt processing.                                                                |
| ubatch            | 512     | Physical maximum batch size (default: 512).                                                              |
| seq_max           | 1       | Max number of sequences (default: 1).                                                                    |
| threads           | 4       | Set the number of threads used for generation (single token).                                            |
| threads_batch     | 4       | Set the number of threads used for prompt and batch processing (multiple tokens).                        |
| rope_scaling_type | -1      | RoPE scaling type. `enum` **LlamaRoPEScalingType**                                                       |
| pooling_type      | -1      | Pooling type for embeddings. `enum` **LlamaPoolingType**                                                 |
| rope_freq_base    | 0.0     | Base frequency for RoPE sampling.                                                                        |
| rope_freq_scale   | 0.0     | Scale factor for RoPE sampling.                                                                          |
| yarn_ext_factor   | -1.0    | YaRN extrapolation mix factor, NaN = from model.                                                         |
| yarn_attn_factor  | 1.0     | YaRN magnitude scaling factor.                                                                           |
| yarn_beta_fast    | 32.0    | YaRN low correction dim.                                                                                 |
| yarn_beta_slow    | 1.0     | YaRN high correction dim.                                                                                |
| yarn_orig_ctx     | 0       | YaRN original context size.                                                                              |
| defrag_thold      | -1.0    | KV cache defragmentation threshold (default: -1.0, < 0 = disabled).                                      |
| logits_all        | false   | Return logits for all tokens, not just the last token.                                                   |
| embedding         | false   | Embedding mode only.                                                                                     |
| offload_kqv       | true    | Whether to offload the KQV ops (including the KV cache) to GPU.                                          |
| flash_attn        | false   | Enable flash attention (default: disabled).                                                              |

- **Model parameters**

| Parameter     | Default | Description                                                                                                                                                              |
|---------------|---------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| gpu_layers    | 0       | Number of layers to offload to GPU (-ngl). If -1, all layers are offloaded.                                                                                              |
| split_mode    | 1       | How to split the model across multiple GPUs. `enum` **LlamaSplitMode**                                                                                                   |
| main_gpu      | /       | When using multiple GPUs this option controls which GPU is used for small tensors for which the overhead of splitting the computation across all GPUs is not worthwhile. |
| tensor_split  | /       | When using multiple GPUs this option controls how large tensors should be split across all GPUs.                                                                         |
| vocab_only    | false   | Only load the vocabulary no weights.                                                                                                                                     |
| mmap          | true    | Use mmap if possible (slower load but may reduce pageouts if not using mlock).                                                                                           |
| mlock         | false   | Lock the model in memory, preventing it from being swapped out when memory-mapped.                                                                                       |
| check_tensors | false   | Validate model tensor data (default: disabled).                                                                                                                          |

**JSON template**

```json
{
  "model_path": "",
  "lora_base": "",
  "lora_path": "",
  "lora_scale": 0.0,
  "verbose": false,
  "numa_strategy": 0,
  "seed": -1,
  "context_size": 512,
  "batch_size": 2048,
  "ubatch": 512,
  "seq_max": 1,
  "threads": 4,
  "threads_batch": 4,
  "rope_scaling_type": -1,
  "pooling_type": -1,
  "rope_freq_base": 0.0,
  "rope_freq_scale": 0.0,
  "yarn_ext_factor": -1.0,
  "yarn_attn_factor": 1.0,
  "yarn_beta_fast": 32.0,
  "yarn_beta_slow": 1.0,
  "yarn_orig_ctx": 0,
  "defrag_thold": -1.0,
  "logits_all": false,
  "embedding": false,
  "offload_kqv": true,
  "flash_attn": false,
  "gpu_layers": 0,
  "split_mode": 1,
  "main_gpu": 0,
  "tensor_split": [],
  "vocab_only": false,
  "mmap": true,
  "mlock": false,
  "check_tensors": false
}
```

### Generate parameters

| Parameter          | Default   | Description                                                                                                                                                                  |
|--------------------|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| temperature        | 0.8       | Adjust the randomness of the generated text.                                                                                                                                 |
| repeat_penalty     | 1.1       | Control the repetition of token sequences in the generated text.                                                                                                             |
| penalize_nl        | true      | Disable penalization for newline tokens when applying the repeat penalty.                                                                                                    |
| frequency_penalty  | 0.0       | Repeat alpha frequency penalty.                                                                                                                                              |
| presence_penalty   | 0.0       | Repeat alpha presence penalty.                                                                                                                                               |
| top_k              | 40        | **TOP-K Sampling** Limit the next token selection to the K most probable tokens.                                                                                             |
| top_p              | 0.9       | **TOP-P Sampling** Limit the next token selection to a subset of tokens with a cumulative probability above a threshold P.                                                   |
| tsf                | 1.0       | **Tail Free Sampling (TFS)** Enable tail free sampling with parameter z.                                                                                                     |
| typical            | 1.0       | **Typical Sampling** Enable typical sampling sampling with parameter p.                                                                                                      |
| min_p              | 0.05      | **Min P Sampling** Sets a minimum base probability threshold for token selection.                                                                                            |
| mirostat_mode      | DISABLED  | **Mirostat Sampling** Enable Mirostat sampling, controlling perplexity during text generation. `DISABLED = disabled`, `V1 = Mirostat`, `V2 = Mirostat 2.0`                   |
| mirostat_eta       | 0.1       | **Mirostat Sampling** Set the Mirostat learning rate, parameter eta.                                                                                                         |
| mirostat_tau       | 5.0       | **Mirostat Sampling** Set the Mirostat target entropy, parameter tau.                                                                                                        |
| dynatemp_range     | 0.0       | **Dynamic Temperature Sampling** Dynamic temperature range. The final temperature will be in the range of (temperature - dynatemp_range) and (temperature + dynatemp_range). |
| dynatemp_exponent  | 1.0       | **Dynamic Temperature Sampling** Dynamic temperature exponent.                                                                                                               |
| grammar_rules      | /         | Specify a grammar (defined inline or in a file) to constrain model output to a specific format.                                                                              |
| max_new_token_size | 512       | Maximum new token generation size.                                                                                                                                           |
| verbose_prompt     | false     | Print the prompt before generating text.                                                                                                                                     |
| last_tokens_size   | 64        | Maximum number of tokens to keep in the last_n_tokens deque.                                                                                                                 |
| special            | false     | If true, special tokens are rendered in the output.                                                                                                                          |
| logit_bias         | /         | Adjust the probability distribution of words.                                                                                                                                |
| stopping_word      | /         | Control the stop word list for generating stops, with values that can be text or token IDs.                                                                                  |
| infill             | false     | Enable infill mode for the model.                                                                                                                                            |
| spm_fill           | false     | Use Suffix/Prefix/Middle pattern for infill (instead of Prefix/Suffix/Middle) as some models prefer this.                                                                    |
| prefix_token       | /         | Specify a prefix token in fill mode. (If not specified, read from the model by default)                                                                                      |
| suffix_token       | /         | Specify a suffix token in fill mode.                                                                                                                                         |
| middle_token       | /         | Specify a middle token in fill mode.                                                                                                                                         |
| session_cache      | false     | If enabled, each chat conversation will be stored in the session cache.                                                                                                      |
| prompt_cache       | false     | Cache the system prompt in the session and does not update them again.                                                                                                       |
| user               | User      | Specify user nickname.                                                                                                                                                       |
| assistant          | Assistant | Specify bot nickname.                                                                                                                                                        |

**JSON template**

```json
{
  "temperature": 0.8,
  "repeat_penalty": 1.1,
  "penalize_nl": true,
  "frequency_penalty": 0.0,
  "presence_penalty": 0.0,
  "top_k": 40,
  "top_p": 0.9,
  "tsf": 1.0,
  "typical": 1.0,
  "min_p": 0.05,
  "mirostat_mode": "DISABLED",
  "mirostat_eta": 0.1,
  "mirostat_tau": 5.0,
  "dynatemp_range": 0.0,
  "dynatemp_exponent": 1.0,
  "grammar_rules": null,
  "max_new_token_size": 512,
  "verbose_prompt": false,
  "last_tokens_size": 64,
  "special": false,
  "logit_bias": null,
  "stopping_word": null,
  "infill": false,
  "spm_fill": false,
  "prefix_token": "",
  "suffix_token": "",
  "middle_token": "",
  "session_cache": false,
  "prompt_cache": false,
  "user": "User",
  "assistant": "Assistant"
}
```