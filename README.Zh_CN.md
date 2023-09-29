# ☕️ Java bindings for [`llama.cpp`](https://github.com/ggerganov/llama.cpp)

[**🇨🇳中文**](./README.Zh_CN.md) | [**🌐English**](./README.md) | 🤖 [**Llama-Java-Chat**](https://github.com/eoctet/llama-java-chat.git) 

这是一个基于 🦙[`llama.cpp`](https://github.com/ggerganov/llama.cpp)  API开发的Java库，目标是更快速将大语言模型的能力集成到Java生态，本项目和其他语言版本库具有一样的功能。

#### 主要功能
- 🚀 基于 Llama.cpp 构建，更多细节请关注 **@ggerganov's** [`llama.cpp`](https://github.com/ggerganov/llama.cpp)。
- 🚀 使用JNI开发本地库，~~而不是JNA~~，测试的性能上与其他库无异。
- 🚀 新增:
  - [X] 对话历史记忆。


## 快速开始

#### Maven POM

```xml
    <dependency>
        <groupId>chat.octet</groupId>
        <artifactId>llama-java-core</artifactId>
        <version>1.1.2</version>
    </dependency>
```

#### Examples

- **Chat Console Example**

这里提供了一个简单的聊天示例，你也可以参考 🤖️ [**Llama-Java-Chat**](https://github.com/eoctet/llama-java-chat.git) 进一步丰富你的应用。

```java
public class ConsoleExample {
    private static final String MODEL_PATH = "/Users/william/development/llm/tools/zh-models/chinese-alpaca-2-7b/ggml-model-7b-q6_k.gguf";

    public static void main(String[] args) {
        ModelParameter modelParams = ModelParameter.builder()
                .modelPath(MODEL_PATH)
                .threads(8)
                .contextSize(4096)
                .verbose(true)
                .build();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
             Model model = new Model(modelParams)) {

            GenerateParameter generateParams = GenerateParameter.builder().build();
            String system = "Answer the questions.";

            while (true) {
                System.out.print("\nQuestion: ");
                String input = bufferedReader.readLine();
                if (StringUtils.trimToEmpty(input).equalsIgnoreCase("exit")) {
                    break;
                }
                model.chat(generateParams, system, input).forEach(e -> System.out.print(e.getText()));
                System.out.print("\n");
                model.metrics();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e);
            System.exit(1);
        }
    }
}
```

- **Chat Completions Example**

```java
public class ChatCompletionsExample {
    private static final String MODEL_PATH = "/llama.cpp/models/llama2/ggml-model-7b-q6_k.gguf";

    public static void main(String[] args) {
        GenerateParameter generateParams = GenerateParameter.builder().build();

        try (Model model = new Model(MODEL_PATH)) {
            CompletionResult result = model.chatCompletions(generateParams, "Who are you?");
            System.out.println(result);
        }
    }
}
```

- **Completions Example**

```java
public class CompletionsExample {
    private static final String MODEL_PATH = "/llama.cpp/models/llama2/ggml-model-7b-q6_k.gguf";

    public static void main(String[] args) {
        GenerateParameter generateParams = GenerateParameter.builder().build();

        try (Model model = new Model(MODEL_PATH)) {
            CompletionResult result = model.completions(generateParams, "long time a ago");
            System.out.println(result);
        }
    }
}
```

## 开发手册

#### 自定义推理

- **Components**
  - LogitsProcessor
  - StoppingCriteria

可以使用 `LogitsProcessor` 和 `StoppingCriteria` 对模型推理过程进行自定义控制。

> 注：如果需要在Java中进行矩阵计算请使用 [`openblas`](https://github.com/bytedeco/javacpp-presets/tree/master/openblas)

**chat.octet.model.components.processor.LogitsProcessor**

自定义一个处理器对词的概率分布进行调整，控制模型推理的生成结果。这里是一个示例：[NoBadWordsLogitsProcessor](src%2Fmain%2Fjava%2Fchat%2Foctet%2Fmodel%2Fprocessor%2Fimpl%2FNoBadWordsLogitsProcessor.java)

```java
    Map<Integer, String> logitBias = Maps.newLinkedHashMap();
    logitBias.put(5546, "false");
    logitBias.put(12113, "5.89");
    LogitsProcessorList logitsProcessorList = new LogitsProcessorList(Lists.newArrayList(new CustomBiasLogitsProcessor(logitBias, model.getVocabSize())));

    GenerateParameter generateParams = GenerateParameter.builder()
            .logitsProcessorList(logitsProcessorList)
            .build();

    ... ...

```

**chat.octet.model.components.criteria.StoppingCriteria**

自定义一个控制器实现对模型推理的停止规则控制，例如：控制生成最大超时时间，这里是一个示例：[MaxTimeCriteria](src%2Fmain%2Fjava%2Fchat%2Foctet%2Fmodel%2Fcriteria%2Fimpl%2FMaxTimeCriteria.java)

```java
    long maxTime = TimeUnit.MINUTES.toMillis(Optional.ofNullable(params.getTimeout()).orElse(10L));
    StoppingCriteriaList stopCriteriaList = new StoppingCriteriaList(Lists.newArrayList(new MaxTimeCriteria(maxTime)));

    GenerateParameter generateParams = GenerateParameter.builder()
            .stoppingCriteriaList(stopCriteriaList)
            .build();

    ... ...

```

#### [LlamaService](src%2Fmain%2Fjava%2Fchat%2Foctet%2Fmodel%2FLlamaService.java)

使用JNI开发，开放与原项目相同的接口并优化JVM Native性能。

> `LlamaService.sampling(...)` 对采样进行了优化，以减少JVM Native之间数据传递带来的性能损失。
>
>
> 完整的文档请参考[`Java Docs`](docs/API.md)。

#### 如何编译

默认已包含各系统版本库，可以直接使用。

> 如果需要支持`GPU`或更加灵活的编译方式，请参考 `llama.cpp` **Build** 文档。

```ini
# 加载外部库文件

-Doctet.llama.lib=<YOUR_LIB_PATH>
```


## 问题反馈

- 如果你有任何疑问，欢迎在GitHub Issue中提交。

----

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)
