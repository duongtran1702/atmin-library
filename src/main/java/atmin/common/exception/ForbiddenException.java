package atmin.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when the user does not have permission to access a resource.
 * Maps to HTTP 403 Forbidden.
 *
 * <p>Usage:</p>
 * <pre>
 * throw new ForbiddenException("You do not have permission to access this resource");
 * </pre>
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
