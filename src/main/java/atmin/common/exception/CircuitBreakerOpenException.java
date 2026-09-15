package atmin.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Signals that a circuit breaker rejected a downstream call while open.
 * Maps to HTTP 503 Service Unavailable without depending on a circuit-breaker vendor.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class CircuitBreakerOpenException extends RuntimeException {

    private final String serviceName;

    public CircuitBreakerOpenException(String message) {
        super(message);
        this.serviceName = null;
    }

    public CircuitBreakerOpenException(String message, Throwable cause) {
        super(message, cause);
        this.serviceName = null;
    }

    private CircuitBreakerOpenException(String serviceName, Throwable cause, boolean serviceReference) {
        super(String.format("Circuit breaker for service '%s' is open. Please try again later.", serviceName), cause);
        this.serviceName = serviceName;
    }

    public static CircuitBreakerOpenException forService(String serviceName) {
        return new CircuitBreakerOpenException(serviceName, null, true);
    }

    public static CircuitBreakerOpenException forService(String serviceName, Throwable cause) {
        return new CircuitBreakerOpenException(serviceName, cause, true);
    }

    public String getServiceName() {
        return serviceName;
    }
}
