package atmin.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pagination information for paginated API responses.
 * Supports both count-based pagination ({@link org.springframework.data.domain.Page})
 * and no-count pagination ({@link org.springframework.data.domain.Slice}).
 *
 * <p><strong>With Spring Data Page (full metadata):</strong></p>
 * <pre>
 * Page&lt;User&gt; page = userRepository.findAll(pageable);
 * PageInfo pageInfo = PageInfo.from(page);
 * return ApiResponse.paginated("Users found", page.getContent(), pageInfo);
 * </pre>
 * <p>JSON output:</p>
 * <pre>
 * "page": {
 *   "pageNumber": 0, "pageSize": 10,
 *   "totalElements": 25, "totalPages": 3,
 *   "hasNext": true, "first": true, "last": false
 * }
 * </pre>
 *
 * <p><strong>With Spring Data Slice (minimal — optimized for large datasets):</strong></p>
 * <pre>
 * Slice&lt;User&gt; slice = userRepository.findAllBy(pageable);
 * return ApiResponse.slicePaginated("Users found", slice);
 * </pre>
 * <p>JSON output (ultra-minimal, no COUNT query):</p>
 * <pre>
 * "page": {
 *   "pageNumber": 0, "pageSize": 10,
 *   "hasNext": true
 * }
 * </pre>
 *
 * <p>When using Slice, the fields {@code totalElements}, {@code totalPages},
 * {@code first}, and {@code last} are all omitted from JSON, producing the
 * most compact pagination response possible.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageInfo {

    private int pageNumber;
    private int pageSize;

    /**
     * Total number of elements across all pages.
     * Null when using Slice-based pagination (omitted from JSON).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long totalElements;

    /**
     * Total number of pages.
     * Null when using Slice-based pagination (omitted from JSON).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer totalPages;

    /**
     * Whether there is a next page available.
     * Always present for both Page and Slice pagination.
     * This is the primary field for implementing infinite scroll / load more UIs.
     */
    private boolean hasNext;

    /**
     * Whether this is the first page.
     * Only present for Page-based pagination (omitted from JSON for Slice).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean first;

    /**
     * Whether this is the last page.
     * Only present for Page-based pagination (omitted from JSON for Slice).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean last;

    /**
     * Create PageInfo from a Spring Data {@link org.springframework.data.domain.Page}.
     * <p>
     * Includes full pagination metadata: totalElements, totalPages, first, last, hasNext.
     * Note: This method requires Spring Data on the classpath.
     * </p>
     *
     * @param page the Spring Data Page object
     * @return PageInfo instance with full pagination details
     */
    public static PageInfo from(org.springframework.data.domain.Page<?> page) {
        return PageInfo.builder()
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    /**
     * Create PageInfo from a Spring Data {@link org.springframework.data.domain.Slice}.
     * <p>
     * Produces an <strong>ultra-minimal</strong> pagination response containing only
     * {@code pageNumber}, {@code pageSize}, and {@code hasNext}. All other fields
     * (totalElements, totalPages, first, last) are null and hidden from JSON.
     * </p>
     *
     * <p>This avoids expensive COUNT(*) queries on large tables, making it ideal for:</p>
     * <ul>
     *   <li>Infinite scroll UIs (Facebook, TikTok style)</li>
     *   <li>Tables with millions of rows</li>
     *   <li>"Load more" buttons instead of traditional page numbers</li>
     * </ul>
     *
     * <p>Example:</p>
     * <pre>
     * Slice&lt;Product&gt; slice = productRepository.findAllByCategory("electronics", pageable);
     * return ResponseEntity.ok(ApiResponse.slicePaginated("Products found", slice));
     * </pre>
     *
     * @param slice the Spring Data Slice object
     * @return PageInfo instance with only pageNumber, pageSize, and hasNext
     */
    public static PageInfo from(org.springframework.data.domain.Slice<?> slice) {
        return PageInfo.builder()
                .pageNumber(slice.getNumber())
                .pageSize(slice.getSize())
                .hasNext(slice.hasNext())
                .build();
    }

    /**
     * Create PageInfo manually without Spring Data dependency.
     * Includes full metadata (totalElements, totalPages, first, last).
     *
     * @param pageNumber    current page number (0-indexed)
     * @param pageSize      number of items per page
     * @param totalElements total number of items
     * @return PageInfo instance with full pagination details
     */
    public static PageInfo of(int pageNumber, int pageSize, long totalElements) {
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;
        boolean isLast = pageNumber >= totalPages - 1;
        return PageInfo.builder()
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(!isLast)
                .first(pageNumber == 0)
                .last(isLast)
                .build();
    }
}


