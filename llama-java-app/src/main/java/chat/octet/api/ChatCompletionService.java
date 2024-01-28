package chat.octet.api;

import chat.octet.api.model.ChatCompletionChunk;
import chat.octet.api.model.ChatCompletionData;
import chat.octet.api.model.ChatCompletionRequestParameter;
import chat.octet.config.CharacterConfig;
import chat.octet.model.LlamaService;
import chat.octet.model.Model;
import chat.octet.model.TokenDecoder;
import chat.octet.model.beans.ChatMessage;
import chat.octet.model.beans.CompletionResult;
import chat.octet.model.beans.Token;
import chat.octet.model.parameters.GenerateParameter;
import chat.octet.utils.CommonUtils;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.MessageFormat;
import java.util.List;

@Slf4j
@Configuration
public class ChatCompletionService {

    @Bean
    public RouterFunction<ServerResponse> chatCompletionsFunction() {
        return RouterFunctions.route(
                RequestPredicates.POST("/v1/chat/completions").and(RequestPredicates.accept(MediaType.TEXT_EVENT_STREAM)),
                serverRequest -> serverRequest.bodyToMono(ChatCompletionRequestParameter.class).flatMap(requestParams -> {
                    long startTime = System.currentTimeMillis();

                    List<ChatMessage> messages = requestParams.getMessages();
                    if (messages == null || messages.isEmpty()) {
                        return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                                .body(BodyInserters.fromValue("Request parameter 'messages' cannot be empty"));
                    }
                    String input = messages.get(messages.size() - 1).getContent();
                    return doCompletions(requestParams, input, startTime, true);
                })
        );
    }

    @Bean
    public RouterFunction<ServerResponse> completionsFunction() {
        return RouterFunctions.route(
                RequestPredicates.POST("/v1/completions").and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                serverRequest -> serverRequest.bodyToMono(ChatCompletionRequestParameter.class).flatMap(requestParams -> {
                    long startTime = System.currentTimeMillis();
                    if (StringUtils.isBlank(requestParams.getPrompt())) {
                        return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                                .body(BodyInserters.fromValue("Request parameter 'prompt' cannot be empty"));
                    }
                    return doCompletions(requestParams, requestParams.getPrompt(), startTime, false);
                })
        );
    }

    @Bean
    public RouterFunction<ServerResponse> tokenizeFunction() {
        return RouterFunctions.route(
                RequestPredicates.POST("/v1/tokenize").and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                serverRequest -> serverRequest.bodyToMono(String.class).flatMap(content -> {
                    int[] tokens = LlamaService.tokenize(content, false, true);
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(tokens));
                })
        );
    }

    @Bean
    public RouterFunction<ServerResponse> detokenizeFunction() {
        return RouterFunctions.route(
                RequestPredicates.POST("/v1/detokenize").and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                serverRequest -> serverRequest.bodyToMono(List.class).flatMap(tokens -> {
                    int[] arrays = tokens.stream().mapToInt((Object i) -> Integer.parseInt(i.toString())).toArray();
                    String text = TokenDecoder.decodeToken(arrays);
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(text));
                })
        );
    }

    private Mono<ServerResponse> doCompletions(ChatCompletionRequestParameter requestParams, String input, long startTime, boolean chat) {
        String id = chat ? CommonUtils.randomString("octetchat") : CommonUtils.randomString("octetcmpl");
        Model model = CharacterModelBuilder.getInstance().getCharacterModel(requestParams.getCharacter());
        CharacterConfig config = CharacterModelBuilder.getInstance().getCharacterConfig();

        GenerateParameter generateParams = config.getGenerateParameter();
        String system = config.getPrompt();

        if (!requestParams.isStream()) {
            CompletionResult result;
            ChatCompletionData data;
            if (chat) {
                result = model.chatCompletions(generateParams, system, input);
                data = new ChatCompletionData(ChatMessage.toAssistant(result.getContent()), result.getFinishReason().toString());
            } else {
                result = model.completions(generateParams, input);
                data = new ChatCompletionData(result.getContent(), result.getFinishReason().toString());
            }
            ChatCompletionChunk chunk = new ChatCompletionChunk(id, model.getModelName(), Lists.newArrayList(data));

            return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(Flux.just(chunk).doOnCancel(() -> {
                        log.info(MessageFormat.format("Generate cancel, elapsed time: {0} ms.", (System.currentTimeMillis() - startTime)));
                        model.metrics();
                    }).doOnComplete(() -> {
                        log.info(MessageFormat.format("Generate completed, elapsed time: {0} ms.", (System.currentTimeMillis() - startTime)));
                        model.metrics();
                    }), ChatCompletionChunk.class);
        } else {
            //streaming output
            Iterable<Token> tokenIterable = chat ? model.chat(generateParams, system, input) : model.generate(generateParams, input);
            return ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(Flux.fromIterable(tokenIterable).map(token -> {
                        String text = token.getFinishReason().isFinished() ? "[DONE]" : token.getText();
                        ChatCompletionData data = chat ? new ChatCompletionData("content", text, token.getFinishReason().name())
                                : new ChatCompletionData(text, token.getFinishReason().name());
                        return new ChatCompletionChunk(id, model.getModelName(), Lists.newArrayList(data));
                    }).doOnCancel(() -> {
                        log.info(MessageFormat.format("Generate cancel, elapsed time: {0} ms.", (System.currentTimeMillis() - startTime)));
                        model.metrics();
                    }).doOnComplete(() -> {
                        log.info(MessageFormat.format("Generate completed, elapsed time: {0} ms.", (System.currentTimeMillis() - startTime)));
                        model.metrics();
                    }), ChatCompletionChunk.class);
        }
    }

}
