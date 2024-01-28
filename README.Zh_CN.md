# 🦙 LLaMA Java ☕️


[![CI](https://github.com/eoctet/llama-java/actions/workflows/maven_build_deploy.yml/badge.svg)](https://github.com/eoctet/llama-java/actions/workflows/maven_build_deploy.yml)
[![Maven Central](https://img.shields.io/maven-central/v/chat.octet/llama-java-core?color=orange)](https://mvnrepository.com/artifact/chat.octet/llama-java-core)
[![README English](https://img.shields.io/badge/Lang-English-red)](./README.md)
[![GitHub](https://img.shields.io/github/license/eoctet/llama-java?color=green)](https://opensource.org/licenses/MIT)
![GitHub all releases](https://img.shields.io/github/downloads/eoctet/llama-java/total?color=blue)

这个是一个 🦙 `LLaMA` Java项目。你可以用它部署自己的私有服务，支持 `Llama2` 系列模型及其他开源模型。

#### 提供
- 简单易用的Java库 `llama-java-core`
- 完整的API服务 `llama-java-app`
  - `服务端部署`，快速实现私有化服务
  - `命令行交互`，简单的本地聊天交互

#### 主要特点
- 🦙 基于  [`llama.cpp`](https://github.com/ggerganov/llama.cpp) 构建
- ☕️ 使用 `JNI` 开发Java库，提供与 `Llama.cpp` 一致的接口
- 🤖 支持 `并行推理`、`连续对话` 和 `文本生成`
- 📦 支持 `Llama2` 系列模型和其他开源模型，例如：`Baichuan 7B`、`QWen 7B`

----

<details>

<summary>最近更新</summary>

- [X] 🚀 提供模型量化接口
- [X] 🚀 自定义模型的提示词模版（例如：Vicuna、Alpaca等等）
- [X] 🚀 并行批处理解码（PS：默认已启用批处理解码）
- [X] 🚀 Min-P 采样支持
- [X] 🚀 YaRN RoPE 支持
- [X] 🚀 增加自定义AI角色、优化OpenAPI

</details>

## 快速开始

> [!NOTE] 
>
> 支持 `llama.cpp` 量化的模型文件，你可以自行量化原始模型或搜索 `huggingface` 获取开源模型。

### 🖥 服务端部署


#### ① 设置一个角色

编辑 `characters.template.json` 设置一个自定义的AI角色。

<details>

<summary>示例角色</summary>

```json
{
  "name": "Octet",
  "prompt": "Answer the questions.",
  "model_parameter": {
    "model_path": "/models/ggml-model-7b_m-q6_k.gguf",
    "model_type": "LLAMA2",
    "context_size": 4096,
    "threads": 6,
    "threads_batch": 6,
    "mmap": true,
    "mlock": false,
    "verbose": true
  },
  "generate_parameter": {
    "temperature": 0.85,
    "repeat_penalty": 1.2,
    "top_k": 40,
    "top_p": 0.9,
    "verbose_prompt": true,
    "user": "User",
    "assistant": "Octet"
  }
}
```

> [!NOTE]
>
> [完整参数说明](https://github.com/eoctet/llama-java/wiki/Llama-Java-parameters)

</details>

#### ② 启动服务

```bash
# Default URL: http://YOUR_IP_ADDR:8152/

cd <YOUR_PATH>/llama-java-app
bash app_server.sh start
```

#### ③ 开始访问

> `POST` **/v1/chat/completions**

```shell
curl --location 'http://127.0.0.1:8152/v1/chat/completions' \
--header 'Content-Type: application/json' \
--data '{
    "messages": [
        {
            "role": "USER",
            "content": "Who are you?"
        }
    ],
    "stream": true,
    "character": "octet"
}'
```

<details>

<summary>接口将以流的方式返回数据</summary>

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

</details>

### 🤖 命令行交互

#### ① 设置一个角色

编辑 `characters.template.json` 设置一个自定义的AI角色。

#### ② 启动服务

运行命令行，指定刚才设置的角色名称。

```bash
java -jar llama-java-app.jar --character octet
```

#### ③ 开始访问

![cmd.png](docs/cmd.png)


> [!TIP]
>
> 使用 `help` 查看更多参数，示例如下：

```bash
java -jar llama-java-app.jar --help

usage: LLAMA-JAVA-APP
    --app <arg>                 App launch type: cli | api (default: cli).
 -c,--completions               Use completions mode.
 -h,--help                      Show this help message and exit.
 -ch,--character <arg>          Load the specified AI character, default: llama2-chat.
```

## 开发手册

#### Maven

```xml
<dependency>
    <groupId>chat.octet</groupId>
    <artifactId>llama-java-core</artifactId>
    <version>LAST_RELEASE_VERSION</version>
</dependency>
```

#### Gradle
```txt
implementation group: 'chat.octet', name: 'llama-java-core', version: 'LAST_RELEASE_VERSION'
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

> [!TIP]
> 
> More examples: `chat.octet.examples.*`


#### Components
  - `LogitsProcessor`
  - `StoppingCriteria`

可以使用 `LogitsProcessor` 和 `StoppingCriteria` 对模型推理过程进行自定义控制。

> 注：如果需要在Java中进行矩阵计算请使用 [`openblas`](https://github.com/bytedeco/javacpp-presets/tree/master/openblas)

**chat.octet.model.components.processor.LogitsProcessor**

自定义一个处理器对词的概率分布进行调整，控制模型推理的生成结果。这里是一个示例：[NoBadWordsLogitsProcessor.java](llama-java-core/src/main/java/chat/octet/model/components/processor/impl/NoBadWordsLogitsProcessor.java)

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

自定义一个控制器实现对模型推理的停止规则控制，例如：控制生成最大超时时间，这里是一个示例：[MaxTimeCriteria](llama-java-core/src/main/java/chat/octet/model/components/criteria/impl/MaxTimeCriteria.java)

```java
    long maxTime = TimeUnit.MINUTES.toMillis(Optional.ofNullable(params.getTimeout()).orElse(10L));
    StoppingCriteriaList stopCriteriaList = new StoppingCriteriaList(Lists.newArrayList(new MaxTimeCriteria(maxTime)));
    
    GenerateParameter generateParams = GenerateParameter.builder()
            .stoppingCriteriaList(stopCriteriaList)
            .build();
```

> 完整的文档请参考 `Java docs`

#### 如何编译

默认已包含各系统版本库，可以直接使用。

> 如果需要更加灵活的编译方式，请参考 `llama.cpp` **Build** 文档。

```ini
# 加载外部库文件

-Doctet.llama.lib=<YOUR_LIB_PATH>
```

#### 帮助文档

- __[Llama Java Parameter](https://github.com/eoctet/llama-java/wiki/Llama-Java-parameters)__


## 免责声明

> [!IMPORTANT]
> 
> - 本项目仅供参考，不对任何问题负责。
> - 本项目不提供任何模型，请自行获取模型文件并遵守相关协议。
> - 请勿将本项目用于非法用途，包括但不限于商业用途、盈利用途、以及违反中国法律法规的用途。
> - 因使用本项目所产生的任何法律责任，由使用者自行承担，本项目不承担任何法律责任。

## 问题反馈

- 如果你有任何疑问，欢迎在GitHub Issue中提交。
