package atmin.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for customizing exception messages.
 * Customize these in application.properties or application.yml with the prefix 'atmin.exceptions'.
 */
@ConfigurationProperties(prefix = "atmin.exceptions")
@Getter
@Setter
public class AtminExceptionProperties {

    /**
     * Message when validation fails (MethodArgumentNotValidException).
     * Default is "Validation failed".
     */
    private String validationFailed = "Validation failed";

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
}
