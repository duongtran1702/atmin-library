package atmin.common.exception.handler;

import atmin.common.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import lombok.RequiredArgsConstructor;
import atmin.common.config.AtminExceptionProperties;

/**
 * Exception handler for Spring Security and JWT related exceptions.
 *
 * <p><strong>Important:</strong> This handler is only activated when Spring Security
 * and/or JWT dependencies are on the classpath. It will NOT cause ClassNotFoundException
 * if these libraries are absent.</p>
 *
 * <p><strong>Note:</strong> This class intentionally does NOT have {@code @RestControllerAdvice}
 * to prevent automatic component scanning. It is registered as a bean exclusively through
 * {@link atmin.common.config.AtminExceptionAutoConfiguration} which uses
 * {@code @ConditionalOnClass} to ensure JWT and Spring Security are present.</p>
 *
 * <p>Handles:</p>
 * <ul>
 *   <li>401 - JwtException (io.jsonwebtoken)</li>
 *   <li>401 - AuthenticationException (Spring Security)</li>
 *   <li>403 - AccessDeniedException (Spring Security)</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class SecurityExceptionHandler {

    private final AtminExceptionProperties properties;

    /**
     * Handles JWT token exceptions (expired, malformed, invalid signature, etc.)
     */
    @ExceptionHandler(io.jsonwebtoken.JwtException.class)
    public ResponseEntity<ApiErrorResponse> handleJwtException(
            io.jsonwebtoken.JwtException ex, HttpServletRequest request) {

        log.warn("JWT authentication failed: {}", ex.getMessage());

        ApiErrorResponse errorResponse = ApiErrorResponse.unauthorized(
                ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles Spring Security authentication failures
     * (e.g., BadCredentialsException, UsernameNotFoundException).
     */
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            org.springframework.security.core.AuthenticationException ex, HttpServletRequest request) {

        log.warn("Authentication failed: {}", ex.getMessage());

        ApiErrorResponse errorResponse = ApiErrorResponse.unauthorized(
                ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles Spring Security access denied (insufficient permissions).
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request) {

        log.warn("Access denied: {}", ex.getMessage());

        ApiErrorResponse errorResponse = ApiErrorResponse.forbidden(
                properties.getAccessDenied(), request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }
}
