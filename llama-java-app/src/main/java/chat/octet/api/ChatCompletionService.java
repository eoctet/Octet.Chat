package chat.octet.api;

import chat.octet.api.handler.ProcessFunction;
import chat.octet.api.model.*;
import chat.octet.config.CharacterConfig;
import chat.octet.model.Generator;
import chat.octet.model.LlamaService;
import chat.octet.model.Model;
import chat.octet.model.TokenDecoder;
import chat.octet.model.beans.ChatMessage;
import chat.octet.model.beans.CompletionResult;
import chat.octet.model.beans.Token;
import chat.octet.model.enums.LlamaTokenType;
import chat.octet.model.parameters.GenerateParameter;
import chat.octet.utils.CommonUtils;
import chat.octet.utils.JsonUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

@Slf4j
@Configuration
public class ChatCompletionService {

    private final Semaphore semaphore = new Semaphore(1);

    private <T> RouterFunction<ServerResponse> handler(RequestPredicate requestPredicate, MediaType mediaType, Class<T> elementClass, ProcessFunction<T> function) {
        return RouterFunctions.route(
                requestPredicate.and(RequestPredicates.accept(mediaType)),
                request -> Mono.fromSupplier(semaphore::tryAcquire).flatMap(acquired -> {
                    if (acquired) {
                        return request.bodyToMono(elementClass).flatMap(function::process).doFinally(signalType -> semaphore.release());
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
    public RouterFunction<ServerResponse> chatCompletionsFunction() {
        return handler(
                POST("/v1/chat/completions"),
                MediaType.TEXT_EVENT_STREAM,
                ChatCompletionRequestParameter.class,
                requestParams -> {
                    long startTime = System.currentTimeMillis();
                    if (StringUtils.isBlank(requestParams.getUser())) {
                        return response(HttpStatus.BAD_REQUEST, "Request parameter 'user' cannot be empty");
                    }
                    List<ChatMessage> messages = requestParams.getMessages();
                    if (CommonUtils.isEmpty(messages)) {
                        return response(HttpStatus.BAD_REQUEST, "Request parameter 'messages' cannot be empty");
                    }
                    String input = messages.get(messages.size() - 1).getContent();
                    return doCompletions(requestParams, input, startTime, true);
                }
        );
    }

    @Bean
    public RouterFunction<ServerResponse> completionsFunction() {
        return handler(
                POST("/v1/completions"),
                MediaType.APPLICATION_JSON,
                ChatCompletionRequestParameter.class,
                requestParams -> {
                    long startTime = System.currentTimeMillis();
                    if (StringUtils.isBlank(requestParams.getPrompt())) {
                        return response(HttpStatus.BAD_REQUEST, "Request parameter 'prompt' cannot be empty");
                    }
                    return doCompletions(requestParams, requestParams.getPrompt(), startTime, false);
                }
        );
    }

    @Bean
    public RouterFunction<ServerResponse> tokenizeFunction() {
        return handler(
                POST("/v1/tokenize"),
                MediaType.APPLICATION_JSON,
                ChatCompletionRequestParameter.class,
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
    public RouterFunction<ServerResponse> detokenizeFunction() {
        return handler(
                POST("/v1/detokenize"),
                MediaType.APPLICATION_JSON,
                ChatCompletionRequestParameter.class,
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
    public RouterFunction<ServerResponse> resetSessionFunction() {
        return handler(
                POST("/v1/session/reset"),
                MediaType.APPLICATION_JSON,
                ChatCompletionRequestParameter.class,
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
                    return response(HttpStatus.OK, "success");
                }
        );
    }

    @Bean
    public RouterFunction<ServerResponse> getCharactersFunction() {
        return RouterFunctions.route(
                RequestPredicates.POST("/v1/characters").and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                serverRequest -> serverRequest.bodyToMono(ChatCompletionRequestParameter.class).flatMap(requestParams -> {
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
    public RouterFunction<ServerResponse> reloadCharactersFunction() {
        return handler(
                POST("/v1/characters/reload"),
                MediaType.APPLICATION_JSON,
                ChatCompletionRequestParameter.class,
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

    private Mono<ServerResponse> doCompletions(ChatCompletionRequestParameter requestParams, String input, long startTime, boolean chat) {
        String id = chat ? CommonUtils.randomString("chat") : CommonUtils.randomString("cmpl");
        Model model = CharacterModelBuilder.getInstance().getCharacterModel();
        CharacterConfig config = CharacterModelBuilder.getInstance().getCharacterConfig();

        GenerateParameter generateParams = config.getGenerateParameter();
        generateParams.setUser(requestParams.getUser());
        generateParams.setSession(Optional.ofNullable(requestParams.getSession()).orElse(""));
        String system = config.getPrompt();

        if (!requestParams.isStream()) {
            CompletionResult result;
            ChatCompletionData data;
            if (chat) {
                result = model.chatCompletions(generateParams, system, input);
                String content = requestParams.isEcho() ? (input + result.getContent()) : result.getContent();
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
        } else {
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
    }

}
