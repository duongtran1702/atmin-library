package atmin.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Signals that a caller has exceeded an application rate limit.
 * Maps to HTTP 429 Too Many Requests and can expose a Retry-After header.
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitExceededException extends RuntimeException {

    private final Long retryAfterSeconds;

    public RateLimitExceededException(String message) {
        super(message);
        this.retryAfterSeconds = null;
    }

    public RateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
        this.retryAfterSeconds = null;
    }

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        this(message, retryAfterSeconds, null);
    }

    public RateLimitExceededException(String message, long retryAfterSeconds, Throwable cause) {
        super(message, cause);
        if (retryAfterSeconds < 0) {
            throw new IllegalArgumentException("retryAfterSeconds must not be negative");
        }
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
