package atmin.common.reactive;

import atmin.common.config.AtminExceptionProperties;
import atmin.common.exception.*;
import atmin.common.response.ApiErrorResponse;
import atmin.common.response.AtminErrorCode;
import atmin.common.trace.TraceIdResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import javax.net.ssl.SSLException;
import java.util.concurrent.TimeoutException;

/**
 * Optional low-level JSON error handler for failures raised in a Spring Cloud
 * Gateway filter chain before a controller can handle them.
 */
@Slf4j
@RequiredArgsConstructor
public class AtminGatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler, Ordered {

    private final ObjectMapper objectMapper;
    private final AtminExceptionProperties properties;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(error);
        }

        Mapping mapping = map(error);
        if (mapping == null) {
            return Mono.error(error);
        }

        String traceId = traceId(exchange);
        ApiErrorResponse body = ApiErrorResponse.of(
                mapping.status(), mapping.message(), exchange.getRequest().getPath().value(), traceId,
                mapping.code(), mapping.retryable(), mapping.service());
        logIfServerError(error, mapping.status(), traceId, exchange);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            exchange.getResponse().setStatusCode(mapping.status());
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            if (properties.isEchoTraceIdHeader()) {
                exchange.getResponse().getHeaders().set(properties.getTraceIdHeader(), traceId);
            }
            if (error instanceof RateLimitExceededException rateLimit
                    && rateLimit.getRetryAfterSeconds() != null) {
                exchange.getResponse().getHeaders().set(
                        HttpHeaders.RETRY_AFTER, rateLimit.getRetryAfterSeconds().toString());
            }
            else if (mapping.status().value() == 429 && mapping.retryAfter() != null) {
                exchange.getResponse().getHeaders().set(HttpHeaders.RETRY_AFTER, mapping.retryAfter());
            }
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
        catch (Exception serializationFailure) {
            return Mono.error(serializationFailure);
        }
    }

    @Override
    public int getOrder() {
        return properties.getGatewayOrder();
    }

    private Mapping map(Throwable error) {
        if (error instanceof RateLimitExceededException rateLimit) {
            return mapping(HttpStatus.TOO_MANY_REQUESTS, rateLimit.getMessage(),
                    AtminErrorCode.RATE_LIMIT_EXCEEDED, true, null, null);
        }
        if (error instanceof DownstreamServiceException downstream) {
            return mapping(HttpStatus.BAD_GATEWAY, properties.getDownstreamServiceError(),
                    AtminErrorCode.DOWNSTREAM_BAD_RESPONSE, true,
                    exposedService(downstream.getServiceName()), null);
        }
        if (error instanceof WebClientResponseException response) {
            HttpStatusCode status = downstreamStatus(response.getStatusCode());
            String retryAfter = safeRetryAfter(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
            return mapping(status, properties.getDownstreamServiceError(),
                    status.value() == 429 ? AtminErrorCode.RATE_LIMIT_EXCEEDED
                            : AtminErrorCode.DOWNSTREAM_BAD_RESPONSE,
                    status.is5xxServerError() || status.value() == 429, null, retryAfter);
        }
        if (error instanceof GatewayTimeoutException || hasTimeoutCause(error)) {
            return mapping(HttpStatus.GATEWAY_TIMEOUT, properties.getDownstreamTimeout(),
                    AtminErrorCode.DOWNSTREAM_TIMEOUT, true, null, null);
        }
        if (error instanceof ServiceUnavailableException
                || error instanceof CircuitBreakerOpenException
                || error instanceof WebClientRequestException
                || hasTransportCause(error)) {
            String service = error instanceof CircuitBreakerOpenException circuit
                    ? exposedService(circuit.getServiceName()) : null;
            AtminErrorCode code = error instanceof CircuitBreakerOpenException
                    ? AtminErrorCode.CIRCUIT_BREAKER_OPEN : AtminErrorCode.DOWNSTREAM_UNAVAILABLE;
            return mapping(HttpStatus.SERVICE_UNAVAILABLE,
                    properties.getDownstreamServiceUnavailable(), code, true, service, null);
        }
        if (error instanceof ResponseStatusException responseStatus) {
            HttpStatusCode status = responseStatus.getStatusCode();
            String message = status.is5xxServerError()
                    ? properties.getUnexpectedError()
                    : responseStatus.getReason();
            if (message == null || message.isBlank()) {
                message = responseStatus.getBody().getDetail();
            }
            return mapping(status, message, null, false, null, null);
        }
        return null;
    }

    private boolean hasTimeoutCause(Throwable error) {
        return hasCause(error, SocketTimeoutException.class)
                || hasCause(error, HttpTimeoutException.class)
                || hasCause(error, TimeoutException.class);
    }

    private boolean hasTransportCause(Throwable error) {
        return hasCause(error, UnknownHostException.class)
                || hasCause(error, ConnectException.class)
                || hasCause(error, NoRouteToHostException.class)
                || hasCause(error, SSLException.class);
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

    private String traceId(ServerWebExchange exchange) {
        Object attribute = exchange.getAttribute(AtminReactiveTraceFilter.TRACE_ID_ATTRIBUTE);
        String candidate = attribute instanceof String string ? string
                : exchange.getRequest().getHeaders().getFirst(properties.getTraceIdHeader());
        return TraceIdResolver.resolve(candidate);
    }

    private void logIfServerError(
            Throwable error, HttpStatusCode status, String traceId, ServerWebExchange exchange) {
        if (status.is5xxServerError()) {
            log.error("TraceId={} | Gateway failure at {} mapped to {}",
                    traceId, exchange.getRequest().getPath().value(), status.value(), error);
        }
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

    private String safeRetryAfter(String value) {
        return value != null && value.matches("[0-9]{1,10}") ? value : null;
    }

    private Mapping mapping(HttpStatusCode status, String message, AtminErrorCode code,
                            boolean retryable, String service, String retryAfter) {
        return new Mapping(status, message, code, retryable, service, retryAfter);
    }

    private record Mapping(HttpStatusCode status, String message, AtminErrorCode code,
                           boolean retryable, String service, String retryAfter) {
    }
}
