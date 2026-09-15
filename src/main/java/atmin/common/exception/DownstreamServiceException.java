package atmin.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Signals that a downstream service returned an unusable or failed response.
 * Maps to HTTP 502 Bad Gateway.
 */
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class DownstreamServiceException extends RuntimeException {

    private final String serviceName;
    private final Integer upstreamStatus;

    public DownstreamServiceException(String message) {
        super(message);
        this.serviceName = null;
        this.upstreamStatus = null;
    }

    public DownstreamServiceException(String message, Throwable cause) {
        super(message, cause);
        this.serviceName = null;
        this.upstreamStatus = null;
    }

    public DownstreamServiceException(String serviceName, int upstreamStatus) {
        this(serviceName, upstreamStatus, null);
    }

    public DownstreamServiceException(String serviceName, int upstreamStatus, Throwable cause) {
        super(String.format("Service '%s' returned an unsuccessful response (HTTP %d).",
                serviceName, upstreamStatus), cause);
        this.serviceName = serviceName;
        this.upstreamStatus = upstreamStatus;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Integer getUpstreamStatus() {
        return upstreamStatus;
    }
}
