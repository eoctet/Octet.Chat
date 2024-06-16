## Quick start

Create your own project and use `Maven` or `Gradle` to import the llama-java-core framework.

> For the latest version, check out GitHub Release or search the Maven repository.


__Maven__

```xml
<dependency>
    <groupId>chat.octet</groupId>
    <artifactId>llama-java-core</artifactId>
    <version>LAST_RELEASE_VERSION</version>
</dependency>
```

__Gradle__

```txt
implementation group: 'chat.octet', name: 'llama-java-core', version: 'LAST_RELEASE_VERSION'
```

__Examples__

- **Chat Console Example**

Here is a simple chat example.

```java
public class ConsoleExample {
    private static final String MODEL_PATH = "/octet-chat/models/llama2/ggml-model-7b-q6_k.gguf";

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

    private static final String MODEL_PATH = "/octet-chat/models/llama2/ggml-model-7b-q6_k.gguf";

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
                //Example: Continuous chat example
                model.chat(generateParams, system, question).output();
                System.out.println("\n");
                model.metrics();
            }
        }
    }
}
```

> [!TIP]
>
> More examples: `chat.octet.examples.*`


## Inference components

- `LogitsProcessor`
- `StoppingCriteria`

You can use `LogitsProcessor` and `StoppingCriteria` to customize and control the model inference process.

> Note: If you need to do matrix calculations in Java, please use [`openblas`](https://github.com/bytedeco/javacpp-presets/tree/master/openblas)

**chat.octet.model.components.processor.LogitsProcessor**

Customize a processor to adjust the probability distribution of words and control the generation of model inference results. Here is an example: [NoBadWordsLogitsProcessor.java](llama-java-core/src/main/java/chat/octet/model/components/processor/impl/NoBadWordsLogitsProcessor.java)

```java
LogitBias logitBias = new LogitBias();
logitBias.put(5546, "false");
logitBias.put(12113, "5.89");
LogitsProcessorList logitsProcessorList = new LogitsProcessorList().add(new CustomBiasLogitsProcessor(logitBias, model.getVocabSize()));

GenerateParameter generateParams = GenerateParameter.builder()
        .logitsProcessorList(logitsProcessorList)
        .build();
```

**chat.octet.model.components.criteria.StoppingCriteria**

Customize a controller to implement stop rule control for model inference, such as controlling the maximum timeout time generated. Here is an example: [MaxTimeCriteria](llama-java-core/src/main/java/chat/octet/model/components/criteria/impl/MaxTimeCriteria.java)

```java
long maxTime = TimeUnit.MINUTES.toMillis(Optional.ofNullable(params.getTimeout()).orElse(10L));
StoppingCriteriaList stopCriteriaList = new StoppingCriteriaList().add(new MaxTimeCriteria(maxTime));

GenerateParameter generateParams = GenerateParameter.builder()
        .stoppingCriteriaList(stopCriteriaList)
        .build();
```

> More information: `Java docs`


## Quantize models

__Download source model__

Search for `huggingface` to obtain open-source models, supports the `Llama2` and `GPT` models, such as `Baichuan 7B`,`Qwen 7B`.

__Convert to GGUF format model__

```shell
# clone llama.cpp
git clone https://github.com/ggerganov/llama.cpp.git

# install python libs
cd llama.cpp
pip3 install -r requirements.txt

# convert to GGUF format
python3 convert.py SOURCE_MODEL_PATH --outfile OUTPUT_MODEL_PATH/model-f16.gguf
```

__Quantize model__

Use `LlamaService.llamaModelQuantize`  to quantify the model, Set `ModelFileType.LLAMA_FTYPE_MOSTLY_Q8_0` adjust quantization accuracy.

```java
public class QuantizeExample {
    public static void main(String[] args) {
        int status = LlamaService.llamaModelQuantize("OUTPUT_MODEL_PATH/model-f16.gguf",
                "OUTPUT_MODEL_PATH/model.gguf",
                ModelFileType.LLAMA_FTYPE_MOSTLY_Q8_0
        );
        System.out.println("Quantize status: " + status);
    }
}
```

> Or use the `quantize` tool:
>
> `quantize OUTPUT_MODEL_PATH/model-f16.gguf OUTPUT_MODEL_PATH/model.gguf q8_0`


## Build

> [!TIP]
>
> By default, each system version library is included.
>
> If you need to recompile, you can use `llama-java-core/build.sh`


- On Linux

```text
linux-x86-64/libllamajava.so
```

- On macOS

```text
darwin-x86-64/default.metallib
darwin-x86-64/libllamajava.dylib
```

```text
darwin-aarch64/default.metallib
darwin-aarch64/libllamajava.dylib
```

- On Windows

```text
win32-x86-64/llamajava.dll
```

Load the external library file.

```shell
-Doctet.llama.lib=<YOUR_LIB_PATH>
```