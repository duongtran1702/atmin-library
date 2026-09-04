package atmin.common.exception.handler;

import atmin.common.config.AtminExceptionProperties;
import atmin.common.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Spring Security controller-level handlers that do not require JJWT.
 */
@Slf4j
@RequiredArgsConstructor
public class SpringSecurityExceptionHandler {

    private final AtminExceptionProperties properties;

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.unauthorized(
                        properties.getSecurityUnauthorized(), request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.forbidden(
                        properties.getAccessDenied(), request.getRequestURI()));
    }
}
