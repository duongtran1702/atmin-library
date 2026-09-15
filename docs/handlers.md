# Exception handlers

Tài liệu này mô tả các handler của `atmin-library:2.1.0`. Chọn [hướng dẫn MVC](mvc/USAGE.md) hoặc [hướng dẫn reactive](reactive/USAGE.md) theo web stack.

## Cơ chế đăng ký

Hai auto-configuration MVC và WebFlux được khai báo tại:

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Spring Boot chọn đúng adapter theo application type. Các controller/security handler nhường quyền khi ứng dụng đã khai báo bean cùng loại. Riêng Gateway handler dùng cơ chế opt-out rõ ràng: đặt `atmin.exceptions.gateway-enabled=false` trước khi đăng ký custom `ErrorWebExceptionHandler`.

| Module | Handler | Điều kiện |
|---|---|---|
| Core | `CoreExceptionHandler` | Spring MVC servlet, mặc định bật |
| Microservice HTTP | `MicroserviceExceptionHandler` | Spring Web, `microservice-enabled=true` |
| WebClient | `WebClientExceptionHandler` | Có Spring WebFlux, `microservice-enabled=true` |
| Bean Validation | `ValidationExceptionHandler` | Có Jakarta Validation |
| Security + JWT | `SecurityExceptionHandler` | Có cả Spring Security và JJWT |
| Security only | `SpringSecurityExceptionHandler` | Có Spring Security, không có JJWT |
| JWT only | `JwtExceptionHandler` | Có JJWT, không có Spring Security |
| Storage | `StorageExceptionHandler` | `storage-enabled=true` |
| Security filter | `SecurityExceptionConfig` | Có Spring Security và Jackson 3 |
| Reactive core | `ReactiveCoreExceptionHandler` | Web application type là reactive |
| Reactive validation | `ReactiveValidationExceptionHandler` | Reactive và có Jakarta Validation |
| Reactive JWT | `ReactiveJwtExceptionHandler` | Reactive và có JJWT |
| Reactive security | `ReactiveSecurityExceptionConfig` | Reactive Security và Jackson 3 |
| Reactive trace | `AtminReactiveTraceFilter` | Reactive application |
| Gateway filter chain | `AtminGatewayErrorWebExceptionHandler` | Có Spring Cloud Gateway |

## Core

Core giữ JSON contract `ApiErrorResponse` của 1.x và mở rộng các lỗi Spring MVC hiện đại:

- validation request body và method parameters;
- JSON/body không đọc được;
- thiếu request parameter hoặc multipart part;
- type mismatch;
- custom exception 400/401/403/404/409/503;
- method/media errors 405/406/415;
- route hoặc static resource không tồn tại;
- `ResponseStatusException` và `ErrorResponseException`;
- catch-all 500 với message an toàn.

`RuntimeException` không xác định luôn ghi stack trace cùng `traceId` ở server nhưng client chỉ nhận `atmin.exceptions.unexpected-error`.

## Microservice HTTP boundaries

`MicroserviceExceptionHandler` xử lý các lỗi đồng bộ từ `RestClient`/`RestTemplate` và các exception trung lập với client:

| Exception | Status |
|---|---:|
| `RateLimitExceededException` | 429 |
| `DownstreamServiceException` | 502 |
| `RestClientResponseException` | 502 với message an toàn |
| `ResourceAccessException` do transport | 503 |
| `UnknownHostException`, `ConnectException`, `NoRouteToHostException` trực tiếp | 503 |
| `CircuitBreakerOpenException` | 503 |
| `ResourceAccessException` do timeout | 504 |
| `SocketTimeoutException`, `HttpTimeoutException` trực tiếp | 504 |
| `GatewayTimeoutException` | 504 |

Nếu có `spring-webflux`, `WebClientExceptionHandler` được thêm tự động:

- `WebClientResponseException` → 502;
- `WebClientRequestException` do transport → 503;
- `WebClientRequestException` do timeout → 504.

Body response từ downstream không được trả thẳng cho caller để tránh lộ thông tin của service nội bộ. Có thể tùy chỉnh ba public message:

```properties
atmin.exceptions.downstream-service-error=Dịch vụ phụ thuộc trả về lỗi
atmin.exceptions.downstream-service-unavailable=Dịch vụ phụ thuộc tạm thời không khả dụng
atmin.exceptions.downstream-timeout=Dịch vụ phụ thuộc phản hồi quá chậm
```

`RateLimitExceededException(message, seconds)` tự thêm header `Retry-After`.

Feign, Resilience4j và các client khác không bị ép thành dependency. Bắt exception của chúng tại boundary/fallback rồi bọc bằng custom exception tương ứng của Atmin.

## WebFlux và Gateway

`AtminReactiveExceptionAutoConfiguration` chỉ chạy khi Spring Boot xác định application type là `REACTIVE`. Nó không được đăng ký đồng thời với MVC auto-configuration.

Reactive controller advice xử lý validation, input/decode, custom exception, WebClient, downstream transport và catch-all 500 mà không gọi `block()` hoặc `subscribe()`.

Reactive Security cung cấp `ServerAuthenticationEntryPoint` và `ServerAccessDeniedHandler`. Gateway integration dùng `ErrorWebExceptionHandler` để nhận lỗi phát sinh trước controller, kiểm tra response committed trước khi ghi JSON.

Gateway application muốn dùng `ErrorWebExceptionHandler` riêng phải đặt `atmin.exceptions.gateway-enabled=false`. Atmin không dò mọi bean cùng interface vì một application có thể có nhiều error handler với order khác nhau; property opt-out giúp tránh cạnh tranh thứ tự một cách tường minh.

Trace ID reactive được lưu trong exchange attribute và Reactor Context, đồng thời có thể echo qua response header. Xem [hướng dẫn reactive đầy đủ](reactive/USAGE.md).

## Validation

`MethodArgumentNotValidException`, `HandlerMethodValidationException` và `ConstraintViolationException` được chuyển thành:

```json
{
  "timestamp": "2026-09-04T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/users",
  "errors": {
    "email": "must be a well-formed email address"
  },
  "traceId": "..."
}
```

Nếu một field có nhiều lỗi, contract 1.x được giữ bằng cách lấy lỗi đầu tiên theo thứ tự binding.

## Security controller handlers

Spring Security không còn phụ thuộc bắt buộc vào JJWT. Thư viện tự chọn một trong ba handler theo classpath.

```groovy
implementation 'org.springframework.boot:spring-boot-starter-security'
```

Nếu dùng JJWT:

```groovy
implementation 'io.jsonwebtoken:jjwt-api:0.13.0'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.13.0'
```

Lỗi authentication/JWT trả message cấu hình thay vì exception message nội bộ:

```yaml
atmin:
  exceptions:
    security-unauthorized: Vui lòng đăng nhập
    access-denied: Bạn không có quyền thực hiện thao tác này
```

## Security filter handlers

Đưa hai bean vào filter chain:

```java
@Bean
SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        AuthenticationEntryPoint authenticationEntryPoint,
        AccessDeniedHandler accessDeniedHandler) throws Exception {
    return http
            .exceptionHandling(configurer -> configurer
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
            .build();
}
```

Ứng dụng tự khai báo bean `AuthenticationEntryPoint` hoặc `AccessDeniedHandler` sẽ được ưu tiên.

## Storage

- `MaxUploadSizeExceededException` → 400.
- `CloudStorageException` → 503.

Message upload:

```properties
atmin.exceptions.file-too-large=Tệp vượt quá dung lượng cho phép
```

## Bật/tắt module

```yaml
atmin:
  exceptions:
    enabled: true
    security-enabled: true
    security-filter-enabled: true
    storage-enabled: true
    microservice-enabled: true
    gateway-enabled: true
    gateway-order: -2
    trace-id-header: X-Trace-Id
    echo-trace-id-header: true
```

## Manual mode

```java
@SpringBootApplication(exclude = AtminExceptionAutoConfiguration.class)
@EnableAtminExceptionHandling(
        enableSecurityHandlers = true,
        enableStorageHandlers = true
)
class Application {
}
```

Không dùng annotation đồng thời với auto-configuration trừ khi có chủ đích. Auto-configuration là cách khuyên dùng.
