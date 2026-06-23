package atmin.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

/**
 * Generic API response wrapper for successful and error responses.
 *
 * <p>Usage in Controller:</p>
 * <pre>
 * // Success with data
 * return ResponseEntity.ok(ApiResponse.success("User found", user));
 *
 * // Success without data
 * return ResponseEntity.ok(ApiResponse.success("Deleted successfully"));
 *
 * // Created
 * return ResponseEntity.status(201).body(ApiResponse.created("User created", user));
 *
 * // Paginated (with Page — includes total count)
 * Page&lt;User&gt; page = userService.findAll(pageable);
 * return ResponseEntity.ok(ApiResponse.paginated("Users retrieved", page.getContent(), PageInfo.from(page)));
 *
 * // Paginated (with Slice — no count, optimized for large datasets)
 * Slice&lt;User&gt; slice = userService.findAllSlice(pageable);
 * return ResponseEntity.ok(ApiResponse.slicePaginated("Users retrieved", slice));
 *
 * // Error
 * return ResponseEntity.status(400).body(ApiResponse.error(400, "Invalid input"));
 * </pre>
 *
 * @param <T> the type of data payload
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private int status;
    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PageInfo page;

    // ======================== Success Factory Methods ========================

    /**
     * Create a success response with data (HTTP 200).
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Create a success response without data (HTTP 200).
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .message(message)
                .build();
    }

    /**
     * Create a success response with custom HTTP status.
     */
    public static <T> ApiResponse<T> success(HttpStatus httpStatus, String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(httpStatus.value())
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Create a 201 Created response with data.
     */
    public static <T> ApiResponse<T> created(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(HttpStatus.CREATED.value())
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Create a 204 No Content response.
     */
    public static <T> ApiResponse<T> noContent(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(HttpStatus.NO_CONTENT.value())
                .message(message)
                .build();
    }

    // ======================== Paginated Factory Methods ========================

    /**
     * Create a paginated success response (HTTP 200).
     *
     * @param message the success message
     * @param data    the page content
     * @param page    pagination info (use {@link PageInfo#from(org.springframework.data.domain.Page)})
     */
    public static <T> ApiResponse<T> paginated(String message, T data, PageInfo page) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .message(message)
                .data(data)
                .page(page)
                .build();
    }

    /**
     * Create a paginated success response from a Spring Data Slice (HTTP 200).
     *
     * <p>This is the recommended method for <strong>large datasets</strong> or
     * <strong>infinite scroll</strong> UIs where counting total elements is expensive.
     * Unlike {@link org.springframework.data.domain.Page}, a
     * {@link org.springframework.data.domain.Slice} does NOT execute a COUNT(*) query,
     * significantly improving query performance on large tables.</p>
     *
     * <p>The resulting JSON will NOT include {@code totalElements} or {@code totalPages}
     * in the {@code page} object. Instead, use the {@code hasNext} field to determine
     * if more data is available.</p>
     *
     * <p>Example:</p>
     * <pre>
     * Slice&lt;Product&gt; slice = productRepository.findAllByCategory("electronics", pageable);
     * return ResponseEntity.ok(ApiResponse.slicePaginated("Products found", slice));
     * </pre>
     *
     * @param message the success message
     * @param slice   the Spring Data Slice object
     * @param <T>     the type of elements in the slice
     * @return ApiResponse with slice content and no-count pagination info
     */
    public static <T> ApiResponse<java.util.List<T>> slicePaginated(
            String message, org.springframework.data.domain.Slice<T> slice) {
        return ApiResponse.<java.util.List<T>>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .message(message)
                .data(slice.getContent())
                .page(PageInfo.from(slice))
                .build();
    }

    // ======================== Error Factory Methods ========================

    /**
     * Create an error response with HTTP status code.
     */
    public static <T> ApiResponse<T> error(int status, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(status)
                .message(message)
                .build();
    }

    /**
     * Create an error response from HttpStatus enum.
     */
    public static <T> ApiResponse<T> error(HttpStatus httpStatus, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(httpStatus.value())
                .message(message)
                .build();
    }

    /**
     * Create a 400 Bad Request error response.
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return error(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Create a 404 Not Found error response.
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return error(HttpStatus.NOT_FOUND, message);
    }

    /**
     * Create a 500 Internal Server Error response.
     */
    public static <T> ApiResponse<T> internalError(String message) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
