# atmin-library 2.1.0 cho Spring MVC

Tài liệu này tập trung trả lời: **thêm atmin-library vào ứng dụng MVC thì làm được gì?**

Để triển khai ngay, dùng [USAGE.md](USAGE.md). File hướng dẫn đó độc lập và chứa đầy đủ dependency, code mẫu, cấu hình và troubleshooting.

## Khả năng chính

- Chuẩn hóa success response bằng `ApiResponse<T>`.
- Chuẩn hóa error response bằng `ApiErrorResponse`.
- Hỗ trợ metadata phân trang từ Spring Data `Page`, `Slice` hoặc dữ liệu thủ công.
- Tự xử lý validation body, method, query, path và request part.
- Mapping custom exception sang HTTP 400, 401, 403, 404, 409, 413, 429, 500, 502, 503 và 504.
- Giữ status phù hợp từ `ResponseStatusException` và lỗi Spring MVC.
- Trả JSON 401/403 từ Servlet Spring Security thay vì HTML mặc định.
- Spring Security và JJWT đều là tùy chọn, có thể dùng độc lập.
- Chuẩn hóa lỗi downstream từ `RestClient`, `RestTemplate` và WebClient client-side.
- Phân loại DNS, connection, TLS và unavailable thành 503; timeout thành 504.
- Không chuyển tiếp upstream body, hostname nội bộ, token hoặc stack trace ra client.
- Nhận, kiểm tra và echo trace ID; giá trị không an toàn được thay bằng UUID.
- Cho phép tùy chỉnh public message và bật/tắt từng nhóm handler.
- Application có thể thay handler bằng bean riêng mà không fork thư viện.

## Contract public

Success response giữ các field chính:

```json
{
  "success": true,
  "status": 200,
  "message": "User found",
  "data": {}
}
```

Error response có trace ID và có thể có code ổn định:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "User not found",
  "path": "/api/users/1",
  "traceId": "request-trace-42",
  "code": "RESOURCE_NOT_FOUND",
  "retryable": false
}
```

## Điều thư viện không làm

- Không chứa entity, DTO hay business rule của application.
- Không tự cấu hình database, broker, retry hoặc circuit breaker.
- Không bắt buộc consumer dùng Security, JWT, Spring Data hay WebClient.
- Không quyết định session policy; Spring Security session vẫn thuộc application.

## Phù hợp với

- REST monolith dùng Spring MVC.
- MVC microservice dùng RestClient/RestTemplate.
- MVC application chỉ dùng WebClient như HTTP client, không chạy reactive server.
- Batch/worker chỉ sử dụng response hoặc exception contract khi cần.

## Bước tiếp theo

Mở [hướng dẫn MVC đầy đủ](USAGE.md) để cài dependency và triển khai.
