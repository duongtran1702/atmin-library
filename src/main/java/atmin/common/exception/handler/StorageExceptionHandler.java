package atmin.common.exception.handler;

import atmin.common.exception.CloudStorageException;
import atmin.common.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import lombok.RequiredArgsConstructor;
import atmin.common.config.AtminExceptionProperties;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Exception handler for file upload and cloud storage related exceptions.
 *
 * <p>Handles:</p>
 * <ul>
 *   <li>400 - MaxUploadSizeExceededException (file too large)</li>
 *   <li>503 - CloudStorageException (cloud storage failure)</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class StorageExceptionHandler {

    private final AtminExceptionProperties properties;

    /**
     * Handles file upload size limit exceeded.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {

        log.error("Max upload size exceeded", ex);

        ApiErrorResponse errorResponse = ApiErrorResponse.badRequest(
                properties.getFileTooLarge(), request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles cloud storage failures (e.g., S3, GCS, Azure Blob).
     */
    @ExceptionHandler(CloudStorageException.class)
    public ResponseEntity<ApiErrorResponse> handleCloudStorageException(
            CloudStorageException ex, HttpServletRequest request) {

        log.error("Cloud storage error: {}", ex.getMessage(), ex);

        ApiErrorResponse errorResponse = ApiErrorResponse.serviceUnavailable(
                ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
