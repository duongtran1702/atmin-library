package atmin.common.reactive;

import atmin.common.config.AtminExceptionProperties;
import atmin.common.response.ApiErrorResponse;
import atmin.common.trace.TraceIdResolver;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Optional JJWT handler for WebFlux controllers. */
@Slf4j
@RequiredArgsConstructor
public class ReactiveJwtExceptionHandler {

    private final AtminExceptionProperties properties;

    @ExceptionHandler(JwtException.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleJwtException(
            JwtException ex, ServerWebExchange exchange) {
        log.warn("Reactive JWT authentication failed: {}", ex.getMessage());
        Object value = exchange.getAttribute(AtminReactiveTraceFilter.TRACE_ID_ATTRIBUTE);
        String candidate = value instanceof String string ? string
                : exchange.getRequest().getHeaders().getFirst(properties.getTraceIdHeader());
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.UNAUTHORIZED,
                properties.getSecurityUnauthorized(),
                exchange.getRequest().getPath().value(),
                TraceIdResolver.resolve(candidate));
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body));
    }
}
