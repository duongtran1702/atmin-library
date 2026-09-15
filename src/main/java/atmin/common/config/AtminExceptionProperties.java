package atmin.common.config;

import atmin.common.response.ApiErrorResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.beans.factory.InitializingBean;

/**
 * Configuration properties for customizing exception messages.
 * Customize these in application.properties or application.yml with the prefix 'atmin.exceptions'.
 */
@ConfigurationProperties(prefix = "atmin.exceptions")
@Getter
@Setter
public class AtminExceptionProperties implements InitializingBean {

    /**
     * Enables all Atmin exception-handler auto-configuration.
     */
    private boolean enabled = true;

    /**
     * Enables the optional Spring Security/JWT controller handlers.
     */
    private boolean securityEnabled = true;

    /**
     * Enables the optional Spring Security filter response handlers.
     */
    private boolean securityFilterEnabled = true;

    /**
     * Enables file-upload and cloud-storage handlers.
     */
    private boolean storageEnabled = true;

    /**
     * Enables handlers for downstream HTTP calls, timeouts, rate limits, and
     * circuit-breaker rejections.
     */
    private boolean microserviceEnabled = true;

    /** Preserves selected safe downstream 4xx statuses instead of mapping all to 502. */
    private boolean downstreamPreserveClientErrors = true;

    /** Allows logical service labels in enriched responses; disabled by default. */
    private boolean downstreamExposeServiceName = false;

    /**
     * MDC key used to resolve a trace ID.
     */
    private String traceIdMdcKey = "traceId";

    /** Header used to receive and return a trace ID across service boundaries. */
    private String traceIdHeader = "X-Trace-Id";

    /** Adds the resolved trace ID to the HTTP response header. */
    private boolean echoTraceIdHeader = true;

    /**
     * Enables low-level error handling when Spring Cloud Gateway is present.
     * Disable this before registering an application-owned ErrorWebExceptionHandler.
     */
    private boolean gatewayEnabled = true;

    /** Order of the optional Gateway WebExceptionHandler. */
    private int gatewayOrder = -2;

    /**
     * Message when validation fails (MethodArgumentNotValidException).
     * Default is "Validation failed".
     */
    private String validationFailed = "Validation failed";

    /**
     * Message when a request body contains malformed or unreadable JSON.
     */
    private String malformedRequest = "Request body is malformed or unreadable.";

    /**
     * Message when a required request parameter or request part is missing.
     */
    private String missingRequestValue = "A required request value is missing.";

    /**
     * Safe message when a downstream service returns a 4xx or 5xx response.
     */
    private String downstreamServiceError = "A downstream service returned an unsuccessful response.";

    /**
     * Safe message when a downstream service cannot be reached.
     */
    private String downstreamServiceUnavailable = "A downstream service is temporarily unavailable.";

    /**
     * Safe message when a downstream HTTP call exceeds its deadline.
     */
    private String downstreamTimeout = "A downstream service did not respond in time.";

    /**
     * Message when an unexpected internal server error occurs (RuntimeException).
     * Default is "An unexpected error occurred on the server.".
     */
    private String unexpectedError = "An unexpected error occurred on the server.";

    /**
     * Message for forbidden access / access denied caught in controllers.
     * Default is "You do not have permission to access this resource.".
     */
    private String accessDenied = "You do not have permission to access this resource.";

    /**
     * Message when file upload size limit is exceeded (MaxUploadSizeExceededException).
     * Default is "File size exceeds the maximum allowed limit.".
     */
    private String fileTooLarge = "File size exceeds the maximum allowed limit.";

    /**
     * Message for security authentication entry point (AuthenticationEntryPoint).
     * Default is "Full authentication is required to access this resource".
     */
    private String securityUnauthorized = "Full authentication is required to access this resource";

    /**
     * Message for security access denied handler (AccessDeniedHandler).
     * Default is "Access Denied".
     */
    private String securityAccessDenied = "Access Denied";

    @Override
    public void afterPropertiesSet() {
        ApiErrorResponse.setMdcKey(traceIdMdcKey);
    }
}
