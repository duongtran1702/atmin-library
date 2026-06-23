# 🛡️ Exception Handlers Documentation

Thư viện tự động bắt và xử lý exception, trả về JSON response chuẩn.
Handlers được chia thành 3 module độc lập để tránh xung đột dependency. Toàn bộ các phản hồi lỗi trả về đều tự động kèm theo trường mã định danh truy vết `traceId`.

## Tổng quan các Module

```
┌─────────────────────────────────────────────────────┐
│                  Exception Handlers                  │
├──────────────────┬───────────────┬───────────────────┤
│  CoreException   │  Security     │  Storage          │
│  Handler         │  Exception    │  Exception        │
│  (luôn bật)      │  Handler      │  Handler          │
│                  │  (cần Spring  │  (luôn bật)       │
│                  │   Security    │                   │
│                  │   + JWT)      │                   │
└──────────────────┴───────────────┴───────────────────┘
```

| Module | Class | Khi nào bật? |
|---|---|---|
| **Core** | `CoreExceptionHandler` | Luôn bật khi thêm dependency |
| **Security** | `SecurityExceptionHandler` | Chỉ khi có Spring Security + JWT trên classpath |
| **Storage** | `StorageExceptionHandler` | Luôn bật khi thêm dependency |

## Import

```java
// Không cần import handler!
// Chúng tự động được đăng ký qua auto-configuration.
// Bạn chỉ cần import exception để throw:
import atmin.common.exception.*;
```

---

## 1. CoreExceptionHandler

> Xử lý các lỗi phổ biến nhất. **Luôn tự động bật.**

| Exception | HTTP | Log Level |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | — |
| `MethodArgumentTypeMismatchException` | 400 | — |
| `IllegalArgumentException` | 400 | WARN |
| `BadRequestException` | 400 | — |
| `UnauthorizedException` | 401 | WARN |
| `ForbiddenException` | 403 | WARN |
| `ResourceNotFoundException` | 404 | — |
| `DuplicateResourceException` | 409 | — |
| `ConflictException` | 409 | — |
| `HttpMediaTypeNotSupportedException` | 415 | — |
| `ServiceUnavailableException` | 503 | ERROR |
| `RuntimeException` (catch-all) | 500 | ERROR |

### Validation Errors (400) — `@Valid`

Khi dùng `@Valid` trên `@RequestBody`, nếu validation thất bại, handler sẽ trả về **chi tiết từng field lỗi** và đính kèm `traceId`:

```java
// DTO
public class CreateUserRequest {
    @NotBlank(message = "Tên không được để trống")
    private String name;

    @Email(message = "Email không đúng format")
    private String email;

    @Min(value = 18, message = "Tuổi phải >= 18")
    private int age;
}

// Controller
@PostMapping
public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
    // Nếu validation fail → tự động trả lỗi, không vào method này
}
```

**JSON Response khi validation fail:**
```json
{
    "timestamp": "2026-06-19T14:30:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Validation failed",
    "path": "/api/users",
    "errors": {
        "name": "Tên không được để trống",
        "email": "Email không đúng format",
        "age": "Tuổi phải >= 18"
    },
    "traceId": "c8b9d0e1-f2a3-4b5c-6d7e-8f9a0b1c2d3e"
}
```

### Type Mismatch (400)

Khi truyền sai kiểu dữ liệu qua URL (ví dụ: String thay vì int):

```
GET /api/users?page=abc
```

```json
{
    "timestamp": "2026-06-19T14:30:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Failed to convert value of type 'java.lang.String' to required type 'int' for parameter 'page'",
    "path": "/api/users",
    "traceId": "fa7a4e6b-a25f-4ce9-8973-2e06cbe19e7a"
}
```

### Catch-all (500)

Mọi `RuntimeException` không được handler cụ thể nào bắt sẽ trả về 500. 
**Message generic** được trả về cho Client để bảo mật (không lộ cấu trúc mã nguồn bên dưới), đồng thời **log chi tiết stack trace** kèm theo `traceId` tại server:

**Server log:**
```text
[ERROR] 2026-06-22 13:45:12.123 [http-nio-8080-exec-1] c.a.c.e.h.CoreExceptionHandler - TraceId=fa7a4e6b-a25f-4ce9-8973-2e06cbe19e7a | An unexpected error occurred at URI: /api/users
java.lang.NullPointerException: Cannot invoke "String.toLowerCase()" because "name" is null
    ...
```

**JSON Response:**
```json
{
    "timestamp": "2026-06-19T14:30:00",
    "status": 500,
    "error": "Internal Server Error",
    "message": "An unexpected error occurred on the server.",
    "path": "/api/users",
    "traceId": "fa7a4e6b-a25f-4ce9-8973-2e06cbe19e7a"
}
```

---

## 2. SecurityExceptionHandler

> Xử lý lỗi liên quan đến Spring Security và JWT.
> **Chỉ bật khi cả 2 dependency có trên classpath.**

| Exception | HTTP | Mô tả |
|---|---|---|
| `JwtException` | 401 | Token JWT hết hạn, sai chữ ký, format sai |
| `AuthenticationException` | 401 | Đăng nhập thất bại (BadCredentials...) |
| `AccessDeniedException` | 403 | Không đủ quyền (role-based) |

### Dependencies cần thiết

```groovy
// build.gradle của project sử dụng
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'
```

### Ví dụ Response

**JWT hết hạn:**
```json
{
    "timestamp": "2026-06-19T14:30:00",
    "status": 401,
    "error": "Unauthorized",
    "message": "JWT expired at 2026-06-19T13:00:00Z",
    "path": "/api/profile",
    "traceId": "fa7a4e6b-a25f-4ce9-8973-2e06cbe19e7a"
}
```

**Sai mật khẩu:**
```json
{
    "timestamp": "2026-06-19T14:30:00",
    "status": 401,
    "error": "Unauthorized",
    "message": "Bad credentials",
    "path": "/api/auth/login",
    "traceId": "fa7a4e6b-a25f-4ce9-8973-2e06cbe19e7a"
}
```

**Không đủ quyền:**
```json
{
    "timestamp": "2026-06-19T14:30:00",
    "status": 403,
    "error": "Forbidden",
    "message": "You do not have permission to access this resource.",
    "path": "/api/admin/settings",
    "traceId": "fa7a4e6b-a25f-4ce9-8973-2e06cbe19e7a"
}
```

---

## 3. StorageExceptionHandler

> Xử lý lỗi upload file và cloud storage.

| Exception | HTTP | Mô tả |
|---|---|---|
| `MaxUploadSizeExceededException` | 400 | File upload vượt quá giới hạn |
| `CloudStorageException` | 503 | Lỗi cloud storage (S3, GCS, Azure) |

### Ví dụ Response

**File quá lớn:**
```json
{
    "timestamp": "2026-06-19T14:30:00",
    "status": 400,
    "error": "Bad Request",
    "message": "File size exceeds the maximum allowed limit.",
    "path": "/api/files/upload",
    "traceId": "fa7a4e6b-a25f-4ce9-8973-2e06cbe19e7a"
}
```

**Cloud Storage lỗi:**
```json
{
    "timestamp": "2026-06-19T14:30:00",
    "status": 503,
    "error": "Service Unavailable",
    "message": "Failed to upload file to S3",
    "path": "/api/files/upload",
    "traceId": "fa7a4e6b-a25f-4ce9-8973-2e06cbe19e7a"
}
```

---

## 4. SecurityExceptionConfig (Xử lý lỗi tầng Security Filter)

> Xử lý lỗi xác thực và phân quyền xảy ra tại tầng **Security Filter Chain** (trước khi request đi vào Controller).
> **Chỉ bật khi có thư viện Spring Security Web trên classpath.**

Thư viện tự động định nghĩa 2 bean:
- `AuthenticationEntryPoint`: Trả về JSON lỗi 401 khi người dùng chưa xác thực truy cập tài nguyên yêu cầu đăng nhập.
- `AccessDeniedHandler`: Trả về JSON lỗi 403 khi người dùng đã xác thực nhưng không có đủ quyền (Role/Authority).

Cả hai đều trả về `ApiErrorResponse` chuẩn và đính kèm `traceId`:

**JSON Response (401 - Chưa đăng nhập):**
```json
{
    "timestamp": "2026-06-19T14:30:00",
    "status": 401,
    "error": "Unauthorized",
    "message": "Full authentication is required to access this resource",
    "path": "/api/admin/dashboard",
    "traceId": "fa7a4e6b-a25f-4ce9-8973-2e06cbe19e7a"
}
```

**JSON Response (403 - Thiếu quyền):**
```json
{
    "timestamp": "2026-06-19T14:30:00",
    "status": 403,
    "error": "Forbidden",
    "message": "Access Denied",
    "path": "/api/admin/dashboard",
    "traceId": "fa7a4e6b-a25f-4ce9-8973-2e06cbe19e7a"
}
```

### Cách áp dụng vào cấu hình Spring Security:
Trong class cấu hình Spring Security (`SecurityFilterChain`), bạn chỉ cần autowire 2 bean này và truyền vào cấu hình `exceptionHandling`:

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ... các cấu hình khác ...
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            );
        return http.build();
    }
}
```

---

## ⚙️ Tùy chỉnh Thông báo Lỗi (Custom Error Messages)

Bạn có thể tùy chỉnh lại toàn bộ các thông báo lỗi mặc định của thư viện thông qua các file cấu hình `application.properties` hoặc `application.yml` trong dự án sử dụng với prefix `atmin.exceptions`.

### Cấu hình qua YAML (`application.yml`):
```yaml
atmin:
  exceptions:
    # Lỗi validation khi dùng @Valid (mặc định: "Validation failed")
    validation-failed: "Dữ liệu gửi lên không đúng định dạng!"
    # Lỗi 500 catch-all (mặc định: "An unexpected error occurred on the server.")
    unexpected-error: "Hệ thống gặp sự cố. Vui lòng liên hệ quản trị viên."
    # Lỗi 403 khi dùng @PreAuthorize/@Secured (mặc định: "You do not have permission...")
    access-denied: "Tài khoản của bạn không được phép thực hiện hành động này."
    # Lỗi upload file quá dung lượng (mặc định: "File size exceeds...")
    file-too-large: "Dung lượng file vượt quá giới hạn tối đa cho phép."
    # Lỗi 401 tầng Security Filter (mặc định: "Full authentication is required...")
    security-unauthorized: "Vui lòng đăng nhập trước khi tiếp tục."
    # Lỗi 403 tầng Security Filter (mặc định: "Access Denied")
    security-access-denied: "Đường dẫn bị khóa, bạn không có quyền truy cập."
```

### Cấu hình qua Properties (`application.properties`):
```properties
atmin.exceptions.validation-failed=Dữ liệu gửi lên không đúng định dạng!
atmin.exceptions.unexpected-error=Hệ thống gặp sự cố. Vui lòng liên hệ quản trị viên.
atmin.exceptions.access-denied=Tài khoản của bạn không được phép thực hiện hành động này.
atmin.exceptions.file-too-large=Dung lượng file vượt quá giới hạn tối đa cho phép.
atmin.exceptions.security-unauthorized=Vui lòng đăng nhập trước khi tiếp tục.
atmin.exceptions.security-access-denied=Đường dẫn bị khóa, bạn không có quyền truy cập.
```

Nếu không cấu hình các thuộc tính này, thư viện sẽ tự động sử dụng các giá trị mặc định bằng tiếng Anh.

---

## Cách kích hoạt Handlers

### Cách 1: Auto-configuration (mặc định, không cần làm gì)

```java
@SpringBootApplication
public class MyApplication { }
// Core + Storage handlers tự bật
// Security handler tự bật nếu có Spring Security + JWT dependency
```

### Cách 2: Dùng annotation @EnableAtminExceptionHandling

```java
@SpringBootApplication
@EnableAtminExceptionHandling(
    enableSecurityHandlers = true,
    enableStorageHandlers = true
)
public class MyApplication { }
```

### Cách 3: Tắt auto-config, chỉ dùng annotation

```java
@SpringBootApplication(exclude = AtminExceptionAutoConfiguration.class)
@EnableAtminExceptionHandling(enableSecurityHandlers = true)
public class MyApplication { }
```

---

## Tự tạo Handler mới (mở rộng)

Nếu muốn thêm handler riêng, tạo class với `@RestControllerAdvice`:

```java
@RestControllerAdvice
@Slf4j
public class MyCustomExceptionHandler {

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimitException(
            RateLimitException ex, HttpServletRequest request) {

        log.warn("Rate limit exceeded: {}", ex.getMessage());

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.TOO_MANY_REQUESTS);
    }
}
```

> **Lưu ý:** Handler tự tạo trong project có **ưu tiên cao hơn** handler trong thư viện.
> Nếu bạn muốn override handler có sẵn, chỉ cần tạo handler mới bắt cùng exception.
