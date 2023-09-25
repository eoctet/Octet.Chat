# ☕️ Java bindings for [`llama.cpp`](https://github.com/ggerganov/llama.cpp)

[**🇨🇳中文**](./README.Zh_CN.md) | [**🌐English**](./README.md) | 🤖 [**Llama-Java-Chat**](https://github.com/eoctet/llama-java-chat.git) 

这是一个基于 🦙[`llama.cpp`](https://github.com/ggerganov/llama.cpp)  API开发的Java库，目标是更快速将大语言模型的能力集成到Java生态，本项目和其他语言版本库具有一样的功能。

#### 主要功能
- 🚀 基于 Llama.cpp 构建，更多细节请关注 **@ggerganov's** [`llama.cpp`](https://github.com/ggerganov/llama.cpp)。
- 🚀 使用JNI开发本地库，~~而不是JNA~~，测试的性能上与其他库无异。
- 🚀 新增:
  - [X] 多用户会话，你可以使用不同的用户身份进行聊天 (Beta)。


## 快速开始

#### Maven POM

```xml
    <dependency>
        <groupId>chat.octet</groupId>
        <artifactId>llama-java-core</artifactId>
        <version>1.1.0</version>
    </dependency>
```

#### ConsoleQA

这里提供了一个简单的聊天示例，你也可以参考 🤖️ [**Llama-Java-Chat**](https://github.com/eoctet/llama-java-chat.git) 进一步丰富你的应用。

```java
public class ConsoleQA {
    private static final String MODEL_PATH = "/llama.cpp/models/llama2/ggml-model-7b-q6_k.gguf";

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
                String question = PromptBuilder.toPrompt(system, input);
                model.generate(generateParams, question).forEach(e -> System.out.print(e.getText()));
                model.printTimings();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e);
            System.exit(1);
        }
    }
}
```

## 开发手册

#### 自定义推理

可以使用 `LogitsProcessor` 和 `StoppingCriteria` 对模型推理过程进行自定义控制。

> 注：如果需要在Java中进行矩阵计算请使用 [`openblas`](https://github.com/bytedeco/javacpp-presets/tree/master/openblas)

**chat.octet.model.processor.LogitsProcessor**

自定义一个处理器对词的概率分布进行调整，控制模型推理的生成结果。这里是一个示例：[NoBadWordsLogitsProcessor](src%2Fmain%2Fjava%2Fchat%2Foctet%2Fmodel%2Fprocessor%2Fimpl%2FNoBadWordsLogitsProcessor.java)

```java
    Map<Integer, String> logitBias = Maps.newLinkedHashMap();
    logitBias.put(5546, "false");
    logitBias.put(12113, "5.89");
    LogitsProcessorList logitsProcessorList = new LogitsProcessorList(Lists.newArrayList(new CustomBiasLogitsProcessor(logitBias, model.getVocabSize())));
    
    ModelParameter modelParams = ModelParameter.builder()
            .logitsProcessorList(logitsProcessorList)
            .build();

    ... ...

```

**chat.octet.model.criteria.StoppingCriteria**

自定义一个控制器实现对模型推理的停止规则控制，例如：控制生成最大超时时间，这里是一个示例：[MaxTimeCriteria](src%2Fmain%2Fjava%2Fchat%2Foctet%2Fmodel%2Fcriteria%2Fimpl%2FMaxTimeCriteria.java)

```java
    long maxTime = TimeUnit.MINUTES.toMillis(Optional.ofNullable(params.getTimeout()).orElse(10L));
    StoppingCriteriaList stopCriteriaList = new StoppingCriteriaList(Lists.newArrayList(new MaxTimeCriteria(maxTime)));
    
    ModelParameter modelParams = ModelParameter.builder()
            .stoppingCriteriaList(stopCriteriaList)
            .build();

    ... ...

```

#### 多用户会话（Beta）

语言模型本身是无状态的，当多个用户同时进行聊天时，语言模型会记忆混乱。
因此，我增加了多用户会话的功能支持，目前这是一个实验性的功能，欢迎提交Issue。

- 使用 [UserContextManager](src%2Fmain%2Fjava%2Fchat%2Foctet%2Fmodel%2FUserContextManager.java) 创建用户会话、删除用户会话；

- 会话上下文窗口长度为 `Model.contextSize`（默认值：512），当达到窗口长度时，保留最近 `keepContextTokensSize` 个词汇的对话历史。

#### [LlamaService](src%2Fmain%2Fjava%2Fchat%2Foctet%2Fmodel%2FLlamaService.java)

使用JNI开发，开放与原项目相同的接口并优化JVM Native性能。

> `LlamaService.samplingXxxx(...)` 对采样进行了优化，以减少JVM Native之间数据传递带来的性能损失。
>
>
> 完整的文档请参考[`Java Docs`](docs/API.md)。

#### 如何编译

默认已包含各系统版本库，可以直接使用。

> 如果需要支持`GPU`或更加灵活的编译方式，请参考 `llama.cpp` **Build** 文档。



## 问题反馈

- 如果你有任何疑问，欢迎在GitHub Issue中提交。

----

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)
