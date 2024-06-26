# 🚀 Octet.Chat


[![CI](https://github.com/eoctet/octet.chat/actions/workflows/maven_build_deploy.yml/badge.svg)](https://github.com/eoctet/octet.chat/actions/workflows/maven_build_deploy.yml)
[![Maven Central](https://img.shields.io/maven-central/v/chat.octet/llama-java-core?color=orange)](https://mvnrepository.com/artifact/chat.octet/llama-java-core)
[![README English](https://img.shields.io/badge/Lang-English-red)](./README.md)
[![GitHub](https://img.shields.io/github/license/eoctet/llama-java?color=green)](https://opensource.org/licenses/MIT)

这是一个Java实现的LLMs项目。你可以用它部署自己的私有服务，支持 `Llama3` 和 `GPT` 模型及其他开源模型。

#### 提供
- 简单易用的Java库 `llama-java-core`
- 完整的应用服务 `octet-chat-app`
  - `服务端部署`，快速实现私有化服务
  - `命令行交互`，简单的本地聊天交互

#### 主要特点
- 🦙 基于  [`llama.cpp`](https://github.com/ggerganov/llama.cpp) 构建
- 😊 支持 `AI Agent`，基于 `Qwen-chat` 实现 `Function calling`
- 🤖 支持 `并行推理`、`连续对话` 和 `文本生成`
- 📦 支持 `Llama3` 和 `GPT` 模型及其他开源模型，例如：`Baichuan 7B`、`Qwen 7B`

----

<details>

<summary>最近更新</summary>

   ...

- [X] 🚀 支持动态温度采样
- [X] 🚀 Octet-chat-app 增加了 WebUI
- [X] 🚀 更新API参数
- [X] 🚀 优化聊天提示词解析、Windows Cli
- [X] 🚀 重构函数调用，优化聊天提示词解析和接口

</details>

## 快速开始

> [!NOTE] 
>
> 你可以自行量化原始模型或搜索 `huggingface` 获取开源模型。


### 🤖 命令行交互

__如何使用__

编辑 `characters.template.json` 设置一个自定义的AI角色。

<details>

<summary>示例角色</summary>

```json
{
  "name": "Assistant Octet",
  "function_call": false,
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

运行命令行交互并指定刚才设置的角色名称，开始聊天：

```bash
java -jar octet-chat-app.jar --character YOUR_CHARACTER
```

> [!TIP]
>
> 使用 `help` 查看更多参数，示例如下：

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
 -f,--function           Enable the function call in chat.      
```


### 🚀 函数调用

> [!NOTE]
>
> 实现基于 `Qwen-chat` 系列模型，更多信息请参考：[Qwen Github](https://github.com/QwenLM/Qwen)

__如何使用__

下载 `Qwen-chat` 模型，编辑 [`octet.json`](octet-chat-app/characters/octet.json) 设置模型文件路径，将 `function_call` 修改为 `true` 开启函数调用。


* 目前实现了两个函数，作为示例你可以继续丰富扩展它们。

| 插件   | 描述                                 |
|------|------------------------------------|
| 时间查询 | 可以查询当前系统时间的函数。                     |
| 接口调用 | 通用的接口调用函数，基于此你可以实现天气、文生图、搜索等服务的接入。 |

> 函数配置文件示例：[functions.json](octet-chat-app/characters/functions.json)

![Octet Agent](docs/agent.png)


### 🖥 Web UI

__如何使用__

和命令行交互一样，首先设置一个自定义的AI角色。

启动服务，打开浏览器开始聊天，默认地址：`http://YOUR_IP_ADDR:8152/`

```bash
# Default URL: http://YOUR_IP_ADDR:8152/

cd <YOUR_PATH>/octet-chat-app
bash app_server.sh start YOUR_CHARACTER
```

![webui.png](docs/webui.png)


> [!TIP]
>
> 你也可以将API服务集成到你的应用中，例如：`VsCode`、`App`、`Wechat`等。

<details>

<summary>如何调用API</summary>

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

</details>


## 帮助文档

__开发文档__

- __[开发手册](https://github.com/eoctet/octet.chat/wiki/开发手册)__
- __[Development manual](https://github.com/eoctet/octet.chat/wiki/Development-manual)__

__角色配置__

- __[Llama Java Parameter](https://github.com/eoctet/octet.chat/wiki/Llama-Java-parameters)__
- __[characters.template.json](octet-chat-app/characters/characters.template.json)__


## 免责声明

> [!IMPORTANT]
> 
> - 本项目不提供任何模型，请自行获取模型文件并遵守相关协议。
> - 请勿将本项目用于非法用途，包括但不限于商业用途、盈利用途、以及违反法律法规的用途。
> - 因使用本项目所产生的任何法律责任，由使用者自行承担，本项目不承担任何法律责任。

## 问题反馈

- 如果你有任何疑问，欢迎在GitHub Issue中提交。
