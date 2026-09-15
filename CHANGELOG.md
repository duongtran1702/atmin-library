# Changelog

Các thay đổi đáng chú ý của dự án được ghi tại đây.

## 2.1.0 - 2026-09-15

### Microservice error handling

- Bổ sung exception trung lập vendor cho rate limit, downstream failure,
  circuit breaker và gateway timeout.
- Tự động xử lý lỗi từ RestClient/RestTemplate và tùy chọn WebClient.
- Nhận diện trực tiếp lỗi DNS/network JDK như `UnknownHostException`,
  `ConnectException`, `NoRouteToHostException` và các timeout.
- Không phát tán hostname, response body hoặc chi tiết hạ tầng upstream ra client.
- Hỗ trợ WebFlux server bằng reactive controller advice và auto-configuration riêng.
- Thêm Reactive Security 401/403 JSON và JJWT handler tùy chọn.
- Truyền trace ID qua request header, exchange attribute và Reactor Context.
- Thêm low-level error handler tùy chọn cho Spring Cloud Gateway filter chain.
- Thêm mã lỗi ổn định, retry hint và logical service field dạng tùy chọn (`NON_NULL`).
- Giữ các downstream client error 400/404/409/422/429 theo cấu hình, bảo toàn `Retry-After` dạng số và chuẩn hóa status còn lại thành 502.
- Xử lý payload WebFlux vượt buffer thành 413 và lỗi TLS/SSL thành 503 an toàn.
- Thêm `README-MICROSERVICES.md` độc lập cho WebFlux, Gateway và reactive tracing.
- Thêm công tắc `atmin.exceptions.microservice-enabled` và ba public message
  có thể tùy chỉnh.
- Giữ nguyên API và JSON contract của 2.0.0.

## 2.0.0 - 2026-09-04

### Nâng cấp nền tảng

- Nâng dependency baseline lên Spring Boot 4.1.1, Spring Framework 7 và Jackson 3.
- Dùng starter `spring-boot-starter-webmvc` theo cấu trúc module Spring Boot 4.
- Nâng JJWT compile API lên 0.13.0.
- Giữ Java 21 và cập nhật cấu hình compile/Javadoc UTF-8.
- Thêm auto-configuration processor và configuration metadata processor.

### Giữ tương thích 1.x

- Giữ group ID, artifact ID và package `atmin.common`.
- Giữ các response field và JSON contract hiện có.
- Giữ factory methods cũ của `ApiResponse`, `ApiErrorResponse`, `PageInfo`.
- Giữ custom exception và constructor cũ.
- Giữ `@EnableAtminExceptionHandling` cho manual mode.

### Bổ sung

- Thêm `ApiResponse.accepted(...)`.
- Thêm `ApiResponse.pagePaginated(...)`.
- Thêm overload dùng `HttpStatusCode`.
- Thêm `PageInfo.slice(...)` cho pagination không phụ thuộc đối tượng Spring Data.
- Thêm validation response factory nhận custom summary message.
- Thêm handler cho malformed body, missing request value, method validation,
  Bean Validation, Spring MVC resource/method errors và checked exceptions.
- Hỗ trợ Spring Security không cần JJWT và JJWT không cần Spring Security.
- Thêm các công tắc bật/tắt auto-configuration theo module.
- Thêm cấu hình MDC key bằng `atmin.exceptions.trace-id-mdc-key`.
- Thêm Apache License 2.0 và bộ test hồi quy.

### Sửa lỗi

- Custom `CoreExceptionHandler` giờ khiến auto-configuration back off đúng cách.
- `ResponseStatusException` và Spring `ErrorResponse` giữ nguyên HTTP status.
- Lỗi authentication/JWT không còn trả exception message nội bộ ra client.
- Security filter dùng Jackson 3 `ObjectMapper` của Spring Boot 4.
- Validation giữ lỗi đầu tiên ổn định thay vì ghi đè không xác định.
- Pagination thủ công kiểm tra đầu vào và tránh phép tính floating-point.
- Credential upload không còn đọc nhầm environment variable `username` của hệ điều hành.

## 1.0.3.Beta

- Phiên bản public trước 2.0.
