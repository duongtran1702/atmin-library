# Exception handlers

Tài liệu này mô tả các handler của `atmin-library:2.0.0`. Hướng dẫn sử dụng đầy đủ nằm trong [README](../README.md).

## Cơ chế đăng ký

`AtminExceptionAutoConfiguration` được khai báo tại:

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Trong servlet Spring MVC application, thư viện tự đăng ký bean phù hợp và nhường quyền khi ứng dụng đã khai báo handler cùng loại.

| Module | Handler | Điều kiện |
|---|---|---|
| Core | `CoreExceptionHandler` | Spring MVC servlet, mặc định bật |
| Bean Validation | `ValidationExceptionHandler` | Có Jakarta Validation |
| Security + JWT | `SecurityExceptionHandler` | Có cả Spring Security và JJWT |
| Security only | `SpringSecurityExceptionHandler` | Có Spring Security, không có JJWT |
| JWT only | `JwtExceptionHandler` | Có JJWT, không có Spring Security |
| Storage | `StorageExceptionHandler` | `storage-enabled=true` |
| Security filter | `SecurityExceptionConfig` | Có Spring Security và Jackson 3 |

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
