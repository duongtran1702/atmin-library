package atmin.common.exception.handler;

import atmin.common.config.AtminExceptionProperties;
import atmin.common.exception.CircuitBreakerOpenException;
import atmin.common.exception.DownstreamServiceException;
import atmin.common.exception.GatewayTimeoutException;
import atmin.common.exception.RateLimitExceededException;
import atmin.common.response.ApiErrorResponse;
import atmin.common.response.AtminErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLException;

/**
 * Handles failures that commonly occur at microservice HTTP boundaries.
 * The handler only depends on Spring Web and is vendor-neutral for circuit breakers.
 */
@Slf4j
@RequiredArgsConstructor
public class MicroserviceExceptionHandler {

    private final AtminExceptionProperties properties;

    @ExceptionHandler(DownstreamServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleDownstreamServiceException(
            DownstreamServiceException ex, HttpServletRequest request) {
        log.error("Downstream service failure at {}: service={}, upstreamStatus={}",
                request.getRequestURI(), ex.getServiceName(), ex.getUpstreamStatus(), ex);
        String message = ex.getServiceName() != null && !properties.isDownstreamExposeServiceName()
                ? properties.getDownstreamServiceError() : ex.getMessage();
        return response(HttpStatus.BAD_GATEWAY, message, request,
                AtminErrorCode.DOWNSTREAM_BAD_RESPONSE, true, exposedService(ex.getServiceName()));
    }

    @ExceptionHandler(GatewayTimeoutException.class)
    public ResponseEntity<ApiErrorResponse> handleGatewayTimeoutException(
            GatewayTimeoutException ex, HttpServletRequest request) {
        log.error("Downstream timeout at {}: service={}",
                request.getRequestURI(), ex.getServiceName(), ex);
        return response(HttpStatus.GATEWAY_TIMEOUT, ex.getMessage(), request,
                AtminErrorCode.DOWNSTREAM_TIMEOUT, true, exposedService(ex.getServiceName()));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimitExceededException(
            RateLimitExceededException ex, HttpServletRequest request) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS);
        if (ex.getRetryAfterSeconds() != null) {
            builder.header(HttpHeaders.RETRY_AFTER, ex.getRetryAfterSeconds().toString());
        }
        return builder.body(ApiErrorResponse.of(
                HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request.getRequestURI(),
                ApiErrorResponse.resolveTraceId(), AtminErrorCode.RATE_LIMIT_EXCEEDED, true, null));
    }

    @ExceptionHandler(CircuitBreakerOpenException.class)
    public ResponseEntity<ApiErrorResponse> handleCircuitBreakerOpenException(
            CircuitBreakerOpenException ex, HttpServletRequest request) {
        log.warn("Circuit breaker rejected request at {}: service={}",
                request.getRequestURI(), ex.getServiceName());
        return response(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request,
                AtminErrorCode.CIRCUIT_BREAKER_OPEN, true, exposedService(ex.getServiceName()));
    }

    /**
     * Preserves selected client statuses and converts all other downstream
     * responses into a safe 502. The downstream response body is never exposed.
     */
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ApiErrorResponse> handleRestClientResponseException(
            RestClientResponseException ex, HttpServletRequest request) {
        log.error("Downstream HTTP error at {}: status={}",
                request.getRequestURI(), ex.getStatusCode(), ex);
        HttpStatusCode publicStatus = downstreamStatus(ex.getStatusCode());
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(publicStatus);
        String retryAfter = ex.getResponseHeaders() != null
                ? ex.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER) : null;
        if (publicStatus.value() == HttpStatus.TOO_MANY_REQUESTS.value()
                && retryAfter != null && retryAfter.matches("[0-9]{1,10}")) {
            builder.header(HttpHeaders.RETRY_AFTER, retryAfter);
        }
        return builder.body(ApiErrorResponse.of(
                publicStatus,
                properties.getDownstreamServiceError(),
                request.getRequestURI(),
                ApiErrorResponse.resolveTraceId(),
                publicStatus.value() == HttpStatus.TOO_MANY_REQUESTS.value()
                        ? AtminErrorCode.RATE_LIMIT_EXCEEDED : AtminErrorCode.DOWNSTREAM_BAD_RESPONSE,
                publicStatus.is5xxServerError() || publicStatus.value() == HttpStatus.TOO_MANY_REQUESTS.value(),
                null));
    }

    /**
     * Converts synchronous HTTP client I/O failures to 504 for timeouts and 503
     * for connection, DNS, TLS, and other transport failures.
     */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceAccessException(
            ResourceAccessException ex, HttpServletRequest request) {
        boolean timeout = hasTimeoutCause(ex);
        HttpStatus status = timeout ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.SERVICE_UNAVAILABLE;
        String message = timeout
                ? properties.getDownstreamTimeout()
                : properties.getDownstreamServiceUnavailable();
        log.error("Downstream transport error at {}: timeout={}", request.getRequestURI(), timeout, ex);
        return response(status, message, request,
                timeout ? AtminErrorCode.DOWNSTREAM_TIMEOUT : AtminErrorCode.DOWNSTREAM_UNAVAILABLE,
                true, null);
    }

    /**
     * Handles DNS resolution and direct connection failures that were not
     * wrapped by RestClient or RestTemplate.
     */
    @ExceptionHandler({
            UnknownHostException.class,
            ConnectException.class,
            NoRouteToHostException.class,
            SSLException.class
    })
    public ResponseEntity<ApiErrorResponse> handleDirectNetworkFailure(
            Exception ex, HttpServletRequest request) {
        log.error("Direct downstream network error at {}: type={}",
                request.getRequestURI(), ex.getClass().getSimpleName(), ex);
        return response(
                HttpStatus.SERVICE_UNAVAILABLE,
                properties.getDownstreamServiceUnavailable(),
                request,
                AtminErrorCode.DOWNSTREAM_UNAVAILABLE,
                true,
                null);
    }

    /**
     * Handles direct JDK socket and HTTP-client timeouts that were not wrapped
     * by a Spring HTTP client.
     */
    @ExceptionHandler({SocketTimeoutException.class, HttpTimeoutException.class})
    public ResponseEntity<ApiErrorResponse> handleDirectNetworkTimeout(
            Exception ex, HttpServletRequest request) {
        log.error("Direct downstream timeout at {}: type={}",
                request.getRequestURI(), ex.getClass().getSimpleName(), ex);
        return response(HttpStatus.GATEWAY_TIMEOUT, properties.getDownstreamTimeout(), request,
                AtminErrorCode.DOWNSTREAM_TIMEOUT, true, null);
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatusCode status,
            String message,
            HttpServletRequest request,
            AtminErrorCode code,
            boolean retryable,
            String service) {
        return ResponseEntity.status(status).body(ApiErrorResponse.of(
                status, message, request.getRequestURI(), ApiErrorResponse.resolveTraceId(),
                code, retryable, service));
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

    private boolean hasTimeoutCause(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SocketTimeoutException
                    || current instanceof HttpTimeoutException
                    || current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
