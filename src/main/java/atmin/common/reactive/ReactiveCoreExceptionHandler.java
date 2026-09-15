package atmin.common.reactive;

import atmin.common.config.AtminExceptionProperties;
import atmin.common.exception.*;
import atmin.common.response.ApiErrorResponse;
import atmin.common.response.AtminErrorCode;
import atmin.common.trace.TraceIdResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import javax.net.ssl.SSLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/** Exception handler for annotated Spring WebFlux controllers. */
@Slf4j
@RequiredArgsConstructor
public class ReactiveCoreExceptionHandler {

    private final AtminExceptionProperties properties;

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleWebExchangeBindException(
            WebExchangeBindException ex, ServerWebExchange exchange) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ex.getGlobalErrors().forEach(error ->
                errors.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
        return validationResponse(errors, exchange);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleMethodValidation(
            HandlerMethodValidationException ex, ServerWebExchange exchange) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            if (result instanceof ParameterErrors parameterErrors) {
                parameterErrors.getFieldErrors().forEach(error ->
                        errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
                continue;
            }
            String name = result.getMethodParameter().getParameterName();
            if (name == null) {
                name = "arg" + result.getMethodParameter().getParameterIndex();
            }
            String parameterName = name;
            result.getResolvableErrors().forEach(error ->
                    errors.putIfAbsent(parameterName, error.getDefaultMessage()));
        }
        ex.getCrossParameterValidationResults().forEach(error ->
                errors.putIfAbsent("_global", error.getDefaultMessage()));
        return validationResponse(errors, exchange);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleWebInput(
            ServerWebInputException ex, ServerWebExchange exchange) {
        log.warn("Invalid reactive request at {}: {}", path(exchange), ex.getMessage());
        return response(HttpStatus.BAD_REQUEST, properties.getMalformedRequest(), exchange,
                AtminErrorCode.MALFORMED_REQUEST, false, null);
    }

    @ExceptionHandler(DataBufferLimitException.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handlePayloadTooLarge(
            DataBufferLimitException ex, ServerWebExchange exchange) {
        log.warn("Reactive payload exceeded the configured buffer limit at {}", path(exchange));
        return response(HttpStatus.CONTENT_TOO_LARGE, properties.getFileTooLarge(), exchange,
                AtminErrorCode.PAYLOAD_TOO_LARGE, false, null);
    }

    @ExceptionHandler({BadRequestException.class, IllegalArgumentException.class})
    public Mono<ResponseEntity<ApiErrorResponse>> handleBadRequest(
            RuntimeException ex, ServerWebExchange exchange) {
        return response(HttpStatus.BAD_REQUEST, ex.getMessage(), exchange,
                AtminErrorCode.MALFORMED_REQUEST, false, null);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleUnauthorized(
            UnauthorizedException ex, ServerWebExchange exchange) {
        return response(HttpStatus.UNAUTHORIZED, ex.getMessage(), exchange,
                AtminErrorCode.UNAUTHORIZED, false, null);
    }

    @ExceptionHandler(ForbiddenException.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleForbidden(
            ForbiddenException ex, ServerWebExchange exchange) {
        return response(HttpStatus.FORBIDDEN, ex.getMessage(), exchange,
                AtminErrorCode.FORBIDDEN, false, null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleNotFound(
            ResourceNotFoundException ex, ServerWebExchange exchange) {
        return response(HttpStatus.NOT_FOUND, ex.getMessage(), exchange,
                AtminErrorCode.RESOURCE_NOT_FOUND, false, null);
    }

    @ExceptionHandler({DuplicateResourceException.class, ConflictException.class})
    public Mono<ResponseEntity<ApiErrorResponse>> handleConflict(
            RuntimeException ex, ServerWebExchange exchange) {
        return response(HttpStatus.CONFLICT, ex.getMessage(), exchange,
                AtminErrorCode.CONFLICT, false, null);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleRateLimit(
            RateLimitExceededException ex, ServerWebExchange exchange) {
        return Mono.fromSupplier(() -> {
            ApiErrorResponse body = error(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), exchange,
                    AtminErrorCode.RATE_LIMIT_EXCEEDED, true, null);
            ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS);
            if (ex.getRetryAfterSeconds() != null) {
                builder.header(HttpHeaders.RETRY_AFTER, ex.getRetryAfterSeconds().toString());
            }
            return builder.body(body);
        });
    }

    @ExceptionHandler(DownstreamServiceException.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleDownstream(
            DownstreamServiceException ex, ServerWebExchange exchange) {
        log.error("Reactive downstream failure at {}", path(exchange), ex);
        return response(HttpStatus.BAD_GATEWAY, properties.getDownstreamServiceError(), exchange,
                AtminErrorCode.DOWNSTREAM_BAD_RESPONSE, true, exposedService(ex.getServiceName()));
    }

    @ExceptionHandler({ServiceUnavailableException.class, CircuitBreakerOpenException.class})
    public Mono<ResponseEntity<ApiErrorResponse>> handleUnavailable(
            RuntimeException ex, ServerWebExchange exchange) {
        log.error("Reactive dependency unavailable at {}", path(exchange), ex);
        String service = ex instanceof CircuitBreakerOpenException circuit
                ? exposedService(circuit.getServiceName()) : null;
        AtminErrorCode code = ex instanceof CircuitBreakerOpenException
                ? AtminErrorCode.CIRCUIT_BREAKER_OPEN : AtminErrorCode.DOWNSTREAM_UNAVAILABLE;
        return response(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), exchange, code, true, service);
    }

    @ExceptionHandler(GatewayTimeoutException.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleGatewayTimeout(
            GatewayTimeoutException ex, ServerWebExchange exchange) {
        log.error("Reactive downstream timeout at {}", path(exchange), ex);
        return response(HttpStatus.GATEWAY_TIMEOUT, ex.getMessage(), exchange,
                AtminErrorCode.DOWNSTREAM_TIMEOUT, true, null);
    }

    @ExceptionHandler(WebClientResponseException.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleWebClientResponse(
            WebClientResponseException ex, ServerWebExchange exchange) {
        log.error("Reactive downstream HTTP error at {}: status={}",
                path(exchange), ex.getStatusCode(), ex);
        HttpStatusCode status = downstreamStatus(ex.getStatusCode());
        return responseWithRetryAfter(status, properties.getDownstreamServiceError(), exchange,
                status.value() == 429 ? AtminErrorCode.RATE_LIMIT_EXCEEDED
                        : AtminErrorCode.DOWNSTREAM_BAD_RESPONSE,
                status.is5xxServerError() || status.value() == 429,
                ex.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
    }

    @ExceptionHandler(WebClientRequestException.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleWebClientRequest(
            WebClientRequestException ex, ServerWebExchange exchange) {
        return transportResponse(ex, exchange);
    }

    @ExceptionHandler({
            UnknownHostException.class,
            ConnectException.class,
            NoRouteToHostException.class,
            SocketTimeoutException.class,
            HttpTimeoutException.class,
            SSLException.class
    })
    public Mono<ResponseEntity<ApiErrorResponse>> handleNetworkException(
            Exception ex, ServerWebExchange exchange) {
        return transportResponse(ex, exchange);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleResponseStatus(
            ResponseStatusException ex, ServerWebExchange exchange) {
        HttpStatusCode status = ex.getStatusCode();
        String message = status.is5xxServerError() ? properties.getUnexpectedError() : ex.getReason();
        if (message == null || message.isBlank()) {
            message = ex.getBody().getDetail();
        }
        return response(status, message, exchange);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleUnexpected(
            Exception ex, ServerWebExchange exchange) {
        ApiErrorResponse body = error(HttpStatus.INTERNAL_SERVER_ERROR,
                properties.getUnexpectedError(), exchange, AtminErrorCode.INTERNAL_ERROR, false, null);
        log.error("TraceId={} | Unexpected reactive error at {}", body.getTraceId(), path(exchange), ex);
        return Mono.just(ResponseEntity.internalServerError().body(body));
    }

    private Mono<ResponseEntity<ApiErrorResponse>> validationResponse(
            Map<String, String> errors, ServerWebExchange exchange) {
        String traceId = traceId(exchange);
        ApiErrorResponse body = ApiErrorResponse.validationError(
                path(exchange), properties.getValidationFailed(), errors, traceId);
        body.setCode(AtminErrorCode.VALIDATION_FAILED);
        body.setRetryable(false);
        return Mono.just(ResponseEntity.badRequest().body(body));
    }

    private Mono<ResponseEntity<ApiErrorResponse>> transportResponse(
            Throwable ex, ServerWebExchange exchange) {
        boolean timeout = hasCause(ex, SocketTimeoutException.class)
                || hasCause(ex, HttpTimeoutException.class)
                || hasCause(ex, TimeoutException.class);
        HttpStatus status = timeout ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.SERVICE_UNAVAILABLE;
        String message = timeout ? properties.getDownstreamTimeout()
                : properties.getDownstreamServiceUnavailable();
        log.error("Reactive transport failure at {}: timeout={}", path(exchange), timeout, ex);
        return response(status, message, exchange,
                timeout ? AtminErrorCode.DOWNSTREAM_TIMEOUT : AtminErrorCode.DOWNSTREAM_UNAVAILABLE,
                true, null);
    }

    private Mono<ResponseEntity<ApiErrorResponse>> response(
            HttpStatusCode status, String message, ServerWebExchange exchange) {
        return Mono.fromSupplier(() -> ResponseEntity.status(status).body(error(status, message, exchange)));
    }

    private Mono<ResponseEntity<ApiErrorResponse>> response(
            HttpStatusCode status, String message, ServerWebExchange exchange,
            AtminErrorCode code, boolean retryable, String service) {
        return Mono.fromSupplier(() -> ResponseEntity.status(status)
                .body(error(status, message, exchange, code, retryable, service)));
    }

    private Mono<ResponseEntity<ApiErrorResponse>> responseWithRetryAfter(
            HttpStatusCode status, String message, ServerWebExchange exchange,
            AtminErrorCode code, boolean retryable, String retryAfter) {
        return Mono.fromSupplier(() -> {
            ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
            if (status.value() == 429 && retryAfter != null && retryAfter.matches("[0-9]{1,10}")) {
                builder.header(HttpHeaders.RETRY_AFTER, retryAfter);
            }
            return builder.body(error(status, message, exchange, code, retryable, null));
        });
    }

    private ApiErrorResponse error(
            HttpStatusCode status, String message, ServerWebExchange exchange) {
        return ApiErrorResponse.of(status, message, path(exchange), traceId(exchange));
    }

    private ApiErrorResponse error(
            HttpStatusCode status, String message, ServerWebExchange exchange,
            AtminErrorCode code, boolean retryable, String service) {
        return ApiErrorResponse.of(status, message, path(exchange), traceId(exchange),
                code, retryable, service);
    }

    private HttpStatusCode downstreamStatus(HttpStatusCode upstreamStatus) {
        if (!properties.isDownstreamPreserveClientErrors()) {
            return HttpStatus.BAD_GATEWAY;
        }
        return switch (upstreamStatus.value()) {
            case 400, 404, 409, 422, 429 -> upstreamStatus;
            default -> HttpStatus.BAD_GATEWAY;
        };
    }

    private String exposedService(String serviceName) {
        return properties.isDownstreamExposeServiceName() ? serviceName : null;
    }

    private String traceId(ServerWebExchange exchange) {
        Object value = exchange.getAttribute(AtminReactiveTraceFilter.TRACE_ID_ATTRIBUTE);
        String candidate = value instanceof String string ? string
                : exchange.getRequest().getHeaders().getFirst(properties.getTraceIdHeader());
        String resolved = TraceIdResolver.resolve(candidate);
        if (properties.isEchoTraceIdHeader() && !exchange.getResponse().isCommitted()) {
            exchange.getResponse().getHeaders().set(properties.getTraceIdHeader(), resolved);
        }
        return resolved;
    }

    private String path(ServerWebExchange exchange) {
        return exchange.getRequest().getPath().value();
    }

    private boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        Throwable current = error;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
