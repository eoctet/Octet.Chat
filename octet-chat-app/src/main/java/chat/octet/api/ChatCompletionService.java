package chat.octet.api;

import chat.octet.agent.OctetAgent;
import chat.octet.api.handler.ProcessFunction;
import chat.octet.api.model.*;
import chat.octet.config.CharacterConfig;
import chat.octet.model.Generator;
import chat.octet.model.LlamaService;
import chat.octet.model.Model;
import chat.octet.model.TokenDecoder;
import chat.octet.model.beans.CompletionResult;
import chat.octet.model.beans.Token;
import chat.octet.model.enums.LlamaTokenType;
import chat.octet.model.parameters.GenerateParameter;
import chat.octet.utils.CommonUtils;
import chat.octet.utils.JsonUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.annotations.RouterOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Semaphore;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;

@OpenAPIDefinition(
        info = @Info(title = "Octet.Chat", description = "Build your own private auto agent."),
        externalDocs = @ExternalDocumentation(description = "Github", url = "https://github.com/eoctet/Octet.Chat")
)
@Slf4j
@Configuration
@Tag(name = "ChatCompletionService", description = "Chat completion service")
public class ChatCompletionService {

    private final Semaphore semaphore = new Semaphore(1);

    private RouterFunction<ServerResponse> handler(RequestPredicate requestPredicate, ProcessFunction<RequestParameter> function) {
        return RouterFunctions.route(
                requestPredicate.and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                request -> Mono.fromSupplier(semaphore::tryAcquire).flatMap(acquired -> {
                    if (acquired) {
                        return request.bodyToMono(RequestParameter.class).flatMap(function::process).doFinally(signalType -> semaphore.release());
                    } else {
                        return ServerResponse.status(HttpStatus.NO_CONTENT).header("Retry-After", "5").build();
                    }
                })
        );
    }

    private Mono<ServerResponse> response(HttpStatus status, String body) {
        return ServerResponse.status(status).contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(body));
    }


    @Bean
    @RouterOperation(
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE},
            operation = @Operation(
                    requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = RequestParameter.class))),
                    description = "Chat with local agent.",
                    operationId = "chat",
                    tags = "Chat & Completions",
                    responses = @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ChatCompletionChunk.class)))
            )
    )
    public RouterFunction<ServerResponse> chatCompletionsFunction() {
        return handler(
                POST("/v1/chat/completions"),
                requestParams -> {
                    if (StringUtils.isBlank(requestParams.getUser())) {
                        return response(HttpStatus.BAD_REQUEST, "Request parameter 'user' cannot be empty");
                    }
                    List<ChatMessage> messages = requestParams.getMessages();
                    if (CommonUtils.isEmpty(messages)) {
                        return response(HttpStatus.BAD_REQUEST, "Request parameter 'messages' cannot be empty");
                    }
                    ChatMessage message = messages.get(messages.size() - 1);
                    return handler(requestParams, message, true);
                }
        );
    }

    @Bean
    @RouterOperation(
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE},
            operation = @Operation(
                    requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = RequestParameter.class))),
                    description = "Completions with local agent.",
                    operationId = "completions",
                    tags = "Chat & Completions",
                    responses = @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ChatCompletionChunk.class)))
            )
    )
    public RouterFunction<ServerResponse> completionsFunction() {
        return handler(
                POST("/v1/completions"),
                requestParams -> {
                    if (StringUtils.isBlank(requestParams.getPrompt())) {
                        return response(HttpStatus.BAD_REQUEST, "Request parameter 'prompt' cannot be empty");
                    }
                    ChatMessage message = ChatMessage.toUser(requestParams.getPrompt());
                    return handler(requestParams, message, false);
                }
        );
    }

    @Bean
    @RouterOperation(
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE,
            operation = @Operation(
                    requestBody = @RequestBody(content = @Content(examples = @ExampleObject(value = "{\"content\":\"Your text\"}"), schema = @Schema(implementation = Object.class))),
                    description = "Tokenize text to tokens.",
                    operationId = "tokenize",
                    tags = "Tokenize",
                    responses = @ApiResponse(responseCode = "200", content = @Content(examples = @ExampleObject(value = "{\"tokens\":[]}")))
            )
    )
    public RouterFunction<ServerResponse> tokenizeFunction() {
        return handler(
                POST("/v1/tokenize"),
                requestParams -> {
                    if (StringUtils.isBlank(requestParams.getContent())) {
                        return response(HttpStatus.BAD_REQUEST, "Request parameter 'content' cannot be empty");
                    }
                    int[] tokens = LlamaService.tokenize(requestParams.getContent(), false, true);

                    Map<String, Object> data = Maps.newHashMap();
                    data.put("tokens", tokens);
                    String json = Optional.ofNullable(JsonUtils.toJson(data)).orElse("");
                    return response(HttpStatus.OK, json);
                }
        );
    }

    @Bean
    @RouterOperation(
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE,
            operation = @Operation(
                    requestBody = @RequestBody(content = @Content(examples = @ExampleObject(value = "{\"tokens\":[]}"), schema = @Schema(implementation = Object.class))),
                    description = "Detokenize tokens to text.",
                    operationId = "detokenize",
                    tags = "Tokenize",
                    responses = @ApiResponse(responseCode = "200", content = @Content(examples = @ExampleObject(value = "{\"content\":\"string\"}")))
            )
    )
    public RouterFunction<ServerResponse> detokenizeFunction() {
        return handler(
                POST("/v1/detokenize"),
                requestParams -> {
                    if (requestParams.getTokens() == null) {
                        return response(HttpStatus.BAD_REQUEST, "Request parameter 'tokens' cannot be empty");
                    }
                    String text = TokenDecoder.decodeToken(requestParams.getTokens());

                    Map<String, Object> data = Maps.newHashMap();
                    data.put("content", text);
                    String json = Optional.ofNullable(JsonUtils.toJson(data)).orElse("");
                    return response(HttpStatus.OK, json);
                }
        );
    }


    @Bean
    @RouterOperation(
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.ALL_VALUE,
            operation = @Operation(
                    requestBody = @RequestBody(content = @Content(examples = @ExampleObject(value = "{\"user\":\"string\",\"session\":\"string\"}"), schema = @Schema(implementation = Object.class))),
                    description = "Reset chat session.",
                    operationId = "reset",
                    tags = "Characters",
                    responses = @ApiResponse(responseCode = "200", content = @Content(examples = @ExampleObject(value = "success")))
            )
    )
    public RouterFunction<ServerResponse> resetSessionFunction() {
        return handler(
                POST("/v1/session/reset"),
                requestParams -> {
                    Model model = CharacterModelBuilder.getInstance().getCharacterModel();

                    String user = requestParams.getUser();
                    if (StringUtils.isBlank(user)) {
                        return response(HttpStatus.BAD_REQUEST, "Request parameter 'user' cannot be empty");
                    }
                    String sessionId = requestParams.getSession();
                    if (StringUtils.isBlank(sessionId)) {
                        return response(HttpStatus.BAD_REQUEST, "Request parameter 'session' cannot be empty");
                    }
                    String key = user + ":" + sessionId;
                    model.removeChatStatus(key);

                    if (CharacterModelBuilder.getInstance().getCharacterConfig().isAgentMode()) {
                        OctetAgent.getInstance().reset(key);
                    }
                    return response(HttpStatus.OK, "success");
                }
        );
    }

    @Bean
    @RouterOperation(
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE,
            operation = @Operation(
                    requestBody = @RequestBody(content = @Content(examples = @ExampleObject(value = "{}"), schema = @Schema(implementation = Object.class))),
                    description = "Show all AI characters list.",
                    operationId = "characters",
                    tags = "Characters",
                    responses = @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CharacterModel.class)))
            )
    )
    public RouterFunction<ServerResponse> getCharactersFunction() {
        return RouterFunctions.route(
                RequestPredicates.POST("/v1/characters").and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                serverRequest -> serverRequest.bodyToMono(RequestParameter.class).flatMap(requestParams -> {
                    List<CharacterModel> result = Lists.newArrayList();

                    Map<String, CharacterConfig> configs = CharacterModelBuilder.getInstance().getCharacterConfigs();
                    configs.forEach((name, config) -> {
                        CharacterModel cm = new CharacterModel(name, config.getModelParameter().getModelName(), config.getModelParameter().getModelType());
                        result.add(cm);
                    });

                    Map<String, Object> rs = Maps.newHashMap();
                    rs.put("characters", result);
                    return response(HttpStatus.OK, JsonUtils.toJson(rs));
                })
        );
    }

    @Bean
    @RouterOperation(
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.ALL_VALUE,
            operation = @Operation(
                    requestBody = @RequestBody(content = @Content(examples = @ExampleObject(value = "{\"character\": \"string\"}"), schema = @Schema(implementation = Object.class))),
                    description = "Reload AI characters.",
                    operationId = "reload",
                    tags = "Characters",
                    responses = @ApiResponse(responseCode = "200", content = @Content(examples = @ExampleObject(value = "success")))
            )
    )
    public RouterFunction<ServerResponse> reloadCharactersFunction() {
        return handler(
                POST("/v1/characters/reload"),
                requestParams -> {
                    String character = requestParams.getCharacter();
                    if (StringUtils.isBlank(character)) {
                        return response(HttpStatus.BAD_REQUEST, "Request parameter 'character' cannot be empty");
                    }
                    if (!CharacterModelBuilder.getInstance().getCharacterConfig().getName().equals(character)) {
                        try {
                            CharacterModelBuilder.getInstance().reloadCharacterModel(character);
                        } catch (Exception e) {
                            log.error("Reload character model failed ", e);
                            return response(HttpStatus.BAD_REQUEST, e.getMessage());
                        }
                    }
                    return response(HttpStatus.OK, "success");
                }
        );
    }

    @Bean
    public RouterFunction<ServerResponse> indexRoute() {
        return RouterFunctions.route(RequestPredicates.path("/"), this::redirectToIndex);
    }

    private Mono<ServerResponse> redirectToIndex(ServerRequest serverRequest) {
        ClassPathResource resource = new ClassPathResource("static/index.html");
        Flux<DataBuffer> dataBufferMono = DataBufferUtils.read(resource, DefaultDataBufferFactory.sharedInstance, 1024)
                .map(dataBuffer -> {
                    if (dataBuffer == null) {
                        throw new RuntimeException("Could not read index.html");
                    }
                    return dataBuffer;
                });

        return ServerResponse.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(BodyInserters.fromDataBuffers(dataBufferMono));
    }

    private Mono<ServerResponse> handler(RequestParameter requestParams, ChatMessage message, boolean chat) {
        Model model = CharacterModelBuilder.getInstance().getCharacterModel();
        CharacterConfig config = CharacterModelBuilder.getInstance().getCharacterConfig();

        GenerateParameter generateParams = config.getGenerateParameter();
        generateParams.setUser(requestParams.getUser());
        generateParams.setSession(Optional.ofNullable(requestParams.getSession()).orElse(""));

        String system = config.getPrompt();
        String input = message.getContent();

        if (requestParams.isStream()) {
            if (config.isAgentMode()) {
                return doAgent(model, generateParams, system, input);
            } else {
                return doCompletions(model, generateParams, system, input, chat);
            }
        } else {
            return doCompletions(model, generateParams, system, input, chat, requestParams.isEcho());
        }
    }

    private Mono<ServerResponse> doCompletions(Model model, GenerateParameter generateParams, String system, String input, boolean chat, boolean isEcho) {
        long startTime = System.currentTimeMillis();
        String id = chat ? CommonUtils.randomString("chat") : CommonUtils.randomString("cmpl");
        CompletionResult result;
        ChatCompletionData data;
        if (chat) {
            result = model.chatCompletions(generateParams, system, input);
            String content = isEcho ? (input + result.getContent()) : result.getContent();
            data = new ChatCompletionData(ChatMessage.toAssistant(content), result.getFinishReason().toString());
        } else {
            result = model.completions(generateParams, input);
            data = new ChatCompletionData(result.getContent(), result.getFinishReason().toString());
        }
        ChatCompletionUsage usage = new ChatCompletionUsage(result.getPromptTokens(), result.getCompletionTokens(), (result.getPromptTokens() + result.getCompletionTokens()));
        ChatCompletionChunk chunk = new ChatCompletionChunk(id, model.getModelName(), usage, Lists.newArrayList(data));

        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .body(Flux.just(chunk).doOnCancel(() -> {
                    log.info(MessageFormat.format("Generate cancel, elapsed time: {0} ms.", (System.currentTimeMillis() - startTime)));
                    model.metrics();
                }).doOnComplete(() -> {
                    log.info(MessageFormat.format("Generate completed, elapsed time: {0} ms.", (System.currentTimeMillis() - startTime)));
                    model.metrics();
                }), ChatCompletionChunk.class);
    }

    private Mono<ServerResponse> doCompletions(Model model, GenerateParameter generateParams, String system, String input, boolean chat) {
        long startTime = System.currentTimeMillis();
        String id = chat ? CommonUtils.randomString("chat") : CommonUtils.randomString("cmpl");
        Generator generator = chat ? model.chat(generateParams, system, input) : model.generate(generateParams, input);

        return ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM)
                .body(Flux.fromIterable(generator)
                        .concatWithValues(new Token(-1, LlamaTokenType.LLAMA_TOKEN_TYPE_USER_DEFINED, "[DONE]")).map(token -> {
                            if (token.getId() == -1 && token.getTokenType() == LlamaTokenType.LLAMA_TOKEN_TYPE_USER_DEFINED) {
                                return token.getText();
                            } else {
                                ChatCompletionData data = chat ? new ChatCompletionData("content", token.getText(), token.getFinishReason().name())
                                        : new ChatCompletionData(token.getText(), token.getFinishReason().name());
                                return new ChatCompletionChunk(id, model.getModelName(), Lists.newArrayList(data));
                            }
                        }).doOnCancel(() -> {
                            log.info(MessageFormat.format("Generate cancel, elapsed time: {0} ms.", (System.currentTimeMillis() - startTime)));
                            model.metrics();
                        }).doOnComplete(() -> {
                            log.info(MessageFormat.format("Generate completed, elapsed time: {0} ms.", (System.currentTimeMillis() - startTime)));
                            model.metrics();
                        }), ChatCompletionChunk.class).doFinally(signalType -> generator.close());
    }

    private Mono<ServerResponse> doAgent(Model model, GenerateParameter generateParams, String system, String input) {
        long startTime = System.currentTimeMillis();
        String id = CommonUtils.randomString("chat");
        OctetAgent.Generator generator = OctetAgent.getInstance().chat(model, generateParams, system, input);

        return ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM)
                .body(Flux.fromIterable(generator).flatMap(tokenList -> Flux.fromIterable(tokenList)
                        .map(token -> new ChatCompletionData("content", token.getText(), token.getFinishReason().name()))
                        .map(data -> {
                            try {
                                Thread.sleep(12);
                            } catch (Exception ignored) {
                            }
                            if ("[DONE]".equals(data.getDelta().getValue())) {
                                return data.getDelta().getValue();
                            } else {
                                return new ChatCompletionChunk(id, model.getModelName(), Lists.newArrayList(data));
                            }
                        })
                ).doOnCancel(() -> {
                    log.info(MessageFormat.format("Generate cancel, elapsed time: {0} ms.", (System.currentTimeMillis() - startTime)));
                    model.metrics();
                }).doOnComplete(() -> {
                    log.info(MessageFormat.format("Generate completed, elapsed time: {0} ms.", (System.currentTimeMillis() - startTime)));
                    model.metrics();
                }), ChatCompletionChunk.class);
    }

}
