package chat.octet.api;

import chat.octet.api.model.ChatCompletionChunk;
import chat.octet.api.model.ChatCompletionData;
import chat.octet.api.model.ChatCompletionRequestParameter;
import chat.octet.model.LlamaService;
import chat.octet.model.Model;
import chat.octet.model.TokenDecoder;
import chat.octet.model.beans.ChatMessage;
import chat.octet.model.beans.CompletionResult;
import chat.octet.model.beans.Token;
import chat.octet.model.components.criteria.StoppingCriteriaList;
import chat.octet.model.components.criteria.impl.MaxTimeCriteria;
import chat.octet.model.components.processor.LogitsProcessorList;
import chat.octet.model.components.processor.impl.CustomBiasLogitsProcessor;
import chat.octet.model.parameters.GenerateParameter;
import chat.octet.utils.CommonUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class ChatCompletionService {

    private final static GenerateParameter DEFAULT_PARAMETER = GenerateParameter.builder().build();

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
                    if (messages.size() > 2) {
                        return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                                .body(BodyInserters.fromValue("Request parameter 'messages' is too long (SIZE > 2), only include user and system messages (optional)."));
                    }
                    String system = null;
                    if (messages.size() == 2) {
                        system = messages.remove(0).getContent();
                    }
                    String user = messages.get(0).getContent();
                    return doCompletions(requestParams, system, user, startTime, true);
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
                    return doCompletions(requestParams, null, requestParams.getPrompt(), startTime, false);
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

    @Bean
    public RouterFunction<ServerResponse> embeddingFunction() {
        return RouterFunctions.route(
                RequestPredicates.POST("/v1/embedding").and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                serverRequest -> serverRequest.bodyToMono(String.class).flatMap(content -> {
                    Model model = ModelBuilder.getInstance().getModel();
                    if (!model.getModelParams().isEmbedding()) {
                        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue("Llama model must be created with embedding=True to call this method"));
                    }
                    float[] embedding = LlamaService.getEmbedding();
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(embedding));
                })
        );
    }

    @Bean
    public RouterFunction<ServerResponse> resetFunction() {
        return RouterFunctions.route(
                RequestPredicates.POST("/v1/reset").and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                serverRequest -> serverRequest.bodyToMono(ChatCompletionRequestParameter.class).flatMap(requestParams -> {
                    if ("ALL".equalsIgnoreCase(requestParams.getUser())) {
                        ModelBuilder.getInstance().getModel().removeAllChatStatus();
                    } else {
                        ModelBuilder.getInstance().getModel().removeChatStatus(requestParams.getUser());
                    }
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue("Success"));
                })
        );
    }

    @Bean
    public RouterFunction<ServerResponse> modelsFunction() {
        return RouterFunctions.route(
                RequestPredicates.GET("/v1/models").and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                serverRequest -> {
                    Map<String, Object> data = Maps.newHashMap();
                    data.put("data", ModelBuilder.getInstance().getModelsList());
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(data));
                }
        );
    }

    private GenerateParameter getGenerateParameter(ChatCompletionRequestParameter params) {
        long maxTime = TimeUnit.MINUTES.toMillis(Optional.ofNullable(params.getTimeout()).orElse(10L));
        StoppingCriteriaList stopCriteriaList = new StoppingCriteriaList(Lists.newArrayList(new MaxTimeCriteria(maxTime)));

        LogitsProcessorList logitsProcessorList = null;
        if (params.getLogitBias() != null && !params.getLogitBias().isEmpty()) {
            logitsProcessorList = new LogitsProcessorList(Lists.newArrayList(new CustomBiasLogitsProcessor(params.getLogitBias(), LlamaService.getVocabSize())));
        }

        return GenerateParameter.builder()
                .temperature(Optional.ofNullable(params.getTemperature()).orElse(DEFAULT_PARAMETER.getTemperature()))
                .topK(Optional.ofNullable(params.getTopK()).orElse(DEFAULT_PARAMETER.getTopK()))
                .topP(Optional.ofNullable(params.getTopP()).orElse(DEFAULT_PARAMETER.getTopP()))
                .minP(Optional.ofNullable(params.getMinP()).orElse(DEFAULT_PARAMETER.getMinP()))
                .tsf(Optional.ofNullable(params.getTfs()).orElse(DEFAULT_PARAMETER.getTsf()))
                .typical(Optional.ofNullable(params.getTypical()).orElse(DEFAULT_PARAMETER.getTypical()))
                .maxNewTokenSize(Optional.ofNullable(params.getMaxNewTokensSize()).orElse(DEFAULT_PARAMETER.getMaxNewTokenSize()))
                .frequencyPenalty(Optional.ofNullable(params.getFrequencyPenalty()).orElse(DEFAULT_PARAMETER.getFrequencyPenalty()))
                .presencePenalty(Optional.ofNullable(params.getPresencePenalty()).orElse(DEFAULT_PARAMETER.getPresencePenalty()))
                .repeatPenalty(Optional.ofNullable(params.getRepeatPenalty()).orElse(DEFAULT_PARAMETER.getRepeatPenalty()))
                .mirostatMode(Optional.ofNullable(params.getMirostatMode()).orElse(DEFAULT_PARAMETER.getMirostatMode()))
                .mirostatETA(Optional.ofNullable(params.getMirostatETA()).orElse(DEFAULT_PARAMETER.getMirostatETA()))
                .mirostatTAU(Optional.ofNullable(params.getMirostatTAU()).orElse(DEFAULT_PARAMETER.getMirostatTAU()))
                .stoppingCriteriaList(stopCriteriaList)
                .logitsProcessorList(logitsProcessorList)
                .verbosePrompt(params.isVerbose())
                .build();
    }

    private Mono<ServerResponse> doCompletions(ChatCompletionRequestParameter requestParams, String system, String input, long startTime, boolean chat) {
        String id = chat ? CommonUtils.randomString("octetchat") : CommonUtils.randomString("octetcmpl");
        GenerateParameter generateParams = getGenerateParameter(requestParams);
        Model model = ModelBuilder.getInstance().getModel();

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
