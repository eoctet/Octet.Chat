## Llama Java parameters

The following is a list of all the parameters involved in this project.

> [!NOTE]
> Other reference documents: <a href="https://huggingface.co/docs/transformers/main_classes/text_generation#transformers.GenerationConfig">Transformers docs</a>.

### Model parameter

| Parameter         | Default | Description                                                                                                                                                               |
|-------------------|---------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| model_path        | /       | Llama model path.                                                                                                                                                         |
| model_name        | /       | Llama model name.                                                                                                                                                         |
| model_type        | LLAMA2  | Llama model type `chat.octet.model.enums.ModelType`.                                                                                                                      |
| context_size      | 512     | option allows you to set the size of the prompt context used by the LLaMA models during text generation.                                                                  |
| main_gpu          | /       | When using multiple GPUs this option controls which GPU is used for small tensors for which the overhead of  splitting the computation across all GPUs is not worthwhile. |
| gpu_layers        | 0       | Number of layers to offload to GPU (-ngl). If -1, all layers are offloaded.                                                                                               |
| split_mode        | 1       | How to split the model across multiple GPUs. `0 = single GPU`, `1 = split layers and KV across GPUs`, `2 = split rows across GPUs`                                        |
| seed              | -1      | Set the random number generator seed.                                                                                                                                     |
| logits_all        | false   | Return logits for all tokens, not just the last token.                                                                                                                    |
| vocab_only        | false   | Only load the vocabulary no weights.                                                                                                                                      |
| mmap              | true    | use mmap if possible (slower load but may reduce pageouts if not using mlock).                                                                                            |
| mlock             | false   | Lock the model in memory, preventing it from being swapped out when memory-mapped.                                                                                        |
| embedding         | false   | Embedding mode only.                                                                                                                                                      |
| threads           | 4       | Set the number of threads used for generation (single token).                                                                                                             |
| threads_batch     | 4       | Set the number of threads used for prompt and batch processing (multiple tokens).                                                                                         |
| batch_size        | 512     | Set the batch size for prompt processing.                                                                                                                                 |
| last_tokens_size  | 64      | Maximum number of tokens to keep in the last_n_tokens deque.                                                                                                              |
| lora_base         | /       | Optional model to use as a base for the layers modified by the LoRA adapter.                                                                                              |
| lora_path         | /       | Apply a LoRA (Low-Rank Adaptation) adapter to the model (implies --no-mmap).                                                                                              |
| lora_scale        | 0.0     | apply LoRA adapter with user defined scaling S (implies --no-mmap).                                                                                                       |
| tensor_split      | /       | When using multiple GPUs this option controls how large tensors should be split across all GPUs.                                                                          |
| rope_freq_base    | 0.0     | Base frequency for RoPE sampling.                                                                                                                                         |
| rope_freq_scale   | 0.0     | Scale factor for RoPE sampling.                                                                                                                                           |
| gqa               | /       | Grouped-query attention. Must be 8 for llama-2 70b.                                                                                                                       |
| rms_norm_eps      | /       | default is 1e-5, 5e-6 is a good value for llama-2 models.                                                                                                                 |
| mul_mat_q         | true    | If true, use experimental mul_mat_q kernels.                                                                                                                              |
| verbose           | false   | Print verbose output to stderr.                                                                                                                                           |
| rope_scaling_type | -1      | RoPE scaling type, from `enum llama_rope_scaling_type`                                                                                                                    |
| yarn_ext_factor   | -1.0    | YaRN extrapolation mix factor, NaN = from model.                                                                                                                          |
| yarn_attn_factor  | 1.0     | YaRN magnitude scaling factor.                                                                                                                                            |
| yarn_beta_fast    | 32.0    | YaRN low correction dim.                                                                                                                                                  |
| yarn_beta_slow    | 1.0     | YaRN high correction dim.                                                                                                                                                 |
| yarn_orig_ctx     | 0       | YaRN original context size.                                                                                                                                               |
| offload_kqv       | true    | whether to offload the KQV ops (including the KV cache) to GPU.                                                                                                           |

**JSON template**

```json
{
  "model_path": null,
  "model_name": null,
  "model_type": "LLAMA2",
  "context_size": 512,
  "main_gpu": null,
  "gpu_layers": 0,
  "split_mode": 1,
  "seed": -1,
  "logits_all": false,
  "vocab_only": false,
  "mmap": true,
  "mlock": false,
  "embedding": false,
  "threads": 4,
  "threads_batch": 4,
  "batch_size": 512,
  "last_tokens_size": 64,
  "lora_base": null,
  "lora_path": null,
  "lora_scale": 0.0,
  "tensor_split": null,
  "rope_freq_base": 0.0,
  "rope_freq_scale": 0.0,
  "gqa": null,
  "rms_norm_eps": null,
  "mul_mat_q": true,
  "verbose": false,
  "rope_scaling_type": -1,
  "yarn_ext_factor": -1.0,
  "yarn_attn_factor": 1.0,
  "yarn_beta_fast": 32.0,
  "yarn_beta_slow": 1.0,
  "yarn_orig_ctx": 0,
  "offload_kqv": true
}
```

### Generate parameter

| Parameter              | Default   | Description                                                                                                                                                |
|------------------------|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| temperature            | 0.8       | Adjust the randomness of the generated text.                                                                                                               |
| repeat_penalty         | 1.1       | Control the repetition of token sequences in the generated text.                                                                                           |
| penalize_nl            | true      | Disable penalization for newline tokens when applying the repeat penalty.                                                                                  |
| frequency_penalty      | 0.0       | Repeat alpha frequency penalty.                                                                                                                            |
| presence_penalty       | 0.0       | Repeat alpha presence penalty.                                                                                                                             |
| top_k                  | 40        | **TOP-K Sampling** Limit the next token selection to the K most probable tokens.                                                                           |
| top_p                  | 0.9       | **TOP-P Sampling** Limit the next token selection to a subset of tokens with a cumulative probability above a threshold P.                                 |
| tsf                    | 1.0       | **Tail Free Sampling (TFS)** Enable tail free sampling with parameter z.                                                                                   |
| typical                | 1.0       | **Typical Sampling** Enable typical sampling sampling with parameter p.                                                                                    |
| min_p                  | 0.05      | **Min P Sampling** Sets a minimum base probability threshold for token selection.                                                                          |
| mirostat_mode          | DISABLED  | **Mirostat Sampling** Enable Mirostat sampling, controlling perplexity during text generation. `DISABLED = disabled`, `V1 = Mirostat`, `V2 = Mirostat 2.0` |
| mirostat_eta           | 0.1       | **Mirostat Sampling** Set the Mirostat learning rate, parameter eta.                                                                                       |
| mirostat_tau           | 5.0       | **Mirostat Sampling** Set the Mirostat target entropy, parameter tau.                                                                                      |
| grammar_rules          | /         | Specify a grammar (defined inline or in a file) to constrain model output to a specific format.                                                            |
| max_new_token_size     | 0         | Maximum new token generation size.                                                                                                                         |
| verbose_prompt         | false     | Print the prompt before generating text.                                                                                                                   |
| logits_processor_list  | /         | logits processor list.                                                                                                                                     |
| stopping_criteria_list | /         | stopping criteria list.                                                                                                                                    |
| user                   | User      | Specify user nickname.                                                                                                                                     |
| assistant              | Assistant | Specify bot nickname.                                                                                                                                      |

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
  "grammar_rules": null,
  "max_new_token_size": 0,
  "verbose_prompt": false,
  "logits_processor_list": null,
  "stopping_criteria_list": null,
  "user": "User",
  "assistant": "Assistant",
  "last_tokens_size": 64
}
```