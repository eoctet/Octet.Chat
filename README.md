# 🚀 Octet.Chat


[![CI](https://github.com/eoctet/octet.chat/actions/workflows/maven_build_deploy.yml/badge.svg)](https://github.com/eoctet/octet.chat/actions/workflows/maven_build_deploy.yml)
[![Maven Central](https://img.shields.io/maven-central/v/chat.octet/llama-java-core?color=orange)](https://mvnrepository.com/artifact/chat.octet/llama-java-core)
[![README Zh_CN](https://img.shields.io/badge/Lang-中文-red)](./README.Zh_CN.md)
[![GitHub](https://img.shields.io/github/license/eoctet/llama-java?color=green)](https://opensource.org/licenses/MIT)

This is a LLMs project implemented in Java.
You can use it to deploy your own private services, supports the `Llama3` and `GPT` models and other open-source models.

#### Provides
- Simple Java library `llama-java-core`
- Complete application `octet-chat-app`
  - `API Services` Quickly realize privatized services
  - `CLI Interaction` Simple local chat interaction

#### Features
- 🦙 Built on [`llama.cpp`](https://github.com/ggerganov/llama.cpp)
- 😊 Support `AI Agent` and implements `Function calling` based on `Qwen-chat`
- 🤖 Supports `parallel inference`, `continuous conversation` and `text generation`
- 📦 Supports the `Llama3` and `GPT` models, such as `Baichuan 7B`,`Qwen 7B`

----

<details>

<summary>Last updated</summary>

   ...

- [X] 🚀 Added custom AI character and optimized OpenAPI
- [X] 🚀 Added AI Agent and implemented Function calling
- [X] 🚀 Supported dynamic temperature sampling.
- [X] 🚀 Added WebUI to octet-chat-app.
- [X] 🚀 Updated API parameters.

</details>

## Quick start

> [!NOTE]
>
> You can quantify the original model yourself or search for `huggingface` to obtain open-source models.


### 🤖 CLI interaction

__How to use__

Edit `characters.template.json` to set a custom AI character. Run command line interaction and specify the set AI character name.

<details>

<summary>Example</summary>

```json
{
  "name": "Assistant Octet",
  "agent_mode": false,
  "prompt": "Answer the questions.",
  "model_parameter": {
    "model_path": "/models/ggml-model-7b_m-q6_k.gguf",
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

</details>

```bash
java -jar octet-chat-app.jar --character YOUR_CHARACTER
```

> [!TIP]
>
> Use `help` to view more parameters, for example:

```bash
java -jar octet-chat-app.jar --help

usage: Octet.Chat
    --app <arg>          App launch type: cli | api (default: cli).
 -c,--completions        Use completions mode.
 -ch,--character <arg>   Load the specified AI character, default:
                         llama2-chat.
 -h,--help               Show this help message and exit.
 -q,--questions <arg>    Load the specified user question list, example:
                         /PATH/questions.txt.
```


### 🚀 AI Agent

> [!NOTE]
>
> Implementation based on the `Qwen-chat` series model. For more information, please refer to: [Qwen Github](https://github.com/QwenLM/Qwen)

__How to use__

Download the `Qwen-chat` model, edit [`octet.json`](octet-chat-app/characters/octet.json) to set the model file path, and change `agent_mode` to `true` to start the agent mode.


* Two plugins are currently implemented, and as examples you can continue to enrich them.

| Plugin   | Description                                                                                                                   |
|----------|-------------------------------------------------------------------------------------------------------------------------------|
| Datetime | A plugin that can query the current system time.                                                                              |
| API      | A universal API calling plugin, based on which you can achieve access to services such as weather, text to image, and search. |

> Plugin configuration file example: [plugins.json](octet-chat-app/characters/plugins.json)

![Octet Agent](docs/agent.png)


### 🖥 Web UI

__How to use__

Just like CLI interaction, set a custom AI character and Launch the app.

open browser enjoy it now `http://YOUR_IP_ADDR:8152/`

```bash
# Default URL: http://YOUR_IP_ADDR:8152/

cd <YOUR_PATH>/octet-chat-app
bash app_server.sh start YOUR_CHARACTER
```

![webui.png](docs/webui.png)


> [!TIP]
>
> It can be integrated into your services, such as `VsCode`, `App`, `Wechat`, etc.

<details>

<summary>How to call API</summary>

> Api docs: http://127.0.0.1:8152/swagger-ui.html

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
    "user": "User",
    "stream": true
}'
```

The API will return data in a stream format:

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


## Documentation

__Development__

- __[开发手册](https://github.com/eoctet/octet.chat/wiki/开发手册)__
- __[Development manual](https://github.com/eoctet/octet.chat/wiki/Development-manual)__

__Characters config__

- __[Llama Java Parameter](https://github.com/eoctet/octet.chat/wiki/Llama-Java-parameters)__
- __[characters.template.json](octet-chat-app/characters/characters.template.json)__


## Disclaimer

> [!IMPORTANT]
>
> - This project does not provide any models. Please obtain the model files yourself and comply with relevant agreements.
> - Please do not use this project for illegal purposes, including but not limited to commercial use, profit-making use, or use that violates laws and regulations.
> - Any legal liability arising from the use of this project shall be borne by the user, and this project shall not bear any legal liability.

## Feedback

- If you have any questions, please submit them in GitHub Issue.
