package atmin.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when attempting to create a resource that already exists.
 * Maps to HTTP 409 Conflict.
 *
 * <p>Usage:</p>
 * <pre>
 * throw new DuplicateResourceException("User with email 'abc@mail.com' already exists");
 * </pre>
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}