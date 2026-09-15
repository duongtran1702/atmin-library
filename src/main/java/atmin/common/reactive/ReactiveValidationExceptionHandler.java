package atmin.common.reactive;

import atmin.common.config.AtminExceptionProperties;
import atmin.common.response.ApiErrorResponse;
import atmin.common.trace.TraceIdResolver;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/** Optional Bean Validation handler for WebFlux applications. */
@RequiredArgsConstructor
public class ReactiveValidationExceptionHandler {

    private final AtminExceptionProperties properties;

    @ExceptionHandler(ConstraintViolationException.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleConstraintViolation(
            ConstraintViolationException ex, ServerWebExchange exchange) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                errors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));
        String traceId = resolveTraceId(exchange);
        ApiErrorResponse body = ApiErrorResponse.validationError(
                exchange.getRequest().getPath().value(),
                properties.getValidationFailed(),
                errors,
                traceId);
        return Mono.just(ResponseEntity.badRequest().body(body));
    }

    private String resolveTraceId(ServerWebExchange exchange) {
        Object value = exchange.getAttribute(AtminReactiveTraceFilter.TRACE_ID_ATTRIBUTE);
        String candidate = value instanceof String string ? string
                : exchange.getRequest().getHeaders().getFirst(properties.getTraceIdHeader());
        return TraceIdResolver.resolve(candidate);
    }
}
