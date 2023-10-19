# ☕️ Java bindings for [`llama.cpp`](https://github.com/ggerganov/llama.cpp)


[![CI](https://github.com/eoctet/llama-java-core/actions/workflows/maven_build_deploy.yml/badge.svg)](https://github.com/eoctet/llama-java-core/actions/workflows/maven_build_deploy.yml)
[![Maven Central](https://img.shields.io/maven-central/v/chat.octet/llama-java-core?color=orange)](https://mvnrepository.com/artifact/chat.octet/llama-java-core)
[![README English](https://img.shields.io/badge/Lang-English-red)](./README.md)
[![Llama java chat](https://img.shields.io/badge/Github-Llama_Java_Chat-blue?logo=github)](https://github.com/eoctet/llama-java-chat.git)
[![GitHub](https://img.shields.io/github/license/eoctet/llama-java-core?color=green)](https://opensource.org/licenses/MIT)


这是一个基于 🦙[`llama.cpp`](https://github.com/ggerganov/llama.cpp)  API开发的Java库，目标是更快速将大语言模型的能力集成到Java生态，本项目和其他语言版本库具有一样的功能。

#### 主要特点
- 🦙 基于 `Llama.cpp` 构建。
- 🤖 支持 `并行推理`、`连续对话` 和 `文本生成`。
- 📦 支持 `Llama2` 系列模型和其他开源模型，例如：`Baichuan 7B`。
- ☕️ 使用 `JNI` 开发本地库，提供与 `Llama.cpp` 一致的接口。

#### 最近更新
  - [X] 🚀 自定义模型的提示词模版（例如：Vicuna、Alpaca等等）
  - [X] 🚀 并行批处理解码
  - [X] 🚀 Llama 语法解析


## 快速开始

#### Maven POM

```xml
<dependency>
    <groupId>chat.octet</groupId>
    <artifactId>llama-java-core</artifactId>
    <version>1.2.3</version>
</dependency>
```

#### Examples

- **Chat Console Example**

这里提供了一个简单的聊天示例，你也可以参考 🤖️ [**Llama-Java-Chat**](https://github.com/eoctet/llama-java-chat.git) 进一步丰富你的应用。

```java
public class ConsoleExample {
    private static final String MODEL_PATH = "/llama.cpp/models/llama2/ggml-model-7b-q6_k.gguf";

    public static void main(String[] args) {
        ModelParameter modelParams = ModelParameter.builder()
                .modelPath(MODEL_PATH)
                .threads(6)
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
                model.chat(generateParams, system, input).output();
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

- **Continuous Chat Example**

```java
public class ContinuousChatExample {

    private static final String MODEL_PATH = "/llama.cpp/models/llama2/ggml-model-7b-q6_k.gguf";

    public static void main(String[] args) {
        String system = "You are a helpful assistant. ";
        String[] questions = new String[]{
                "List five emojis about food and explain their meanings",
                "Write a fun story based on the third emoji",
                "Continue this story and refine it",
                "Summarize a title for this story, extract five keywords, and the keywords should not exceed five words",
                "Mark the characters, time, and location of this story",
                "Great, translate this story into Chinese",
                "Who are you and why are you here?",
                "Summarize today's conversation"
        };

        GenerateParameter generateParams = GenerateParameter.builder()
                .verbosePrompt(true)
                .user("William")
                .build();

        try (Model model = new Model(MODEL_PATH)) {
            for (String question : questions) {
                //Example 1: Continuous generation example.
                //String text = PromptBuilder.toPrompt(system, question);
                //model.generate(generateParams, text).output();

                //Example 2: Continuous chat example
                model.chat(generateParams, system, question).output();
                System.out.println("\n");
                model.metrics();
            }
        }
    }
}
```

> More examples: `chat.octet.examples.*`


## 开发手册

#### Components
  - `LogitsProcessor`
  - `StoppingCriteria`

可以使用 `LogitsProcessor` 和 `StoppingCriteria` 对模型推理过程进行自定义控制。

> 注：如果需要在Java中进行矩阵计算请使用 [`openblas`](https://github.com/bytedeco/javacpp-presets/tree/master/openblas)

**chat.octet.model.components.processor.LogitsProcessor**

自定义一个处理器对词的概率分布进行调整，控制模型推理的生成结果。这里是一个示例：[NoBadWordsLogitsProcessor.java](src%2Fmain%2Fjava%2Fchat%2Foctet%2Fmodel%2Fcomponents%2Fprocessor%2Fimpl%2FNoBadWordsLogitsProcessor.java)

```java
Map<Integer, String> logitBias = Maps.newLinkedHashMap();
logitBias.put(5546, "false");
logitBias.put(12113, "5.89");
LogitsProcessorList logitsProcessorList = new LogitsProcessorList(Lists.newArrayList(new CustomBiasLogitsProcessor(logitBias, model.getVocabSize())));

GenerateParameter generateParams = GenerateParameter.builder()
        .logitsProcessorList(logitsProcessorList)
        .build();

...

```

**chat.octet.model.components.criteria.StoppingCriteria**

自定义一个控制器实现对模型推理的停止规则控制，例如：控制生成最大超时时间，这里是一个示例：[MaxTimeCriteria](src%2Fmain%2Fjava%2Fchat%2Foctet%2Fmodel%2Fcomponents%2Fcriteria%2Fimpl%2FMaxTimeCriteria.java)

```java
long maxTime = TimeUnit.MINUTES.toMillis(Optional.ofNullable(params.getTimeout()).orElse(10L));
StoppingCriteriaList stopCriteriaList = new StoppingCriteriaList(Lists.newArrayList(new MaxTimeCriteria(maxTime)));

GenerateParameter generateParams = GenerateParameter.builder()
        .stoppingCriteriaList(stopCriteriaList)
        .build();

...

```

> 完整的文档请参考 [Java docs](docs%2Fapidocs%2Findex.html)

#### 如何编译

默认已包含各系统版本库，可以直接使用。

> 如果需要支持`GPU`或更加灵活的编译方式，请参考 `llama.cpp` **Build** 文档。

```ini
# 加载外部库文件

-Doctet.llama.lib=<YOUR_LIB_PATH>
```

## Why Java

没有特殊理由，我希望这不是粗燥的API封装，在此基础之上做一些优化和扩展，使其能够更好地移植到Java生态。


## 问题反馈

- 如果你有任何疑问，欢迎在GitHub Issue中提交。


