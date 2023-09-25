# ☕️ Java bindings for [`llama.cpp`](https://github.com/ggerganov/llama.cpp)

[**🇨🇳中文**](./README.Zh_CN.md) | [**🌐English**](./README.md) | 🤖 [**Llama-Java-Chat**](https://github.com/eoctet/llama-java-chat.git)

Another simple Java bindings for 🦙 [**llama.cpp**](https://github.com/ggerganov/llama.cpp), The goal is to integrate the capabilities of LLMs into the Java ecosystem, this project has the same functionality as other language versions.

#### Main content
- 🚀 Built based on Llama.cpp, For more details, please follow **@ggerganov's** [`llama.cpp`](https://github.com/ggerganov/llama.cpp).
- 🚀 Developed using JNI, ~~NOT JNA~~.
- 🚀 News:
  - [X] Multi-user sessions (Beta).


## Quick start

#### Maven POM

```xml
    <dependency>
        <groupId>chat.octet</groupId>
        <artifactId>llama-java-core</artifactId>
        <version>1.1.0</version>
    </dependency>
```

#### ConsoleQA

Here is a simple chat example, and you can also refer to another project 🤖️ [**Llama-Java-Chat**](https://github.com/eoctet/llama-java-chat.git) to further enrich your application.

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

## Development

#### Customize inference

You can use `LogitsProcessor` and `StoppingCriteria` to customize and control the model inference process.

- Note: If you need to do matrix calculations in Java, please use [`openblas`](https://github.com/bytedeco/javacpp-presets/tree/master/openblas)

**chat.octet.model.processor.LogitsProcessor**

Customize a processor to adjust the probability distribution of words and control the generation of model inference results. Here is an example: [NoBadWordsLogitsProcessor](src%2Fmain%2Fjava%2Fchat%2Foctet%2Fmodel%2Fprocessor%2Fimpl%2FNoBadWordsLogitsProcessor.java)

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

Customize a controller to implement stop rule control for model inference, such as controlling the maximum timeout time generated. Here is an example: [MaxTimeCriteria](src%2Fmain%2Fjava%2Fchat%2Foctet%2Fmodel%2Fcriteria%2Fimpl%2FMaxTimeCriteria.java)

```java
    long maxTime = TimeUnit.MINUTES.toMillis(Optional.ofNullable(params.getTimeout()).orElse(10L));
    StoppingCriteriaList stopCriteriaList = new StoppingCriteriaList(Lists.newArrayList(new MaxTimeCriteria(maxTime)));
    
    ModelParameter modelParams = ModelParameter.builder()
            .stoppingCriteriaList(stopCriteriaList)
            .build();
    
    ... ...

```

#### Multi-user Session (Beta)

The language model is stateless, and when multiple users are chatting at the same time, the language model will experience memory confusion.
So, I have added support for multi-user sessions, which is currently an experimental feature. Welcome to submit an issue.

- This is user context manager: [UserContextManager](src%2Fmain%2Fjava%2Fchat%2Foctet%2Fmodel%2FUserContextManager.java) 

- The session context window length is `Model.contextSize` (default: 512). When the window length is reached, the conversation history of the most recent `keepContextTokensSize` tokens is retained.

#### [LlamaService](src%2Fmain%2Fjava%2Fchat%2Foctet%2Fmodel%2FLlamaService.java)

Develop using JNI:

- Same interface as the original project.
- Optimize JVM Native performance.

> `LlamaService.samplingXxxx(...)` sampling has been optimized to reduce performance losses caused by data transfer between JVM Native.
>
>
> More information: [`Java Docs`](docs/API.md)

#### Build

By default, each system version library is included.

> If you need to support `GPU` or more flexible compilation methods, please refer to `llama.cpp`


----


## Feedback

- If you have any questions, please submit them in GitHub Issue.

----

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)
