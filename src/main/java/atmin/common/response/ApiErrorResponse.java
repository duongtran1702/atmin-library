package atmin.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Standard error response format for REST APIs.
 * Provides a consistent error structure across all endpoints.
 *
 * <p>Every error response automatically includes a {@code traceId} for distributed
 * tracing and log correlation. The traceId is resolved in the following order:</p>
 * <ol>
 *   <li>From SLF4J MDC using a configurable key (default: {@code "traceId"})</li>
 *   <li>If MDC has no value, a random UUID is generated as fallback</li>
 * </ol>
 *
 * <p>To customize the MDC key (e.g., for Spring Cloud Sleuth or Micrometer Tracing),
 * call {@link #setMdcKey(String)} at application startup:</p>
 * <pre>
 * ApiErrorResponse.setMdcKey("correlationId");
 * </pre>
 *
 * <p>Example JSON output:</p>
 * <pre>
 * {
 *   "timestamp": "2026-06-19T14:30:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "User not found with id: 1",
 *   "path": "/api/users/1",
 *   "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 * }
 * </pre>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {

    /**
     * The MDC key used to resolve trace IDs.
     * Default is "traceId". Can be changed via {@link #setMdcKey(String)}.
     */
    private static volatile String mdcKey = "traceId";

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private int status;
    private String error;
    private String message;
    private String path;

    /**
     * Validation errors map (field → error message).
     * Only included in response when not null (e.g., for @Valid failures).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> errors;

    /**
     * Trace ID for distributed tracing and log correlation.
     * Automatically populated from MDC or generated as UUID.
     * Use this value to search server logs when debugging client-reported errors.
     */
    private String traceId;

    // ======================== Trace ID Resolution ========================

    /**
     * Resolve the trace ID from SLF4J MDC or generate a UUID fallback.
     *
     * <p>Resolution order:</p>
     * <ol>
     *   <li>Read from MDC using the configured key (default: "traceId")</li>
     *   <li>If MDC value is null or blank, generate {@code UUID.randomUUID()}</li>
     * </ol>
     *
     * @return a non-null trace ID string
     */
    public static String resolveTraceId() {
        String traceId = MDC.get(mdcKey);
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        return UUID.randomUUID().toString();
    }

    /**
     * Set the MDC key used to resolve trace IDs.
     * Call this method at application startup if your tracing system uses
     * a different MDC key (e.g., "correlationId", "X-Request-Id", "spanId").
     *
     * <p>Example in a @PostConstruct or @Bean:</p>
     * <pre>
     * ApiErrorResponse.setMdcKey("correlationId");
     * </pre>
     *
     * @param key the MDC key name (must not be null)
     */
    public static void setMdcKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("MDC key must not be null or blank");
        }
        mdcKey = key;
    }

    /**
     * Get the currently configured MDC key.
     *
     * @return the MDC key used for trace ID resolution
     */
    public static String getMdcKey() {
        return mdcKey;
    }

    // ======================== Factory Methods ========================

    /**
     * Create an error response from HttpStatus with a custom message.
     * TraceId is automatically resolved from MDC or generated as UUID.
     *
     * @param httpStatus the HTTP status
     * @param message    the error message
     * @param path       the request URI path
     * @return ApiErrorResponse instance with auto-populated traceId
     */
    public static ApiErrorResponse of(HttpStatus httpStatus, String message, String path) {
        return ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(httpStatus.value())
                .error(httpStatus.getReasonPhrase())
                .message(message)
                .path(path)
                .traceId(resolveTraceId())
                .build();
    }

    /**
     * Create a 400 Bad Request error response.
     */
    public static ApiErrorResponse badRequest(String message, String path) {
        return of(HttpStatus.BAD_REQUEST, message, path);
    }

    /**
     * Create a 401 Unauthorized error response.
     */
    public static ApiErrorResponse unauthorized(String message, String path) {
        return of(HttpStatus.UNAUTHORIZED, message, path);
    }

    /**
     * Create a 403 Forbidden error response.
     */
    public static ApiErrorResponse forbidden(String message, String path) {
        return of(HttpStatus.FORBIDDEN, message, path);
    }

    /**
     * Create a 404 Not Found error response.
     */
    public static ApiErrorResponse notFound(String message, String path) {
        return of(HttpStatus.NOT_FOUND, message, path);
    }

    /**
     * Create a 409 Conflict error response.
     */
    public static ApiErrorResponse conflict(String message, String path) {
        return of(HttpStatus.CONFLICT, message, path);
    }

    /**
     * Create a 500 Internal Server Error response.
     */
    public static ApiErrorResponse internalServerError(String message, String path) {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, message, path);
    }

    /**
     * Create a 503 Service Unavailable error response.
     */
    public static ApiErrorResponse serviceUnavailable(String message, String path) {
        return of(HttpStatus.SERVICE_UNAVAILABLE, message, path);
    }

    /**
     * Create a validation error response with field-level errors.
     * TraceId is automatically resolved from MDC or generated as UUID.
     *
     * @param path   the request URI path
     * @param errors map of field name → error message
     * @return ApiErrorResponse with validation errors and auto-populated traceId
     */
    public static ApiErrorResponse validationError(String path, Map<String, String> errors) {
        return ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .path(path)
                .errors(errors)
                .traceId(resolveTraceId())
                .build();
    }
}
