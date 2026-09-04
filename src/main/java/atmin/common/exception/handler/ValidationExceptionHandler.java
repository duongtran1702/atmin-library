package atmin.common.exception.handler;

import atmin.common.config.AtminExceptionProperties;
import atmin.common.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles Bean Validation failures raised outside request-body binding.
 */
@RequiredArgsConstructor
public class ValidationExceptionHandler {

    private final AtminExceptionProperties properties;

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                errors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));

        ApiErrorResponse response = ApiErrorResponse.validationError(
                request.getRequestURI(), properties.getValidationFailed(), errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
