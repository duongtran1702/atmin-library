# atmin-library 2.1.0 cho WebFlux và Gateway

Tài liệu này tập trung trả lời: **thêm atmin-library vào ứng dụng reactive thì làm được gì?**

Để triển khai ngay, dùng [USAGE.md](USAGE.md). File hướng dẫn đó độc lập và chứa đầy đủ dependency, code mẫu, cấu hình và troubleshooting.

## Khả năng WebFlux

- Dùng chung `ApiResponse`, `ApiErrorResponse` và `PageInfo` với MVC.
- Controller có thể trả `Mono<ApiResponse<T>>`.
- Controller advice xử lý validation, malformed input và custom exception trong reactive chain.
- Reactive Security trả JSON 401/403 bằng `ServerAuthenticationEntryPoint` và `ServerAccessDeniedHandler`.
- WebClient error mapping giữ các downstream 400/404/409/422/429 được cho phép.
- Downstream 5xx được chuẩn hóa thành 502.
- DNS, connection, no-route và TLS failure thành 503; timeout thành 504.
- `Retry-After` chỉ được giữ khi là chuỗi số an toàn.
- Trace ID được lưu trong exchange attribute và Reactor Context.
- Trace ID trong response body và header dùng cùng một giá trị.
- Production handler không gọi `block()` hoặc `subscribe()`.

## Khả năng Spring Cloud Gateway

- Optional `ErrorWebExceptionHandler` cho lỗi phát sinh trước controller.
- Mapping rate limit, downstream response, unavailable, circuit breaker và timeout.
- Không ghi response lần hai nếu response đã committed.
- Exception không nhận diện được được chuyển cho handler tiếp theo.
- Cho phép thay bằng handler riêng qua `atmin.exceptions.gateway-enabled=false`.
- Không tự tạo route, rate limiter, retry hoặc circuit-breaker policy.

## An toàn và tính độc lập

- Không chuyển tiếp upstream response body, hostname, credential hoặc stack trace.
- Logical service name bị ẩn mặc định.
- WebFlux/Gateway integration chỉ kích hoạt trong reactive application.
- MVC có WebClient trên classpath vẫn chỉ chạy MVC server adapter.
- Security, JJWT, validation, Gateway và Spring Data đều là optional integration.

## Contract error

```json
{
  "status": 503,
  "error": "Service Unavailable",
  "message": "A downstream service is temporarily unavailable.",
  "path": "/api/orders",
  "traceId": "request-trace-42",
  "code": "DOWNSTREAM_UNAVAILABLE",
  "retryable": true
}
```

## Phù hợp với

- Reactive REST microservice.
- API Gateway dùng Spring Cloud Gateway.
- Reactive Security application.
- Service gọi downstream bằng WebClient.
- Reactive application dùng R2DBC hoặc data layer khác do consumer lựa chọn.

## Bước tiếp theo

Mở [hướng dẫn reactive đầy đủ](USAGE.md) để cài dependency và triển khai.
