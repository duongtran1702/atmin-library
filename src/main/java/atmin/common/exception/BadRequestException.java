package atmin.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a request contains invalid data.
 * Maps to HTTP 400 Bad Request.
 *
 * <p>Usage:</p>
 * <pre>
 * throw new BadRequestException("Email format is invalid");
 * </pre>
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
