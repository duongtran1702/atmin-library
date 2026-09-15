package atmin.common.exception.handler;

import atmin.common.config.AtminExceptionProperties;
import atmin.common.response.ApiErrorResponse;
import atmin.common.response.AtminErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * Optional handler for MVC applications that use WebClient for downstream calls.
 */
@Slf4j
@RequiredArgsConstructor
public class WebClientExceptionHandler {

    private final AtminExceptionProperties properties;

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ApiErrorResponse> handleWebClientResponseException(
            WebClientResponseException ex, HttpServletRequest request) {
        log.error("Reactive downstream HTTP error at {}: status={}",
                request.getRequestURI(), ex.getStatusCode(), ex);
        HttpStatusCode status = downstreamStatus(ex.getStatusCode());
        ResponseEntity<ApiErrorResponse> response = response(status, properties.getDownstreamServiceError(), request,
                status.value() == HttpStatus.TOO_MANY_REQUESTS.value()
                        ? AtminErrorCode.RATE_LIMIT_EXCEEDED : AtminErrorCode.DOWNSTREAM_BAD_RESPONSE,
                status.is5xxServerError() || status.value() == HttpStatus.TOO_MANY_REQUESTS.value());
        String retryAfter = ex.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        if (status.value() == HttpStatus.TOO_MANY_REQUESTS.value() && isSafeRetryAfter(retryAfter)) {
            return ResponseEntity.status(status)
                    .header(HttpHeaders.RETRY_AFTER, retryAfter)
                    .body(response.getBody());
        }
        return response;
    }

    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleWebClientRequestException(
            WebClientRequestException ex, HttpServletRequest request) {
        boolean timeout = hasTimeoutCause(ex);
        HttpStatus status = timeout ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.SERVICE_UNAVAILABLE;
        String message = timeout
                ? properties.getDownstreamTimeout()
                : properties.getDownstreamServiceUnavailable();
        log.error("Reactive downstream transport error at {}: timeout={}",
                request.getRequestURI(), timeout, ex);
        return response(status, message, request,
                timeout ? AtminErrorCode.DOWNSTREAM_TIMEOUT : AtminErrorCode.DOWNSTREAM_UNAVAILABLE,
                true);
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatusCode status,
            String message,
            HttpServletRequest request,
            AtminErrorCode code,
            boolean retryable) {
        return ResponseEntity.status(status).body(ApiErrorResponse.of(
                status, message, request.getRequestURI(), ApiErrorResponse.resolveTraceId(),
                code, retryable, null));
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

    private boolean isSafeRetryAfter(String value) {
        return value != null && value.matches("[0-9]{1,10}");
    }
}
