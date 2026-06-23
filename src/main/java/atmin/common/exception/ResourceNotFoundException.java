package atmin.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested resource is not found.
 * Maps to HTTP 404 Not Found.
 *
 * <p>Usage:</p>
 * <pre>
 * // Simple message
 * throw new ResourceNotFoundException("User not found");
 *
 * // With resource details (auto-formats message)
 * throw new ResourceNotFoundException("User", "id", 123);
 * // → "User not found with id: '123'"
 * </pre>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private String resourceName;
    private String fieldName;
    private Object fieldValue;

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a detailed not-found exception with resource info.
     *
     * @param resourceName the name of the resource (e.g., "User", "Order")
     * @param fieldName    the field used for lookup (e.g., "id", "email")
     * @param fieldValue   the value that was not found
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }
}