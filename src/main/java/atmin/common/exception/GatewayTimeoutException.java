package atmin.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Signals that a downstream service did not respond before its deadline.
 * Maps to HTTP 504 Gateway Timeout.
 */
@ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
public class GatewayTimeoutException extends RuntimeException {

    private final String serviceName;

    public GatewayTimeoutException(String message) {
        super(message);
        this.serviceName = null;
    }

    public GatewayTimeoutException(String message, Throwable cause) {
        super(message, cause);
        this.serviceName = null;
    }

    private GatewayTimeoutException(String serviceName, Throwable cause, boolean serviceReference) {
        super(String.format("Service '%s' did not respond in time.", serviceName), cause);
        this.serviceName = serviceName;
    }

    public static GatewayTimeoutException forService(String serviceName) {
        return new GatewayTimeoutException(serviceName, null, true);
    }

    public static GatewayTimeoutException forService(String serviceName, Throwable cause) {
        return new GatewayTimeoutException(serviceName, cause, true);
    }

    public String getServiceName() {
        return serviceName;
    }
}
