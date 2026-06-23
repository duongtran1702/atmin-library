package atmin.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when authentication is required but not provided or invalid.
 * Maps to HTTP 401 Unauthorized.
 *
 * <p>Usage:</p>
 * <pre>
 * throw new UnauthorizedException("Invalid or expired token");
 * </pre>
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
