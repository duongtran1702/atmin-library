package atmin.common.exception.handler;

import atmin.common.config.AtminExceptionProperties;
import atmin.common.response.ApiErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * JJWT controller-level handler that does not require Spring Security.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtExceptionHandler {

    private final AtminExceptionProperties properties;

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiErrorResponse> handleJwtException(
            JwtException ex, HttpServletRequest request) {
        log.warn("JWT validation failed at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.unauthorized(
                        properties.getSecurityUnauthorized(), request.getRequestURI()));
    }
}
