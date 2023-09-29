# ☕️ Java bindings for [`llama.cpp`](https://github.com/ggerganov/llama.cpp)

[**🇨🇳中文**](./README.Zh_CN.md) | [**🌐English**](./README.md) | 🤖 [**Llama-Java-Chat**](https://github.com/eoctet/llama-java-chat.git)

Another simple Java bindings for 🦙 [**llama.cpp**](https://github.com/ggerganov/llama.cpp), The goal is to integrate the capabilities of LLMs into the Java ecosystem, this project has the same functionality as other language versions.

#### Main content
- 🚀 Built based on Llama.cpp, For more details, please follow **@ggerganov's** [`llama.cpp`](https://github.com/ggerganov/llama.cpp).
- 🚀 Developed using JNI, ~~NOT JNA~~.
- 🚀 News:
  - [X] Conversation Memory.


## Quick start

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

Here is a simple chat example, and you can also refer to another project 🤖️ [**Llama-Java-Chat**](https://github.com/eoctet/llama-java-chat.git) to further enrich your application.

```java
public class ConsoleExample {
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

## Development

#### Customize inference

- **Components**
  - LogitsProcessor
  - StoppingCriteria

You can use `LogitsProcessor` and `StoppingCriteria` to customize and control the model inference process.

> Note: If you need to do matrix calculations in Java, please use [`openblas`](https://github.com/bytedeco/javacpp-presets/tree/master/openblas)

**chat.octet.model.components.processor.LogitsProcessor**

Customize a processor to adjust the probability distribution of words and control the generation of model inference results. Here is an example: [NoBadWordsLogitsProcessor.java](src%2Fmain%2Fjava%2Fchat%2Foctet%2Fmodel%2Fcomponents%2Fprocessor%2Fimpl%2FNoBadWordsLogitsProcessor.java)

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

Customize a controller to implement stop rule control for model inference, such as controlling the maximum timeout time generated. Here is an example: [MaxTimeCriteria](src%2Fmain%2Fjava%2Fchat%2Foctet%2Fmodel%2Fcomponents%2Fcriteria%2Fimpl%2FMaxTimeCriteria.java)

```java
    long maxTime = TimeUnit.MINUTES.toMillis(Optional.ofNullable(params.getTimeout()).orElse(10L));
    StoppingCriteriaList stopCriteriaList = new StoppingCriteriaList(Lists.newArrayList(new MaxTimeCriteria(maxTime)));

    GenerateParameter generateParams = GenerateParameter.builder()
            .stoppingCriteriaList(stopCriteriaList)
            .build();
    
    ... ...

```

#### [LlamaService](src%2Fmain%2Fjava%2Fchat%2Foctet%2Fmodel%2FLlamaService.java)

Develop using JNI:

- Same interface as the original project.
- Optimize JVM Native performance.

> `LlamaService.sampling(...)` sampling has been optimized to reduce performance losses caused by data transfer between JVM Native.
>
>
> More information: [`Java Docs`](docs/API.md)

#### Build

By default, each system version library is included.

> If you need to support `GPU` or more flexible compilation methods, please refer to `llama.cpp`

```ini
# (Optional) Load the external library file

-Doctet.llama.lib=<YOUR_LIB_PATH>
```

## Feedback

- If you have any questions, please submit them in GitHub Issue.

----

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)
