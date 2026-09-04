package atmin.common.exception.handler;

import atmin.common.exception.*;
import atmin.common.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import atmin.common.config.AtminExceptionProperties;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Core exception handler that handles common exceptions.
 * This handler is always active and does not depend on any optional dependencies
 * like Spring Security or JWT.
 *
 * <p>Handles:</p>
 * <ul>
 *   <li>400 - MethodArgumentNotValidException (validation errors)</li>
 *   <li>400 - HandlerMethodValidationException</li>
 *   <li>400 - malformed body and missing request values</li>
 *   <li>400 - MethodArgumentTypeMismatchException</li>
 *   <li>400 - IllegalArgumentException</li>
 *   <li>400 - BadRequestException</li>
 *   <li>401 - UnauthorizedException</li>
 *   <li>403 - ForbiddenException</li>
 *   <li>404 - ResourceNotFoundException</li>
 *   <li>409 - DuplicateResourceException</li>
 *   <li>409 - ConflictException</li>
 *   <li>415 - HttpMediaTypeNotSupportedException</li>
 *   <li>Spring MVC ErrorResponse exceptions with their original status</li>
 *   <li>500 - RuntimeException (catch-all)</li>
 *   <li>500 - checked Exception (catch-all)</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(AtminExceptionProperties.class)
public class CoreExceptionHandler {

    private final AtminExceptionProperties properties;

    // ======================== 400 Bad Request ========================

    /**
     * Handles @Valid annotation validation failures.
     * Returns field-level error details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> errorsMap = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errorsMap.putIfAbsent(error.getField(), error.getDefaultMessage())
        );

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(properties.getValidationFailed())
                .path(request.getRequestURI())
                .errors(errorsMap)
                .traceId(ApiErrorResponse.resolveTraceId())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles validation of controller method parameters introduced in modern
     * Spring MVC versions.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpServletRequest request) {
        Map<String, String> errorsMap = new LinkedHashMap<>();

        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            if (result instanceof ParameterErrors parameterErrors) {
                parameterErrors.getFieldErrors().forEach(error ->
                        errorsMap.putIfAbsent(error.getField(), error.getDefaultMessage()));
                parameterErrors.getGlobalErrors().forEach(error ->
                        errorsMap.putIfAbsent(parameterErrors.getObjectName(), error.getDefaultMessage()));
                continue;
            }

            String parameterName = result.getMethodParameter().getParameterName();
            if (parameterName == null) {
                parameterName = "arg" + result.getMethodParameter().getParameterIndex();
            }
            String finalParameterName = parameterName;
            result.getResolvableErrors().forEach(error ->
                    errorsMap.putIfAbsent(finalParameterName, error.getDefaultMessage()));
        }

        ex.getCrossParameterValidationResults().forEach(error ->
                errorsMap.putIfAbsent("_global", error.getDefaultMessage()));

        ApiErrorResponse response = ApiErrorResponse.validationError(
                request.getRequestURI(), properties.getValidationFailed(), errorsMap);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles malformed JSON and other unreadable request bodies without
     * exposing parser implementation details to API clients.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Unreadable request body at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(ApiErrorResponse.badRequest(
                properties.getMalformedRequest(), request.getRequestURI()));
    }

    /**
     * Handles missing request parameters and multipart request parts.
     */
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class
    })
    public ResponseEntity<ApiErrorResponse> handleMissingRequestValue(
            Exception ex, HttpServletRequest request) {
        log.warn("Missing request value at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(ApiErrorResponse.badRequest(
                properties.getMissingRequestValue(), request.getRequestURI()));
    }

    /**
     * Handles type mismatch in method arguments (e.g., String to int conversion).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String message = String.format(
                "Failed to convert value of type 'java.lang.String' to required type '%s' for parameter '%s'",
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown",
                ex.getName()
        );

        ApiErrorResponse errorResponse = ApiErrorResponse.of(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles IllegalArgumentException.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.warn("Invalid argument: {}", ex.getMessage());

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles custom BadRequestException.
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequestException(
            BadRequestException ex, HttpServletRequest request) {

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // ======================== 401 Unauthorized ========================

    /**
     * Handles custom UnauthorizedException.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorizedException(
            UnauthorizedException ex, HttpServletRequest request) {

        log.warn("Unauthorized access: {}", ex.getMessage());

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    // ======================== 403 Forbidden ========================

    /**
     * Handles custom ForbiddenException.
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleForbiddenException(
            ForbiddenException ex, HttpServletRequest request) {

        log.warn("Forbidden access: {}", ex.getMessage());

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    // ======================== 404 Not Found ========================

    /**
     * Handles ResourceNotFoundException.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    // ======================== 409 Conflict ========================

    /**
     * Handles DuplicateResourceException.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request) {

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handles ConflictException.
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflictException(
            ConflictException ex, HttpServletRequest request) {

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    // ======================== 415 Unsupported Media Type ========================

    /**
     * Handles unsupported media type requests.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    /**
     * Preserves status codes produced by Spring MVC instead of turning them into
     * a generic 500 response.
     */
    @ExceptionHandler({
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class,
            NoHandlerFoundException.class,
            NoResourceFoundException.class
    })
    public ResponseEntity<ApiErrorResponse> handleSpringWebError(
            Exception ex, HttpServletRequest request) {
        ErrorResponse springError = (ErrorResponse) ex;
        HttpStatusCode status = springError.getStatusCode();
        String message = springError.getBody().getDetail();
        if (message == null || message.isBlank()) {
            message = ex.getMessage();
        }
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(status, message, request.getRequestURI()));
    }

    /**
     * Preserves explicit statuses thrown with ResponseStatusException.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException ex, HttpServletRequest request) {
        String message = ex.getReason();
        if (message == null || message.isBlank()) {
            message = ex.getBody().getDetail();
        }
        return ResponseEntity.status(ex.getStatusCode())
                .body(ApiErrorResponse.of(ex.getStatusCode(), message, request.getRequestURI()));
    }

    /**
     * Handles custom Spring ErrorResponseException subclasses.
     */
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ApiErrorResponse> handleErrorResponseException(
            ErrorResponseException ex, HttpServletRequest request) {
        String message = ex.getBody().getDetail();
        if (message == null || message.isBlank()) {
            message = ex.getMessage();
        }
        return ResponseEntity.status(ex.getStatusCode())
                .body(ApiErrorResponse.of(ex.getStatusCode(), message, request.getRequestURI()));
    }

    // ======================== 503 Service Unavailable ========================

    /**
     * Handles ServiceUnavailableException.
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleServiceUnavailableException(
            ServiceUnavailableException ex, HttpServletRequest request) {

        log.error("Service unavailable: {}", ex.getMessage(), ex);

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
    }

    // ======================== 500 Internal Server Error (Catch-all) ========================

    /**
     * Catch-all handler for any unhandled RuntimeException.
     * Returns a generic error message to avoid leaking internal details.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                properties.getUnexpectedError(),
                request.getRequestURI()
        );

        log.error("TraceId={} | An unexpected error occurred at URI: {}",
                errorResponse.getTraceId(), request.getRequestURI(), ex);

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Catch-all for checked exceptions thrown by controller methods.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(
            Exception ex, HttpServletRequest request) {
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                properties.getUnexpectedError(),
                request.getRequestURI()
        );
        log.error("TraceId={} | An unexpected checked exception occurred at URI: {}",
                errorResponse.getTraceId(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
