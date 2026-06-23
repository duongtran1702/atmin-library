package atmin.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class CloudStorageException extends RuntimeException {
    public CloudStorageException(String message) {
        super(message);
    }

    public CloudStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}