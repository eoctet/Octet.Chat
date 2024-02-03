# 🦙 LLaMA Java ☕️


[![CI](https://github.com/eoctet/llama-java/actions/workflows/maven_build_deploy.yml/badge.svg)](https://github.com/eoctet/llama-java/actions/workflows/maven_build_deploy.yml)
[![Maven Central](https://img.shields.io/maven-central/v/chat.octet/llama-java-core?color=orange)](https://mvnrepository.com/artifact/chat.octet/llama-java-core)
[![README Zh_CN](https://img.shields.io/badge/Lang-中文-red)](./README.Zh_CN.md)
[![GitHub](https://img.shields.io/github/license/eoctet/llama-java?color=green)](https://opensource.org/licenses/MIT)
![GitHub all releases](https://img.shields.io/github/downloads/eoctet/llama-java/total?color=blue)

This is a 🦙 `LLaMA` Java project. You can use it to deploy your own private services, support `Llama2` series models and other open source models.

#### Provides
- Simple Java library `llama-java-core`
- Complete API services `llama-java-app`
  - `Server deployment` Quickly realize privatized services
  - `CLI Interaction` Simple local chat interaction

#### Features
- 🦙 Built on [`llama.cpp`](https://github.com/ggerganov/llama.cpp)
- 😊 Support `AI Agent` and implements `Function calling` based on `Qwen-chat`
- 🤖 Supports `parallel inference`, `continuous conversation` and `text generation`
- 📦 Support for `Llama2` series models and other open source models, such as `Baichuan 7B`,`Qwen 7B`

----

<details>

<summary>Last updated</summary>

   ...

- [X] 🚀 Added custom AI character and optimized OpenAPI
- [X] 🚀 Added AI Agent and implemented Function calling

</details>

## Quick start

> [!NOTE]
>
> Support the model files for `llama.cpp`, you can quantify the original model yourself or search for `huggingface` to obtain open-source models.

### 🖥 Server deployment


#### ① Set up an AI character

Edit `characters.template.json` to set a custom AI character.

<details>

<summary>Example</summary>

```json
{
  "agent_mode": false,
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

> [Character parameter help](https://github.com/eoctet/llama-java/wiki/Llama-Java-parameters)

</details>

#### ② Launch the app

```bash
# Default URL: http://YOUR_IP_ADDR:8152/

cd <YOUR_PATH>/llama-java-app
bash app_server.sh start
```

#### ③ Get started

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

<summary>The API will return data in a stream format</summary>

```json
{
    "id": "octetchat-98fhd2dvj7",
    "model": "Llama2-chat",
    "created": 1695614393810,
    "choices": [
        {
            "index": 0,
            "delta": {
                "content": "Hi"
            },
            "finish_reason": "NONE"
        }
    ]
}
```

</details>

### 🤖 CLI interaction

__How to use__

Edit `characters.template.json` to set a custom AI character. Run the command line interaction and specify the set AI character name.

```bash
java -jar llama-java-app.jar --character YOUR_CHARACTER
```

### 🚀 AI Agent

> [!NOTE]
>
> Implementation based on the `Qwen-chat` series model. For more information, please refer to: [Qwen Github](https://github.com/QwenLM/Qwen)

__How to use__

Download the `Qwen-chat` model, edit [`octet.json`](llama-java-app/characters/octet.json) to set the model file path, and change `agent_mode` to `true` to start the agent mode.

Run the command line interaction to start chatting:

```bash
java -jar llama-java-app.jar --character octet
```

* Two plugins are currently implemented, and as examples you can continue to enrich them.

| Plugin   | Description                                                                                                                   |
|----------|-------------------------------------------------------------------------------------------------------------------------------|
| Datetime | A plugin that can query the current system time.                                                                              |
| API      | A universal API calling plugin, based on which you can achieve access to services such as weather, text to image, and search. |

> Plugin configuration file example: [plugins.json](llama-java-app/characters/plugins.json)

![Octet Agent](docs/agent.png)


> [!TIP]
>
> Use `help` to view more parameters, for example:

```bash
java -jar llama-java-app.jar --help

usage: LLAMA-JAVA-APP
    --app <arg>          App launch type: cli | api (default: cli).
 -c,--completions        Use completions mode.
 -ch,--character <arg>   Load the specified AI character, default:
                         llama2-chat.
 -h,--help               Show this help message and exit.
 -q,--questions <arg>    Load the specified user question list, example:
                         /PATH/questions.txt.
```

## Documentation

__Development__

- __[开发手册](https://github.com/eoctet/llama-java/wiki/开发手册)__
- __[Development manual](https://github.com/eoctet/llama-java/wiki/Development-manual)__

__Characters config__

- __[Llama Java Parameter](https://github.com/eoctet/llama-java/wiki/Llama-Java-parameters)__
- __[characters.template.json](llama-java-app/characters/characters.template.json)__


## Disclaimer

> [!IMPORTANT]
>
> - This project does not provide any models. Please obtain the model files yourself and comply with relevant agreements.
> - Please do not use this project for illegal purposes, including but not limited to commercial use, profit-making use, or use that violates Chinese laws and regulations.
> - Any legal liability arising from the use of this project shall be borne by the user, and this project shall not bear any legal liability.

## Feedback

- If you have any questions, please submit them in GitHub Issue.
