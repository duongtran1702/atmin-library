package atmin.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an upstream or external service is unavailable.
 * Maps to HTTP 503 Service Unavailable.
 *
 * <p>Usage:</p>
 * <pre>
 * throw new ServiceUnavailableException("Payment service is currently unavailable");
 * // or with service name:
 * throw new ServiceUnavailableException("payment-service", true);
 * </pre>
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ServiceUnavailableException extends RuntimeException {

    private final String serviceName;

    public ServiceUnavailableException(String message) {
        super(message);
        this.serviceName = null;
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
        this.serviceName = null;
    }

    /**
     * Create exception with a service name for structured error messages.
     *
     * @param serviceName the name of the unavailable service
     * @param useServiceName flag to distinguish from message-only constructor
     */
    public ServiceUnavailableException(String serviceName, boolean useServiceName) {
        super(String.format("Service '%s' is currently unavailable. Please try again later.", serviceName));
        this.serviceName = serviceName;
    }

    public ServiceUnavailableException(String serviceName, boolean useServiceName, Throwable cause) {
        super(String.format("Service '%s' is currently unavailable. Please try again later.", serviceName), cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
