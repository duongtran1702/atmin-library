package atmin.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a resource conflict occurs (e.g., duplicate data).
 * Maps to HTTP 409 Conflict.
 * This is an alias/alternative to {@link DuplicateResourceException}.
 *
 * <p>Usage:</p>
 * <pre>
 * throw new ConflictException("A user with this email already exists");
 * </pre>
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
