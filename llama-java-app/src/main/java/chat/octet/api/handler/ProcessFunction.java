package chat.octet.api.handler;


import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public interface ProcessFunction<T> {

    Mono<ServerResponse> process(T params);
}
