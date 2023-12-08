# 🦙 LLaMA Java ☕️


[![CI](https://github.com/eoctet/llama-java-core/actions/workflows/maven_build_deploy.yml/badge.svg)](https://github.com/eoctet/llama-java-core/actions/workflows/maven_build_deploy.yml)
[![Maven Central](https://img.shields.io/maven-central/v/chat.octet/llama-java-core?color=orange)](https://mvnrepository.com/artifact/chat.octet/llama-java-core)
[![README English](https://img.shields.io/badge/Lang-English-red)](./README.md)
[![GitHub](https://img.shields.io/github/license/eoctet/llama-java-core?color=green)](https://opensource.org/licenses/MIT)

这个是一个 🦙 `LLaMA Java` 实现。提供了一个Java库 `llama-java-core` 以及一个完整的API服务，你可以用它部署自己的私有服务，支持 `Llama2` 系列模型及其他开源模型。

#### 主要特点
- 🦙 基于  [`llama.cpp`](https://github.com/ggerganov/llama.cpp) 构建
- 🚀 支持 `OpenAPI`，快速实现私有化服务
- 🤖 支持 `并行推理`、`连续对话` 和 `文本生成`
- 📦 支持 `Llama2` 系列模型和其他开源模型，例如：`Baichuan 7B`、`QWen 7B`
- ☕️ 使用 `JNI` 开发本地库，提供与 `Llama.cpp` 一致的接口
- 💻 支持 `命令行交互` 和 `服务端部署`

#### 最近更新
- [X] 🚀 提供模型量化接口
- [X] 🚀 自定义模型的提示词模版（例如：Vicuna、Alpaca等等）
- [X] 🚀 并行批处理解码
- [X] 🚀 Min-P 采样支持
- [X] 🚀 YaRN RoPE 支持

## 快速开始

#### 🖥 服务端部署

- 下载并启动服务

```bash
# Default URL: http://YOUR_IP_ADDR:8152/

cd <YOUR_PATH>/llama-java-app
bash app_server.sh start
```

- 目录示例

```text
=> llama-java-app
   ⌊___ llama-java-app.jar
   ⌊___ app_server.sh
   ⌊___ conf
        ⌊___ setting.json

···
```

与 `ChatGPT` 的接口规范保持一致，仅实现主要的接口，可以与 [`ChatGPT Next Web`](https://github.com/Yidadaa/ChatGPT-Next-Web) 等WebUI、App集成使用。

> ℹ️ __其中不同之处__
> 1. 新增了Llama系列模型的参数，删除了不支持的GPT参数；
> 2. 默认使用了 `Llama2-chat` 提示词模版，如需适配其他模型，可自行调整；
> 3. 没有请求认证、使用量查询等不需要的功能；
> 4. 优化对话聊天接口，不需要传递历史对话上下文，仅当前对话内容即可。
>
> > 完整的API信息请参考[`API 文档`](docs/API.md)。

![webui.png](docs%2Fwebui.png)

举个栗子

> `POST` **/v1/chat/completions**

```shell
curl --location 'http://127.0.0.1:8152/v1/chat/completions' \
--header 'Content-Type: application/json' \
--data '{
    "messages": [
        {
            "role": "SYSTEM",
            "content": "<YOUR_PROMPT>"
        },
        {
            "role": "USER",
            "content": "Who are you?"
        }
    ],
    "user": "william",
    "verbose": true,
    "stream": true,
    "model": "Llama2-chat"
}'
```

接口将以流的方式返回数据：

```json
{
    "id": "octetchat-98fhd2dvj7",
    "model": "Llama2-chat",
    "created": 1695614393810,
    "choices": [
        {
            "index": 0,
            "delta": {
                "content": "你好"
            },
            "finish_reason": "NONE"
        }
    ]
}
```

#### 🤖 命令行交互

运行命令行交互，指定需要加载的语言模型。

```bash
java -jar llama-java-app.jar --model llama2-chat --system 'YOUR_PROMPT'
```

```txt
... ...

User: 你是谁
AI: 作为一个 AI，我不知道我是谁。我的设计者和创建者创造了我。
但是，我是一个虚拟助手，旨在提供帮助和回答问题。
```

> 使用 `help` 查看更多参数，示例如下：

```bash
java -jar llama-java-app.jar --help

usage: LLAMA-JAVA-APP
    --app <arg>                 App launch type: cli | api (default: cli).
 -c,--completions               Use completions mode.
    --frequency-penalty <arg>   Repeat alpha frequency penalty (default:
                                0.0, 0.0 = disabled)
 -h,--help                      Show this help message and exit.
 -m,--model <arg>               Load model name, default: llama2-chat.
    --max-new-tokens <arg>      Maximum new token generation size
                                (default: 0 unlimited).
    --min-p <arg>               Min-p sampling (default: 0.05, 0 =
                                disabled).
    --mirostat <arg>            Enable Mirostat sampling, controlling
                                perplexity during text generation
                                (default: 0, 0 = disabled, 1 = Mirostat, 2
                                = Mirostat 2.0).
    --mirostat-ent <arg>        Set the Mirostat target entropy, parameter
                                tau (default: 5.0).
    --mirostat-lr <arg>         Set the Mirostat learning rate, parameter
                                eta (default: 0.1).
    --no-penalize-nl <arg>      Disable penalization for newline tokens
                                when applying the repeat penalty (default:
                                true).
    --presence-penalty <arg>    Repeat alpha presence penalty (default:
                                0.0, 0.0 = disabled)
    --repeat-penalty <arg>      Control the repetition of token sequences
                                in the generated text (default: 1.1).
    --system <arg>              Set a system prompt.
    --temperature <arg>         Adjust the randomness of the generated
                                text (default: 0.8).
    --tfs <arg>                 Enable tail free sampling with parameter z
                                (default: 1.0, 1.0 = disabled).
    --top-k <arg>               Top-k sampling (default: 40, 0 =
                                disabled).
    --top-p <arg>               Top-p sampling (default: 0.9).
    --typical <arg>             Enable typical sampling sampling with
                                parameter p (default: 1.0, 1.0 =
                                disabled).
    --verbose-prompt            Print the prompt before generating text.
```

## 开发手册

#### Maven POM

```xml
<dependency>
    <groupId>chat.octet</groupId>
    <artifactId>llama-java-core</artifactId>
    <version>1.3.0</version>
</dependency>
```

#### Examples

- **Chat Console Example**

这里提供了一个简单的聊天示例。

```java
public class ConsoleExample {
    private static final String MODEL_PATH = "/llama-java-app/models/llama2/ggml-model-7b-q6_k.gguf";

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

    private static final String MODEL_PATH = "/llama-java-app/models/llama2/ggml-model-7b-q6_k.gguf";

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
```

**chat.octet.model.components.criteria.StoppingCriteria**

自定义一个控制器实现对模型推理的停止规则控制，例如：控制生成最大超时时间，这里是一个示例：[MaxTimeCriteria](src%2Fmain%2Fjava%2Fchat%2Foctet%2Fmodel%2Fcomponents%2Fcriteria%2Fimpl%2FMaxTimeCriteria.java)

```java
    long maxTime = TimeUnit.MINUTES.toMillis(Optional.ofNullable(params.getTimeout()).orElse(10L));
    StoppingCriteriaList stopCriteriaList = new StoppingCriteriaList(Lists.newArrayList(new MaxTimeCriteria(maxTime)));
    
    GenerateParameter generateParams = GenerateParameter.builder()
            .stoppingCriteriaList(stopCriteriaList)
            .build();
```

> 完整的文档请参考 [Java docs](docs%2Fapidocs%2Findex.html)

#### 如何编译

默认已包含各系统版本库，可以直接使用。

> 如果需要更加灵活的编译方式，请参考 `llama.cpp` **Build** 文档。

```ini
# 加载外部库文件

-Doctet.llama.lib=<YOUR_LIB_PATH>
```

## 问题反馈

- 如果你有任何疑问，欢迎在GitHub Issue中提交。
