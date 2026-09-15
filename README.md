# atmin-library 2.1

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F.svg)](https://spring.io/projects/spring-boot)
[![Version](https://img.shields.io/badge/version-2.1.0-blue.svg)](https://central.sonatype.com/artifact/io.github.duongtran1702/atmin-library)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

`atmin-library` là thư viện dùng chung cho REST API chạy trên Spring Boot 4. File này là hướng dẫn độc lập, đầy đủ cho Spring MVC và các API dùng chung: cài đặt, response, phân trang, exception, security, trace ID, cấu hình, kiểm thử và phát hành.

Chọn đúng một tài liệu theo web stack; không cần đọc cả hai:

- **Spring MVC/Servlet:** chỉ cần file này.
- **Spring WebFlux/Gateway:** có thể dùng độc lập [README-MICROSERVICES.md](README-MICROSERVICES.md).

Phiên bản 2.1 giữ nguyên cách dùng và JSON contract của dòng 1.x/2.0. Các API cũ như `ApiResponse.success(...)`, `created(...)`, `paginated(...)`, `slicePaginated(...)`, `PageInfo.from(...)`, các custom exception và `@EnableAtminExceptionHandling` vẫn hoạt động. Những khả năng mới được bổ sung theo hướng cộng thêm.

## Mục lục

- [Điểm nổi bật](#điểm-nổi-bật)
- [Yêu cầu môi trường](#yêu-cầu-môi-trường)
- [Bảng khả năng MVC và WebFlux](#bảng-khả-năng-mvc-và-webflux)
- [Cài đặt nhanh](#cài-đặt-nhanh)
- [ApiResponse](#apiresponse)
- [PageInfo](#pageinfo)
- [ApiErrorResponse](#apierrorresponse)
- [Custom exception](#custom-exception)
- [Exception handler tự động](#exception-handler-tự-động)
- [Cấu hình](#cấu-hình)
- [Lỗi thường gặp trong microservice](#lỗi-thường-gặp-trong-microservice)
- [Spring Security và JWT](#spring-security-và-jwt)
- [Trace ID](#trace-id)
- [Tùy biến và ghi đè](#tùy-biến-và-ghi-đè)
- [Nâng cấp từ 1.x](#nâng-cấp-từ-1x)
- [Phát triển và kiểm thử](#phát-triển-và-kiểm-thử)
- [Đóng gói và phát hành](#đóng-gói-và-phát-hành)
- [Xử lý sự cố](#xử-lý-sự-cố)

## Điểm nổi bật

- Auto-configuration chuẩn Spring Boot qua `AutoConfiguration.imports`; thêm dependency là dùng được.
- Giữ nguyên response contract của 1.x để nâng cấp ít rủi ro.
- Tương thích cấu trúc module mới của Spring Boot 4 và Jackson 3.
- `ApiResponse<T>` cho success, created, accepted, no-content và error.
- Hỗ trợ `Page`, `Slice` và metadata phân trang tạo thủ công.
- Response lỗi đồng nhất, có validation errors và `traceId`.
- Không trả chi tiết lỗi nội bộ cho client khi xảy ra lỗi 500.
- Giữ đúng status của `ResponseStatusException` và các lỗi Spring MVC thay vì đổi thành 500.
- Xử lý JSON sai định dạng, thiếu parameter/part, validation body và validation method.
- Chuẩn hóa lỗi gọi service khác từ `RestClient`, `RestTemplate` và tùy chọn `WebClient`.
- Có exception riêng cho 429, 502, 503 và 504; hỗ trợ header `Retry-After`.
- Spring Security có thể hoạt động độc lập, không còn bắt buộc phải cài JJWT.
- JJWT có thể hoạt động độc lập nếu ứng dụng không dùng Spring Security.
- Từng module Security, Security Filter và Storage có thể bật/tắt riêng.
- Handler của ứng dụng được ưu tiên nhờ `@ConditionalOnMissingBean`.
- Có configuration metadata cho gợi ý thuộc tính trong IDE.
- Có sources JAR, Javadoc JAR, ký GPG và bundle dành cho Maven Central.

## Yêu cầu môi trường

| Thành phần | Yêu cầu |
|---|---|
| Java | 21 trở lên |
| Spring Boot | 4.x; bản build hiện tại dùng BOM 4.1.1 |
| Web stack | `spring-boot-starter-webmvc` hoặc `spring-boot-starter-webflux` |
| Gradle khi phát triển thư viện | Dùng wrapper đi kèm, hiện là 9.5.1 |
| Runtime | Servlet MVC, Reactive WebFlux và Gateway tùy chọn |

Thư viện dùng package `jakarta.*`, Spring Framework 7 và Jackson 3 theo Spring Boot 4. Không dùng bản 2.1 này cho Spring Boot 2.x hoặc 3.x.

## Bảng khả năng MVC và WebFlux

| Tính năng | Monolithic/MVC | Microservice/WebFlux |
|---|:---:|:---:|
| `ApiResponse`, `ApiErrorResponse`, `PageInfo` | Có | Có |
| Custom exception 400–504 | Có | Có |
| Validation body/method | Có | Có |
| Security 401/403 JSON | Servlet Security | Reactive Security |
| RestClient/RestTemplate downstream | Có | Không áp dụng |
| WebClient downstream | Có, dependency tùy chọn | Có |
| Trace propagation | MDC | Reactor Context + request header |
| `X-Trace-Id` response header | Qua tracing của ứng dụng | Tự động |
| Gateway filter-chain error | Không áp dụng | Có, khi Gateway hiện diện |
| Upload/storage handler | Servlet multipart | Custom storage exception; streaming thuộc ứng dụng |

Spring Boot tự chọn đúng auto-configuration theo web application type, nên hai handler server không được đăng ký đồng thời.

## Cài đặt nhanh

### Gradle Kotlin DSL

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("io.github.duongtran1702:atmin-library:2.1.0")
}
```

### Gradle Groovy DSL

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    implementation 'io.github.duongtran1702:atmin-library:2.1.0'
}
```

### Maven

Khi project kế thừa Spring Boot parent hoặc import Spring Boot BOM:

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>

    <dependency>
        <groupId>io.github.duongtran1702</groupId>
        <artifactId>atmin-library</artifactId>
        <version>2.1.0</version>
    </dependency>
</dependencies>
```

Với WebFlux, thay starter MVC bằng:

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'io.github.duongtran1702:atmin-library:2.1.0'
}
```

Đoạn WebFlux trên chỉ giúp nhận diện dependency khi so sánh web stack; người dùng reactive có hướng dẫn độc lập riêng, không cần kết hợp nội dung với file này.

Sau khi thêm dependency, không cần thêm annotation:

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

Auto-configuration mặc định bật Core, Storage và mọi integration tùy chọn có dependency phù hợp trên classpath.

## ApiResponse

Import:

```java
import atmin.common.response.ApiResponse;
```

Cấu trúc JSON:

```json
{
  "success": true,
  "status": 200,
  "message": "Lấy dữ liệu thành công",
  "data": {},
  "page": {}
}
```

`data` và `page` bị loại khỏi JSON khi mang giá trị `null`.

### Success 200

```java
ApiResponse<UserDto> response =
        ApiResponse.success("Lấy người dùng thành công", user);
```

Không có data:

```java
ApiResponse<Void> response =
        ApiResponse.success("Thao tác thành công");
```

### Created 201

```java
return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.created("Tạo người dùng thành công", savedUser));
```

### Accepted 202

```java
return ResponseEntity.accepted()
        .body(ApiResponse.accepted("Yêu cầu đang được xử lý", jobId));
```

### Status tùy chỉnh

API cũ với `HttpStatus` vẫn giữ nguyên, đồng thời từ 2.0 nhận mọi `HttpStatusCode`:

```java
ApiResponse<MyDto> response = ApiResponse.success(
        HttpStatus.PARTIAL_CONTENT,
        "Trả về một phần dữ liệu",
        data
);
```

### No Content 204

```java
ApiResponse<Void> response = ApiResponse.noContent("Đã xóa");
```

Nếu endpoint thực sự trả HTTP 204, HTTP không nên có body. Có thể dùng:

```java
return ResponseEntity.noContent().build();
```

### Error response đơn giản

```java
ApiResponse<Void> badRequest = ApiResponse.badRequest("Dữ liệu không hợp lệ");
ApiResponse<Void> notFound = ApiResponse.notFound("Không tìm thấy dữ liệu");
ApiResponse<Void> serverError = ApiResponse.internalError("Có lỗi xảy ra");
ApiResponse<Void> custom = ApiResponse.error(HttpStatus.CONFLICT, "Dữ liệu bị trùng");
```

Đối với exception handler, nên dùng `ApiErrorResponse` để có timestamp, path, validation errors và trace ID.

## PageInfo

Import:

```java
import atmin.common.response.PageInfo;
```

### Spring Data Page

Cách dùng 1.x vẫn giữ nguyên:

```java
Page<UserDto> page = userService.findAll(pageable);

return ResponseEntity.ok(
        ApiResponse.paginated(
                "Danh sách người dùng",
                page.getContent(),
                PageInfo.from(page)
        )
);
```

Từ 2.0 có factory ngắn hơn:

```java
return ResponseEntity.ok(
        ApiResponse.pagePaginated("Danh sách người dùng", page)
);
```

JSON:

```json
{
  "success": true,
  "status": 200,
  "message": "Danh sách người dùng",
  "data": [
    { "id": 1, "name": "An" },
    { "id": 2, "name": "Bình" }
  ],
  "page": {
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 45,
    "totalPages": 3,
    "hasNext": true,
    "first": true,
    "last": false
  }
}
```

### Spring Data Slice

`Slice` phù hợp infinite scroll hoặc “load more” vì không yêu cầu truy vấn tổng số phần tử:

```java
Slice<UserDto> slice = userService.findSlice(pageable);

return ResponseEntity.ok(
        ApiResponse.slicePaginated("Danh sách người dùng", slice)
);
```

JSON chỉ giữ metadata cần thiết:

```json
{
  "success": true,
  "status": 200,
  "message": "Danh sách người dùng",
  "data": [
    { "id": 1, "name": "An" }
  ],
  "page": {
    "pageNumber": 0,
    "pageSize": 20,
    "hasNext": true
  }
}
```

### Tạo metadata thủ công

Có tổng số phần tử:

```java
PageInfo pageInfo = PageInfo.of(0, 20, 45);
```

Không chạy count query:

```java
PageInfo pageInfo = PageInfo.slice(0, 20, true);
```

`pageNumber` phải từ 0, `pageSize` phải lớn hơn 0 và `totalElements` không được âm. Dữ liệu không hợp lệ sẽ tạo `IllegalArgumentException`.

## ApiErrorResponse

Import:

```java
import atmin.common.response.ApiErrorResponse;
```

Các field cũ được giữ nguyên. Bản 2.1 thêm ba field tùy chọn; chúng không xuất hiện trong JSON của factory/exception cũ khi giá trị là `null`:

```java
public class ApiErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> errors;
    private String traceId;
    private AtminErrorCode code; // nullable: mã ổn định cho client
    private Boolean retryable;   // nullable: lỗi có phù hợp để retry không
    private String service;      // nullable: logical service name, không tự suy ra hostname
}
```

Ví dụ 404:

```json
{
  "timestamp": "2026-09-04T15:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id: '123'",
  "path": "/api/users/123",
  "traceId": "414b38b7-6aca-44f1-aef2-0fa8ce584a99"
}
```

Factory methods:

```java
ApiErrorResponse.badRequest(message, path);
ApiErrorResponse.unauthorized(message, path);
ApiErrorResponse.forbidden(message, path);
ApiErrorResponse.notFound(message, path);
ApiErrorResponse.conflict(message, path);
ApiErrorResponse.internalServerError(message, path);
ApiErrorResponse.serviceUnavailable(message, path);
ApiErrorResponse.of(HttpStatusCode.valueOf(422), message, path);
```

Validation:

```java
Map<String, String> errors = Map.of(
        "email", "Email không hợp lệ",
        "name", "Tên không được để trống"
);

ApiErrorResponse.validationError("/api/users", errors);

// Từ bản 2.0: tùy chỉnh summary message
ApiErrorResponse.validationError(
        "/api/users",
        "Dữ liệu đầu vào không hợp lệ",
        errors
);
```

## Custom exception

Các exception 1.x được giữ nguyên package và constructor:

| Exception | HTTP status | Trường hợp dùng |
|---|---:|---|
| `BadRequestException` | 400 | Request sai nghiệp vụ hoặc dữ liệu |
| `UnauthorizedException` | 401 | Chưa xác thực hoặc xác thực không hợp lệ |
| `ForbiddenException` | 403 | Đã xác thực nhưng không có quyền |
| `ResourceNotFoundException` | 404 | Không tìm thấy resource |
| `DuplicateResourceException` | 409 | Tạo resource bị trùng |
| `ConflictException` | 409 | Xung đột trạng thái hoặc dữ liệu |
| `RateLimitExceededException` | 429 | Client vượt giới hạn request |
| `DownstreamServiceException` | 502 | Service phụ thuộc trả response lỗi/không hợp lệ |
| `ServiceUnavailableException` | 503 | Dịch vụ phụ thuộc không sẵn sàng |
| `CircuitBreakerOpenException` | 503 | Circuit breaker đang mở và từ chối lời gọi |
| `GatewayTimeoutException` | 504 | Service phụ thuộc không phản hồi trước deadline |
| `CloudStorageException` | 503 | Lỗi S3, GCS, Azure Blob hoặc storage khác |

Ví dụ:

```java
throw new BadRequestException("Email không hợp lệ");

throw new ResourceNotFoundException("User", "id", userId);
// message: User not found with id: '123'

throw new DuplicateResourceException(
        "Email đã tồn tại",
        originalException
);

throw new ServiceUnavailableException("payment-service", true);

throw new DownstreamServiceException("payment-service", 500);

throw GatewayTimeoutException.forService("inventory-service");

throw new RateLimitExceededException("Bạn thao tác quá nhanh", 30);

throw CircuitBreakerOpenException.forService("catalog-service");
```

Các constructor nhận `Throwable cause` được giữ lại để bảo toàn stack trace.

## Exception handler tự động

### Core handler

Luôn được đăng ký trong servlet web application, trừ khi tắt toàn bộ thư viện hoặc ứng dụng đã có bean `CoreExceptionHandler`.

Core xử lý:

| Nhóm lỗi | Status |
|---|---:|
| `MethodArgumentNotValidException` từ `@Valid @RequestBody` | 400 |
| `HandlerMethodValidationException` | 400 |
| JSON không đọc được | 400 |
| Thiếu request parameter hoặc multipart part | 400 |
| Sai kiểu method argument | 400 |
| `IllegalArgumentException`, `BadRequestException` | 400 |
| `UnauthorizedException` | 401 |
| `ForbiddenException` | 403 |
| `ResourceNotFoundException`, route/resource không tồn tại | 404 |
| `DuplicateResourceException`, `ConflictException` | 409 |
| HTTP method không hỗ trợ | 405 |
| Media type không chấp nhận/không hỗ trợ | 406/415 |
| `ResponseStatusException`, `ErrorResponseException` | Giữ nguyên status |
| `ServiceUnavailableException` | 503 |
| Exception còn lại | 500 với public message an toàn |

Ví dụ validation:

```java
public record CreateUserRequest(
        @NotBlank String name,
        @Email String email
) {}

@PostMapping("/users")
public ApiResponse<UserDto> create(
        @Valid @RequestBody CreateUserRequest request) {
    return ApiResponse.created("Đã tạo", userService.create(request));
}
```

Response:

```json
{
  "timestamp": "2026-09-04T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/users",
  "errors": {
    "name": "must not be blank",
    "email": "must be a well-formed email address"
  },
  "traceId": "4bce4786-8128-4d87-9e57-cdc7caa41dc1"
}
```

### Bean Validation handler

Khi `jakarta.validation.ConstraintViolationException` có trên classpath, thư viện tự xử lý validation ở service/method và đưa constraint path vào `errors`.

Để dùng validation trong ứng dụng:

```groovy
implementation 'org.springframework.boot:spring-boot-starter-validation'
```

### Storage handler

Storage handler xử lý:

- `MaxUploadSizeExceededException` thành 400 với message cấu hình.
- `CloudStorageException` thành 503.

Có thể tắt riêng bằng `atmin.exceptions.storage-enabled=false`.

## Cấu hình

Tất cả thuộc tính dùng prefix `atmin.exceptions`.

| Thuộc tính | Mặc định | Mô tả |
|---|---|---|
| `enabled` | `true` | Bật/tắt toàn bộ auto-configuration |
| `security-enabled` | `true` | Bật handler Security/JWT ở controller |
| `security-filter-enabled` | `true` | Tạo AuthenticationEntryPoint và AccessDeniedHandler |
| `storage-enabled` | `true` | Bật handler upload/storage |
| `microservice-enabled` | `true` | Bật handler lỗi giao tiếp giữa các service |
| `downstream-preserve-client-errors` | `true` | Giữ 400/404/409/422/429 từ downstream; các status khác được chuẩn hóa thành 502 |
| `downstream-expose-service-name` | `false` | Cho phép trả logical service name do ứng dụng chủ động cung cấp |
| `trace-id-mdc-key` | `traceId` | Key đọc trace ID từ SLF4J MDC |
| `trace-id-header` | `X-Trace-Id` | Header nhận/trả trace ID trong WebFlux/Gateway |
| `echo-trace-id-header` | `true` | Trả trace ID trong response header reactive |
| `gateway-enabled` | `true` | Bật low-level handler khi Spring Cloud Gateway có mặt; đặt `false` trước khi đăng ký Gateway error handler riêng |
| `gateway-order` | `-2` | Thứ tự của Gateway error handler |
| `validation-failed` | `Validation failed` | Summary của validation error |
| `malformed-request` | `Request body is malformed or unreadable.` | JSON/body không đọc được |
| `missing-request-value` | `A required request value is missing.` | Thiếu parameter/part |
| `downstream-service-error` | `A downstream service returned an unsuccessful response.` | Upstream trả 4xx/5xx |
| `downstream-service-unavailable` | `A downstream service is temporarily unavailable.` | Lỗi kết nối, DNS hoặc TLS tới upstream |
| `downstream-timeout` | `A downstream service did not respond in time.` | Upstream quá thời gian chờ |
| `unexpected-error` | `An unexpected error occurred on the server.` | Public message cho 500 |
| `access-denied` | `You do not have permission to access this resource.` | Lỗi 403 ở controller |
| `file-too-large` | `File size exceeds the maximum allowed limit.` | Upload vượt giới hạn |
| `security-unauthorized` | `Full authentication is required to access this resource` | Lỗi 401 từ Security/JWT |
| `security-access-denied` | `Access Denied` | Lỗi 403 từ Security filter |

Ví dụ `application.yml`:

```yaml
atmin:
  exceptions:
    enabled: true
    security-enabled: true
    security-filter-enabled: true
    storage-enabled: true
    microservice-enabled: true
    downstream-preserve-client-errors: true
    downstream-expose-service-name: false
    trace-id-mdc-key: traceId
    trace-id-header: X-Trace-Id
    echo-trace-id-header: true
    gateway-enabled: true
    gateway-order: -2
    validation-failed: Dữ liệu đầu vào không hợp lệ
    malformed-request: JSON gửi lên không hợp lệ
    missing-request-value: Thiếu dữ liệu bắt buộc
    downstream-service-error: Dịch vụ phụ thuộc trả về lỗi
    downstream-service-unavailable: Dịch vụ phụ thuộc tạm thời không khả dụng
    downstream-timeout: Dịch vụ phụ thuộc phản hồi quá chậm
    unexpected-error: Hệ thống đang gặp sự cố
    access-denied: Bạn không có quyền thực hiện thao tác này
    file-too-large: Tệp vượt quá dung lượng cho phép
    security-unauthorized: Vui lòng đăng nhập
    security-access-denied: Bạn không có quyền truy cập
```

Ví dụ `application.properties`:

```properties
atmin.exceptions.validation-failed=Dữ liệu đầu vào không hợp lệ
atmin.exceptions.unexpected-error=Hệ thống đang gặp sự cố
atmin.exceptions.trace-id-mdc-key=correlationId
```

Tắt toàn bộ:

```properties
atmin.exceptions.enabled=false
```

Tắt riêng Security nhưng giữ Core và Storage:

```properties
atmin.exceptions.security-enabled=false
atmin.exceptions.security-filter-enabled=false
```

## Lỗi thường gặp trong microservice

Microservice có thêm một lớp lỗi nằm giữa các service. Bản 2.1 giữ các field cũ, đồng thời có thể thêm `code`, `retryable`, `service` cho lỗi mới:

| Nguồn lỗi | Response của API hiện tại | Ghi chú |
|---|---:|---|
| Downstream 400/404/409/422/429 qua `RestClient`, `RestTemplate`, `WebClient` | Giữ status | Có thể tắt bằng `downstream-preserve-client-errors=false`; 429 giữ `Retry-After` dạng số an toàn |
| Downstream status khác | 502 | Không phát tán body lỗi nội bộ của upstream |
| `ResourceAccessException` do timeout | 504 | Nhận diện timeout trong cause chain |
| `ResourceAccessException` do connection/DNS/TLS | 503 | Upstream tạm thời không truy cập được |
| `UnknownHostException` | 503 | DNS không phân giải được hostname/service name |
| `ConnectException`, `NoRouteToHostException` | 503 | Connection refused hoặc không có route tới service |
| `SocketTimeoutException`, `HttpTimeoutException` | 504 | Timeout thoát ra trực tiếp từ HTTP client/JDK |
| `SSLException` / TLS handshake | 503 | Không trả certificate, hostname hoặc chi tiết hạ tầng |
| `WebClientRequestException` do timeout | 504 | Dùng được khi ứng dụng MVC gọi downstream bằng `WebClient` |
| `WebClientRequestException` do transport | 503 | Không biến ứng dụng thành WebFlux server |
| `RateLimitExceededException` | 429 | Có thể kèm `Retry-After` theo giây |
| `CircuitBreakerOpenException` | 503 | Trung lập với Resilience4j/Spring Retry |
| `DownstreamServiceException` | 502 | Dùng khi tự chuyển lỗi từ Feign hoặc client khác |
| `GatewayTimeoutException` | 504 | Dùng khi timeout đã được phát hiện ở service layer |

Ví dụ với `RestClient` — nếu exception được để lan tới controller advice thì thư viện tự xử lý:

```java
OrderDto order = restClient.get()
        .uri("http://order-service/api/orders/{id}", id)
        .retrieve()
        .body(OrderDto.class);
```

Ví dụ khi dùng OpenFeign hoặc client khác không phải dependency của thư viện:

```java
try {
    return paymentClient.pay(request);
} catch (FeignException ex) {
    throw new DownstreamServiceException(
            "payment-service",
            ex.status(),
            ex
    );
}
```

Ví dụ fallback circuit breaker:

```java
public PaymentResult paymentFallback(
        PaymentRequest request,
        Throwable cause) {
    throw CircuitBreakerOpenException.forService(
            "payment-service",
            cause
    );
}
```

Rate limit có thời gian retry:

```java
throw new RateLimitExceededException(
        "Vượt quá số request cho phép",
        60
);
```

Response sẽ có HTTP 429 và header:

```http
Retry-After: 60
```

Có thể tắt toàn bộ nhóm này mà không ảnh hưởng Core/Security/Storage:

```properties
atmin.exceptions.microservice-enabled=false
```

Nếu ứng dụng tự khai báo bean `MicroserviceExceptionHandler` hoặc `WebClientExceptionHandler`, auto-configuration sẽ nhường quyền cho bean đó.

Ví dụ, hostname sai hoặc service chưa cùng Docker/Kubernetes network có thể tạo `UnknownHostException`. Thư viện trả 503 với public message an toàn; hostname và stack trace chỉ được ghi ở server:

```json
{
  "status": 503,
  "error": "Service Unavailable",
  "message": "A downstream service is temporarily unavailable.",
  "path": "/api/orders",
  "traceId": "..."
}
```

> Exception từ Kafka/RabbitMQ consumer không đi qua Spring MVC controller nên không thể được xử lý đúng bằng `@ControllerAdvice`. Với messaging, hãy cấu hình retry, dead-letter queue và error handler của broker/framework; sau đó dùng `traceId` để liên kết log.

## Spring Security và JWT

### Chỉ dùng Spring Security

Từ 2.0 không bắt buộc JJWT để xử lý `AuthenticationException` và `AccessDeniedException`:

```groovy
implementation 'org.springframework.boot:spring-boot-starter-security'
```

### Dùng JJWT

```groovy
implementation 'io.jsonwebtoken:jjwt-api:0.13.0'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.13.0'
```

Thư viện chỉ yêu cầu `jjwt-api` để nhận diện `JwtException`. Serializer/deserializer JWT cụ thể do ứng dụng lựa chọn.

### Lỗi ở Security filter chain

Exception trong Security filter xảy ra trước controller, vì vậy `@ExceptionHandler` không nhận được. Khi Jackson 3 và Spring Security có mặt, thư viện cung cấp hai bean:

- `AuthenticationEntryPoint`: trả 401 theo `ApiErrorResponse`.
- `AccessDeniedHandler`: trả 403 theo `ApiErrorResponse`.

Gắn vào `SecurityFilterChain`:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler) throws Exception {

        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/public/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .build();
    }
}
```

Nếu ứng dụng tự khai báo bean cùng type, auto-configuration sẽ nhường quyền cho bean của ứng dụng.

Vì lý do an toàn, lỗi authentication/JWT trả message cấu hình `security-unauthorized`, không đưa message nội bộ của token hoặc credential ra client. Chi tiết exception vẫn được ghi log phía server.

## Trace ID

`ApiErrorResponse` luôn có `traceId`. Filter của thư viện nhận và echo `X-Trace-Id` cho cả Servlet/MVC lẫn WebFlux; giá trị không hợp lệ hoặc dài quá 128 ký tự sẽ được thay bằng UUID an toàn.

Thứ tự xử lý:

1. Đọc SLF4J MDC bằng key đã cấu hình; mặc định là `traceId`.
2. Nếu không có hoặc giá trị rỗng, sinh UUID.

Cấu hình khuyên dùng:

```yaml
atmin:
  exceptions:
    trace-id-mdc-key: traceId
```

API 1.x vẫn còn:

```java
ApiErrorResponse.setMdcKey("correlationId");
```

Với dòng 2.x nên ưu tiên property để IDE nhận diện và cấu hình tập trung. Không đặt `X-Request-Id` làm MDC key trừ khi ứng dụng đã chủ động đưa header đó vào MDC.

## Tùy biến và ghi đè

### Dùng annotation thủ công

`@EnableAtminExceptionHandling` vẫn được giữ để tương thích với 1.x. Chỉ dùng khi cần loại auto-configuration và chọn module bằng annotation:

```java
@SpringBootApplication(exclude = AtminExceptionAutoConfiguration.class)
@EnableAtminExceptionHandling(
        enableSecurityHandlers = true,
        enableStorageHandlers = true
)
public class Application {
}
```

Core luôn bật trong manual mode. Security/JWT chỉ được import khi dependency tương ứng tồn tại.

### Ghi đè CoreExceptionHandler

```java
@RestControllerAdvice
public class ApplicationExceptionHandler extends CoreExceptionHandler {

    public ApplicationExceptionHandler(AtminExceptionProperties properties) {
        super(properties);
    }

    @ExceptionHandler(MyBusinessException.class)
    ResponseEntity<ApiErrorResponse> handleBusiness(
            MyBusinessException ex,
            HttpServletRequest request) {
        return ResponseEntity.unprocessableContent()
                .body(ApiErrorResponse.of(
                        HttpStatus.UNPROCESSABLE_CONTENT,
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }
}
```

Do bean tùy chỉnh là một `CoreExceptionHandler`, auto-configuration không tạo Core advice mặc định.

### Tắt auto-configuration bằng class

```java
@SpringBootApplication(exclude = AtminExceptionAutoConfiguration.class)
public class Application {
}
```

Hoặc:

```properties
spring.autoconfigure.exclude=atmin.common.config.AtminExceptionAutoConfiguration
```

## Nâng cấp từ 1.x

### Bước 1: đổi version

```groovy
implementation 'io.github.duongtran1702:atmin-library:2.1.0'
```

### Bước 2: dùng starter Spring Boot 4 mới

```groovy
implementation 'org.springframework.boot:spring-boot-starter-webmvc'
```

Spring Boot 4 đã tách các starter theo module. Với project mới nên dùng `spring-boot-starter-webmvc` thay cho tên `spring-boot-starter-web` cũ.

### Bước 3: kiểm tra Jackson nếu có custom integration

Spring Boot 4 dùng Jackson 3:

```java
import tools.jackson.databind.ObjectMapper;
```

Các annotation `JsonInclude`, `JsonFormat` vẫn ở package:

```java
import com.fasterxml.jackson.annotation.JsonInclude;
```

Ứng dụng chỉ dùng response/handler của thư viện không cần tự sửa mapper.

### Những gì không đổi

- Group/artifact: `io.github.duongtran1702:atmin-library`.
- Package gốc: `atmin.common`.
- Tên và constructor của các exception cũ.
- Field JSON của `ApiResponse`, `ApiErrorResponse`, `PageInfo`.
- Factory methods 1.x.
- Auto-configuration mặc định.
- `@EnableAtminExceptionHandling`.

### Thay đổi hành vi có chủ đích

- Spring MVC exception giữ status gốc, không bị catch-all đổi thành 500.
- Lỗi Security/JWT dùng public message cấu hình thay vì lộ message nội bộ.
- Security handler hoạt động kể cả khi không có JJWT.
- Custom `CoreExceptionHandler` thực sự làm auto-configuration back off.
- `PageInfo.of` và `PageInfo.slice` từ chối page metadata không hợp lệ.
- Security filter serialization dùng Jackson 3 mặc định của Spring Boot 4.

## Phát triển và kiểm thử

Clone repository, dùng Java 21 rồi chạy:

```powershell
.\gradlew.bat clean check
```

Trên macOS/Linux:

```bash
./gradlew clean check
```

`check` chạy test và kiểm tra Javadoc. Bộ test bao phủ:

- factory methods cũ và mới;
- `Page`, `Slice`, pagination thủ công;
- MDC trace ID và UUID fallback;
- custom exception/status;
- safe 500 response;
- malformed JSON;
- auto-configuration;
- module switches;
- Security không có JJWT;
- custom bean back-off.

Các lệnh hữu ích:

```powershell
# Test
.\gradlew.bat test

# Tạo JAR, sources JAR và Javadoc JAR
.\gradlew.bat build

# Cài vào Maven local để test bằng project khác
.\gradlew.bat publishToMavenLocal

# Tạo bundle Maven Central đã ký
.\gradlew.bat zipBundle
```

## Đóng gói và phát hành

Thư viện đã đặt version `2.1.0`. Bạn chỉ cần tự commit, push và publish khi sẵn sàng.

### Cấu hình credential

Không commit secret vào repository. Đặt trong `~/.gradle/gradle.properties`:

```properties
signing.keyId=YOUR_GPG_KEY_ID
signing.password=YOUR_GPG_PASSWORD
signing.secretKeyRingFile=C:/Users/your-user/.gnupg/secring.gpg

centralUsername=YOUR_CENTRAL_TOKEN_USERNAME
centralPassword=YOUR_CENTRAL_TOKEN_PASSWORD
```

Hoặc dùng environment variables:

```text
MAVEN_CENTRAL_USERNAME
MAVEN_CENTRAL_PASSWORD
```

### Tạo bundle

```powershell
.\gradlew.bat clean check zipBundle
```

File kết quả:

```text
build/distributions/atmin-library-2.1.0-bundle.zip
```

### Upload bằng task có sẵn

```powershell
.\gradlew.bat deploy
```

`deploy` upload bundle lên Sonatype Central Portal với chế độ `USER_MANAGED`. Sau khi validation thành công, vào Central Portal kiểm tra và bấm Publish.

Hướng dẫn chi tiết: [docs/publishing.md](docs/publishing.md).

## Cấu trúc dự án

```text
src/main/java/atmin/common
├── annotation
│   └── EnableAtminExceptionHandling.java
├── config
│   ├── AtminExceptionAutoConfiguration.java
│   ├── AtminReactiveExceptionAutoConfiguration.java
│   ├── AtminServletTraceFilter.java
│   ├── AtminExceptionHandlerRegistrar.java
│   ├── AtminExceptionProperties.java
│   ├── SecurityExceptionConfig.java
│   └── ReactiveSecurityExceptionConfig.java
├── exception
│   ├── ... custom exceptions 400–504
│   └── handler
│       ├── CoreExceptionHandler.java
│       ├── SecurityExceptionHandler.java
│       ├── SpringSecurityExceptionHandler.java
│       ├── JwtExceptionHandler.java
│       ├── ValidationExceptionHandler.java
│       ├── MicroserviceExceptionHandler.java
│       ├── WebClientExceptionHandler.java
│       └── StorageExceptionHandler.java
├── reactive
│   ├── AtminReactiveTraceFilter.java
│   ├── ReactiveCoreExceptionHandler.java
│   └── AtminGatewayErrorWebExceptionHandler.java
├── trace
│   └── TraceIdResolver.java
└── response
    ├── ApiResponse.java
    ├── ApiErrorResponse.java
    ├── AtminErrorCode.java
    └── PageInfo.java
```

Tài liệu bổ sung:

- [Response classes](docs/responses.md)
- [Custom exceptions](docs/exceptions.md)
- [Exception handlers](docs/handlers.md)
- [Publishing](docs/publishing.md)

## Xử lý sự cố

### Handler không tự hoạt động

Kiểm tra:

1. Project dùng Spring MVC hoặc Spring WebFlux và application type được Spring Boot nhận diện đúng.
2. Có đúng starter: `spring-boot-starter-webmvc` hoặc `spring-boot-starter-webflux`.
3. `atmin.exceptions.enabled` không bị đặt thành `false`.
4. Auto-configuration không nằm trong `spring.autoconfigure.exclude`.
5. Không có custom bean `CoreExceptionHandler` khác.

Bật debug auto-configuration:

```properties
debug=true
```

### Security trả HTML/default response

Controller advice không bắt exception phát sinh trong Security filter chain. Hãy gắn `AuthenticationEntryPoint` và `AccessDeniedHandler` của thư viện vào `SecurityFilterChain` như ví dụ phía trên.

### Không có validation errors

Thêm:

```groovy
implementation 'org.springframework.boot:spring-boot-starter-validation'
```

Và dùng `@Valid` hoặc `@Validated` đúng vị trí.

### Không đọc được Page hoặc Slice

Thêm Spring Data phù hợp, ví dụ:

```groovy
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
```

Nếu không dùng Spring Data, dùng `ApiResponse.paginated(...)` cùng `PageInfo.of(...)` hoặc `PageInfo.slice(...)`.

### Trace ID luôn là UUID mới

Điều này nghĩa là MDC chưa có giá trị ở thời điểm tạo response. Đảm bảo tracing/filter của ứng dụng đặt đúng key và `atmin.exceptions.trace-id-mdc-key` khớp với key đó.

### Lỗi gọi service khác vẫn trả 500

Kiểm tra exception gốc có bị service của bạn bắt và đổi thành `RuntimeException` hay không. Hãy để `RestClientResponseException`/`ResourceAccessException` lan tới handler, hoặc bọc exception của Feign/client khác bằng `DownstreamServiceException`, `GatewayTimeoutException` hay `CircuitBreakerOpenException`.

## License

Phát hành theo [Apache License 2.0](LICENSE).
