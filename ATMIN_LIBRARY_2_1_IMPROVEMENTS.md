# Kế hoạch hoàn thiện atmin-library 2.1 cho Spring MVC và WebFlux

## 1. Mục tiêu

Hoàn thiện `atmin-library:2.1.0` để một contract dùng chung có thể hoạt động an toàn trong cả hai mô hình:

- Monolith sử dụng Spring MVC/Servlet.
- Microservice sử dụng Spring WebFlux/reactive.
- API Gateway sử dụng Spring Cloud Gateway.
- Ứng dụng MVC gọi service khác bằng `RestClient`, `RestTemplate` hoặc `WebClient`.
- Ứng dụng WebFlux gọi service khác bằng `WebClient`.

Phiên bản 2.1 phải giữ tương thích ngược với contract 1.x/2.0:

- Không đổi cấu trúc cơ bản của `ApiResponse<T>`.
- Không đổi cấu trúc cơ bản của `ApiErrorResponse`.
- Không xóa các factory method hiện có.
- Không đổi package `atmin.common.*` đang được consumer sử dụng.
- Không bắt consumer MVC phải chuyển sang reactive.
- Không kéo Servlet stack vào ứng dụng WebFlux.
- Không kéo WebFlux server vào ứng dụng MVC chỉ vì consumer dùng `WebClient`.

## 2. Trạng thái hiện tại và khoảng trống

Bản 2.1 hiện đã có:

- `ApiResponse`, `ApiErrorResponse`, `PageInfo`.
- Exception handler cho Spring MVC.
- Security handler cho Servlet Security.
- Chuẩn hóa lỗi downstream từ `RestClient`, `RestTemplate` và `WebClient` trong ứng dụng MVC.
- Exception riêng cho HTTP 429, 502, 503 và 504.
- `Retry-After` cho rate limit.

Tuy nhiên, thư viện chưa thực sự hỗ trợ WebFlux server vì:

- Auto-configuration chỉ kích hoạt với `WebApplicationType.SERVLET`.
- Handler sử dụng `HttpServletRequest`.
- Security sử dụng `AuthenticationEntryPoint` và `AccessDeniedHandler` của Servlet.
- Chưa xử lý các exception đặc thù của WebFlux.
- Chưa có handler cho lỗi phát sinh trong filter chain của Spring Cloud Gateway.
- Trace ID chỉ ưu tiên MDC, không an toàn khi reactive chain đổi thread.
- Source MVC và WebFlux chưa được cô lập hoàn toàn.

`spring-webflux` đang có trong `compileOnly` mới chỉ phục vụ việc compile `WebClientExceptionHandler`; điều này không đồng nghĩa thư viện đã hỗ trợ WebFlux server.

## 3. Kiến trúc mục tiêu

### 3.1. Khuyến nghị tốt nhất: tách module

```text
atmin-library/
├── atmin-core/
├── atmin-webmvc/
├── atmin-webflux/
├── atmin-spring-boot-starter/
└── atmin-test-support/              # Tùy chọn
```

### 3.2. Trách nhiệm từng module

#### `atmin-core`

Không phụ thuộc Servlet hoặc WebFlux. Module này chứa:

```text
atmin.common.response
├── ApiResponse
├── ApiErrorResponse
└── PageInfo

atmin.common.exception
├── BadRequestException
├── UnauthorizedException
├── ForbiddenException
├── ResourceNotFoundException
├── DuplicateResourceException
├── ConflictException
├── RateLimitExceededException
├── DownstreamServiceException
├── ServiceUnavailableException
├── CircuitBreakerOpenException
├── GatewayTimeoutException
└── CloudStorageException
```

#### `atmin-webmvc`

Chứa implementation dành riêng cho Servlet:

- `HttpServletRequest`.
- `@RestControllerAdvice` cho Spring MVC.
- `AuthenticationEntryPoint`.
- `AccessDeniedHandler`.
- `MaxUploadSizeExceededException`.
- `RestClient` và `RestTemplate` exception mapping.
- `WebClient` exception mapping khi MVC dùng WebClient làm HTTP client.

#### `atmin-webflux`

Chứa implementation reactive:

- `ServerWebExchange`.
- `@RestControllerAdvice` cho WebFlux controller.
- `ServerAuthenticationEntryPoint`.
- `ServerAccessDeniedHandler`.
- WebFlux validation/input exception mapping.
- WebClient downstream exception mapping.
- Reactive trace propagation.
- Gateway `ErrorWebExceptionHandler` tùy chọn.

#### `atmin-spring-boot-starter`

Starter tự chọn adapter phù hợp dựa trên `WebApplicationType`. Có thể giữ artifact hiện tại làm facade tương thích ngược:

```groovy
implementation 'io.github.duongtran1702:atmin-library:2.1.0'
```

Ngoài ra nên cung cấp artifact tường minh:

```groovy
implementation 'io.github.duongtran1702:atmin-library-webmvc:2.1.0'
```

```groovy
implementation 'io.github.duongtran1702:atmin-library-webflux:2.1.0'
```

### 3.3. Nếu chưa tách multi-module trong 2.1

Nếu muốn giữ một artifact trong lần phát hành này, tối thiểu phải cô lập package và auto-configuration:

```text
atmin.common.config.mvc
atmin.common.config.webflux
atmin.common.exception.handler.mvc
atmin.common.exception.handler.webflux
atmin.common.security.mvc
atmin.common.security.webflux
```

Không để class cấu hình chung tham chiếu trực tiếp đồng thời tới cả Servlet và WebFlux types.

## 4. Auto-configuration

### 4.1. MVC auto-configuration

```java
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(ControllerAdvice.class)
@ConditionalOnProperty(
        prefix = "atmin.exceptions",
        name = "enabled",
        matchIfMissing = true
)
public class AtminMvcExceptionAutoConfiguration {
}
```

### 4.2. WebFlux auto-configuration

```java
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass({ServerWebExchange.class, ControllerAdvice.class})
@ConditionalOnProperty(
        prefix = "atmin.exceptions",
        name = "enabled",
        matchIfMissing = true
)
public class AtminWebFluxExceptionAutoConfiguration {
}
```

### 4.3. Đăng ký auto-configuration

Thêm riêng từng class vào:

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

### 4.4. Điều kiện bắt buộc

- Dùng `@ConditionalOnWebApplication` để MVC và WebFlux không cùng được đăng ký.
- Dùng `@ConditionalOnClass` cho dependency tùy chọn.
- Dùng `@ConditionalOnMissingBean` để application có thể override.
- Không dùng một config class chứa method signature tham chiếu tới class không chắc có trên classpath.
- Có test khi chỉ có MVC, chỉ có WebFlux và khi cả hai dependency cùng có mặt.

## 5. WebFlux Core Exception Handler

Tạo handler riêng, ví dụ:

```java
@RestControllerAdvice
@RequiredArgsConstructor
public class ReactiveCoreExceptionHandler {

    private final AtminExceptionProperties properties;

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleResourceNotFound(
            ResourceNotFoundException exception,
            ServerWebExchange exchange) {

        ApiErrorResponse response = ApiErrorResponse.notFound(
                exception.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
        );
    }
}
```

Các nhóm lỗi WebFlux cần hỗ trợ:

| Exception/nhóm lỗi | HTTP status |
|---|---:|
| `WebExchangeBindException` | 400 |
| `HandlerMethodValidationException` | 400 |
| `ServerWebInputException` | 400 |
| `MissingRequestValueException` | 400 |
| `DecodingException` | 400 |
| `UnauthorizedException` | 401 |
| `ForbiddenException` | 403 |
| `ResourceNotFoundException` | 404 |
| `NoResourceFoundException` | 404 |
| `MethodNotAllowedException` | 405 |
| `NotAcceptableStatusException` | 406 |
| `DuplicateResourceException` | 409 |
| `ConflictException` | 409 |
| `UnsupportedMediaTypeStatusException` | 415 |
| `RateLimitExceededException` | 429 |
| `DownstreamServiceException` | 502 hoặc policy đã cấu hình |
| `ServiceUnavailableException` | 503 |
| `CircuitBreakerOpenException` | 503 |
| `GatewayTimeoutException` | 504 |
| `ResponseStatusException` | Giữ nguyên status |
| Exception không xác định | 500 |

Quy tắc:

- Không trả message nội bộ của exception 500 ra client.
- Không log validation và lỗi 4xx thông thường ở mức `ERROR`.
- Log lỗi 5xx kèm stack trace và trace ID.
- Không gọi `block()` hoặc `subscribe()` trong handler.
- Không ghi response nếu `ServerHttpResponse.isCommitted()` đã trả `true`.

## 6. Reactive Security

WebFlux không sử dụng security handler Servlet. Cần bổ sung:

- `ServerAuthenticationEntryPoint` trả JSON 401.
- `ServerAccessDeniedHandler` trả JSON 403.
- Serialization qua Jackson 3 `ObjectMapper`.
- Ghi response bằng `ServerHttpResponse` và `DataBuffer`.
- Giữ nguyên contract `ApiErrorResponse`.

API consumer dự kiến:

```java
@Bean
SecurityWebFilterChain securityWebFilterChain(
        ServerHttpSecurity http,
        ServerAuthenticationEntryPoint authenticationEntryPoint,
        ServerAccessDeniedHandler accessDeniedHandler) {

    return http
            .exceptionHandling(errors -> errors
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
            .build();
}
```

Thư viện chỉ auto-register bean khi:

- Ứng dụng là reactive web application.
- Spring Security reactive có trên classpath.
- Consumer chưa khai báo bean cùng loại.
- `atmin.exceptions.security-enabled=true`.

## 7. Spring Cloud Gateway

Lỗi phát sinh trong Gateway filter chain có thể không đi qua controller advice. Cần cung cấp một `ErrorWebExceptionHandler` tùy chọn.

### Các lỗi cần chuẩn hóa

- JWT thiếu hoặc không hợp lệ: 401.
- JWT hợp lệ nhưng không đủ quyền: 403.
- Redis rate limiter từ chối: 429 và `Retry-After` khi có.
- Không tìm thấy route: 404.
- Downstream trả response không hợp lệ: 502.
- Không kết nối được downstream: 503.
- Circuit breaker mở: 503.
- Downstream timeout: 504.

### Điều kiện đăng ký

```java
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(ErrorWebExceptionHandler.class)
@ConditionalOnMissingBean(name = "atminGatewayErrorWebExceptionHandler")
```

Handler cần có order cao hơn error handler mặc định, nhưng không được ghi response hai lần.

Property đề xuất:

```yaml
atmin:
  exceptions:
    gateway-enabled: true
    gateway-order: -2
```

## 8. Downstream error mapping

### 8.1. Không ánh xạ mọi upstream 4xx thành 502

Policy mặc định đề xuất:

| Downstream result | Public result |
|---|---:|
| 400 | 400, body được sanitize |
| 401 | 502 mặc định; có thể cấu hình giữ 401 |
| 403 | 502 mặc định; có thể cấu hình giữ 403 |
| 404 | 404 nếu là domain lookup, nếu không thì 502 |
| 409 | 409 |
| 422 | 422 |
| 429 | 429 và giữ `Retry-After` hợp lệ |
| 500–599 không xác định | 502 |
| Connection/DNS/TLS failure | 503 |
| Timeout | 504 |

Không chuyển tiếp nguyên body, stack trace, hostname nội bộ hoặc token từ downstream.

### 8.2. Property đề xuất

```yaml
atmin:
  exceptions:
    microservice-enabled: true
    downstream-preserve-client-errors: true
    downstream-sanitize-body: true
    downstream-expose-service-name: false
```

### 8.3. WebClient

Hỗ trợ trong cả MVC và WebFlux:

- `WebClientResponseException`.
- `WebClientRequestException`.
- Timeout trong cause chain.
- `ConnectException`.
- `UnknownHostException`.
- `NoRouteToHostException`.
- TLS/SSL handshake failure được sanitize thành 503.

Không bắt buộc consumer phải dùng `onStatus()` nếu muốn exception tự lan tới handler. Nếu consumer tự map lỗi thì dùng các custom exception của thư viện.

## 9. Trace ID trong Servlet và Reactive

### 9.1. Tạo abstraction

```java
public interface TraceIdResolver {
    String resolve();
}
```

Có thể thêm overload dành cho web context:

```java
String resolve(HttpServletRequest request);

String resolve(ServerWebExchange exchange);
```

Hoặc tách thành hai implementation để module core không phụ thuộc web stack.

### 9.2. MVC resolution order

1. MDC theo key cấu hình.
2. Request header `X-Trace-Id` hoặc `X-Request-Id`.
3. Sinh UUID mới.

### 9.3. WebFlux resolution order

1. Reactor Context.
2. Micrometer Observation/Tracing context nếu có.
3. Header `X-Trace-Id` hoặc `X-Request-Id`.
4. MDC nếu vẫn tồn tại.
5. Sinh UUID mới.

Response nên trả lại:

```http
X-Trace-Id: <trace-id>
```

API static `ApiErrorResponse.setMdcKey(...)` nên được giữ để tương thích nhưng đánh dấu `@Deprecated`, khuyến nghị dùng configuration property.

## 10. Error contract nâng cao

Giữ nguyên các field cũ và chỉ bổ sung field nullable:

```json
{
  "timestamp": "2026-09-15T05:30:00Z",
  "status": 503,
  "error": "Service Unavailable",
  "code": "DOWNSTREAM_UNAVAILABLE",
  "message": "Dịch vụ phụ thuộc tạm thời không khả dụng",
  "path": "/api/v1/chat/conversations",
  "traceId": "...",
  "service": "gateway",
  "retryable": true
}
```

### Thay đổi đề xuất

- Dùng `Instant` theo UTC thay vì `LocalDateTime` cho response mới.
- Thêm `code` ổn định để client không phải phân tích message.
- Thêm `retryable` để client quyết định retry.
- Thêm `service` nhưng mặc định không tiết lộ downstream hostname.
- Đánh dấu field mới `NON_NULL` để không phá JSON contract cũ.

Enum đề xuất:

```java
public enum AtminErrorCode {
    VALIDATION_FAILED,
    MALFORMED_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    RESOURCE_NOT_FOUND,
    CONFLICT,
    RATE_LIMIT_EXCEEDED,
    DOWNSTREAM_BAD_RESPONSE,
    DOWNSTREAM_UNAVAILABLE,
    DOWNSTREAM_TIMEOUT,
    CIRCUIT_BREAKER_OPEN,
    INTERNAL_ERROR
}
```

Không dùng enum name làm message hiển thị cho người dùng.

## 11. Pagination cho MVC, JPA và R2DBC

Giữ các API hiện tại:

```java
ApiResponse.pagePaginated(message, page);
ApiResponse.slicePaginated(message, slice);
ApiResponse.paginated(message, content, PageInfo.of(page, size, total));
```

Bổ sung helper không phụ thuộc JPA cho reactive service:

```java
Mono<ApiResponse<List<T>>> pagePaginated(
        String message,
        Flux<T> content,
        Mono<Long> totalElements,
        int page,
        int size
);
```

```java
Mono<ApiResponse<List<T>>> slicePaginated(
        String message,
        Flux<T> content,
        int page,
        int size
);
```

Với slice reactive, lấy `size + 1` phần tử để xác định `hasNext`, sau đó chỉ trả tối đa `size` phần tử.

Nên cân nhắc cursor pagination cho chat/feed:

```json
{
  "page": {
    "pageSize": 20,
    "nextCursor": "opaque-cursor",
    "hasNext": true
  }
}
```

Cursor phải là opaque string; client không nên phụ thuộc cấu trúc bên trong.

## 12. Upload và giới hạn payload

MVC và WebFlux có exception khác nhau:

### MVC

- `MaxUploadSizeExceededException`.
- `MissingServletRequestPartException`.

### WebFlux

- `DataBufferLimitException`.
- Multipart decoding/input exception.
- Storage publisher error.

Không giữ `DataBuffer` sau khi request kết thúc. Không gom file lớn vào memory. Library chỉ nên chuẩn hóa lỗi; streaming/storage implementation vẫn thuộc application.

## 13. Bean Validation

MVC tiếp tục xử lý:

- `MethodArgumentNotValidException`.
- `HandlerMethodValidationException`.
- `ConstraintViolationException`.

WebFlux xử lý:

- `WebExchangeBindException`.
- `HandlerMethodValidationException`.
- `ConstraintViolationException`.

Validation errors cần:

- Giữ thứ tự ổn định bằng `LinkedHashMap`.
- Không làm mất nested path, ví dụ `items[0].name`.
- Có policy rõ ràng nếu một field có nhiều lỗi.
- Không trả rejected value nếu có thể chứa password, token hoặc secret.

## 14. Messaging: Kafka/RabbitMQ

`@ControllerAdvice` không xử lý exception từ Kafka/RabbitMQ consumer. README phải giữ cảnh báo này.

Có thể thêm module tùy chọn `atmin-messaging` chứa contract, không tự áp đặt broker configuration:

```java
public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String traceId,
        String source,
        T data
) {
}
```

Thư viện có thể cung cấp:

- Event envelope.
- Retry classification.
- Dead-letter metadata.
- Trace propagation helper.
- Idempotency key helper.

Không nên:

- Tự tạo topic cố định.
- Tự chọn retry count cho mọi service.
- Tự gọi `subscribe()`.
- Nuốt exception khiến broker không retry/DLT.

## 15. Configuration properties đề xuất

Giữ các property 2.1 hiện tại và bổ sung dần:

```yaml
atmin:
  exceptions:
    enabled: true
    security-enabled: true
    security-filter-enabled: true
    storage-enabled: true
    microservice-enabled: true
    gateway-enabled: true

    trace-id-mdc-key: traceId
    trace-id-header: X-Trace-Id
    echo-trace-id-header: true

    downstream-preserve-client-errors: true
    downstream-sanitize-body: true
    downstream-expose-service-name: false

    validation-failed: Dữ liệu đầu vào không hợp lệ
    malformed-request: Request body không hợp lệ
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

Mọi property phải có configuration metadata để IDE không báo unresolved property.

## 16. README cần viết lại

### 16.1. Sửa version sai

Thay toàn bộ câu còn ghi “2.0” thành “2.1” khi mô tả tính năng mới của bản hiện tại, ngoại trừ phần lịch sử nâng cấp.

Hiện cần sửa ít nhất:

- “Không dùng bản 2.0 này...” → “Không dùng bản 2.1 này...”.
- “2.0 nhận mọi `HttpStatusCode`” → “2.1 nhận mọi `HttpStatusCode`”, hoặc ghi rõ tính năng có từ 2.0.
- “Bản 2.0 chuẩn hóa...” → “Bản 2.1 chuẩn hóa...”.
- Các tiêu đề/nội dung Spring Security còn ghi 2.0.

### 16.2. Tách hướng dẫn cài đặt

#### Spring MVC

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    implementation 'io.github.duongtran1702:atmin-library-webmvc:2.1.0'
}
```

#### Spring WebFlux

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'io.github.duongtran1702:atmin-library-webflux:2.1.0'
}
```

#### Starter tự nhận diện

```groovy
dependencies {
    implementation 'io.github.duongtran1702:atmin-library:2.1.0'
}
```

README phải nói rõ starter không tự thêm MVC hoặc WebFlux; consumer vẫn chọn web stack của mình.

### 16.3. Thêm capability matrix

| Tính năng | MVC | WebFlux |
|---|---:|---:|
| `ApiResponse` | Có | Có |
| `ApiErrorResponse` | Có | Có |
| Bean Validation | Có | Có |
| Security 401/403 JSON | Có | Có |
| JJWT exception | Có | Có |
| Downstream HTTP errors | Có | Có |
| Upload limit | Có | Có |
| Trace propagation | MDC/request | Reactor Context/request |
| Gateway filter errors | Không áp dụng | Có |
| Page/Slice | Có | Có |
| Reactive pagination helper | Không cần | Có |

### 16.4. Thêm ví dụ riêng

README cần có ví dụ độc lập cho:

- MVC controller.
- WebFlux controller trả `Mono<ApiResponse<T>>`.
- MVC `SecurityFilterChain`.
- WebFlux `SecurityWebFilterChain`.
- MVC gọi downstream bằng `RestClient`.
- WebFlux gọi downstream bằng `WebClient`.
- Gateway error response.
- R2DBC pagination.
- Trace ID xuyên qua nhiều service.

## 17. Test matrix bắt buộc

### 17.1. Core

- Tất cả factory method cũ.
- Field nullable không xuất hiện trong JSON.
- Status và message chính xác.
- `PageInfo` từ chối metadata không hợp lệ.
- Error code mới không phá JSON cũ.

### 17.2. MVC với MockMvc

- Validation request body.
- Validation request parameter.
- Malformed JSON.
- Missing parameter/part.
- 401/403 ở controller.
- 401/403 từ security filter.
- 404/405/406/415.
- Upload vượt giới hạn.
- Downstream 4xx/5xx.
- Connection failure và timeout.
- Custom handler back-off.

### 17.3. WebFlux với WebTestClient

- `WebExchangeBindException`.
- `ServerWebInputException`.
- Malformed JSON.
- Missing request value.
- 401/403 ở controller.
- 401/403 từ reactive security filter.
- 404/405/406/415.
- `DataBufferLimitException`.
- WebClient 4xx/5xx.
- Connection failure và timeout.
- Reactor Context trace ID.
- Response đã committed.
- Custom reactive handler back-off.

### 17.4. Gateway

- Route không tồn tại.
- Rate limit 429.
- Downstream unavailable 503.
- Circuit breaker open 503.
- Timeout 504.
- Error response luôn là JSON contract của thư viện.

### 17.5. Classpath compatibility

- Chỉ có `spring-webmvc`.
- Chỉ có `spring-webflux`.
- Không có web stack.
- MVC có `WebClient` trên classpath.
- Cả MVC và WebFlux cùng có trên classpath.
- Có/không Spring Security.
- Có/không JJWT.
- Có/không Bean Validation.
- Có/không Spring Data Commons.

### 17.6. Quality gates

```powershell
.\gradlew.bat clean check
```

`check` phải bao gồm:

- Unit tests.
- MVC integration tests.
- WebFlux integration tests.
- Javadoc.
- Compiler warnings (`-Xlint:deprecation`, `-Xlint:unchecked`).
- Kiểm tra auto-configuration metadata.

## 18. Migration và backward compatibility

### Giữ nguyên

- Package `atmin.common.response`.
- Package `atmin.common.exception`.
- Constructor exception cũ.
- Factory method của `ApiResponse` và `PageInfo`.
- JSON field hiện có.
- Property 2.0/2.1 hiện có.
- `@EnableAtminExceptionHandling` trong Servlet mode.

### Có thể bổ sung

- Error `code`.
- `retryable`.
- `service`.
- Reactive handler.
- Gateway handler.
- Reactive security beans.
- Reactive pagination helper.

### Không nên làm trong minor version

- Xóa constructor cũ.
- Đổi package.
- Đổi tên JSON field.
- Đổi status mapping đang được consumer dựa vào mà không có compatibility property.
- Buộc tất cả application phải dùng WebFlux.
- Biến mọi downstream 4xx thành 502 mà không có migration note.

## 19. Checklist trước khi push/publish 2.1.0

### Kiến trúc

- [ ] Core không phụ thuộc Servlet/WebFlux implementation.
- [ ] MVC và WebFlux handler được tách riêng.
- [ ] Auto-configuration nhận diện đúng web application type.
- [ ] Không đăng ký đồng thời MVC và WebFlux advice.
- [ ] Consumer có thể override từng handler bằng bean riêng.

### MVC

- [ ] Các test cũ vẫn chạy.
- [ ] Servlet security vẫn trả `ApiErrorResponse`.
- [ ] RestClient/RestTemplate/WebClient error mapping hoạt động.

### WebFlux

- [ ] Controller advice reactive hoạt động.
- [ ] Reactive validation trả field errors.
- [ ] Reactive security trả JSON 401/403.
- [ ] WebClient timeout/transport/status error được phân loại đúng.
- [ ] Không có `block()` hoặc `subscribe()` trong production path.

### Gateway

- [ ] Filter-chain errors được chuẩn hóa.
- [ ] Rate limit có `Retry-After` khi phù hợp.
- [ ] Handler không ghi response sau khi committed.

### Contract và bảo mật

- [ ] Không lộ upstream body mặc định.
- [ ] Không lộ token, hostname nội bộ hoặc stack trace.
- [ ] Error code ổn định và có tài liệu.
- [ ] Trace ID xuất hiện trong body và response header.
- [ ] Timestamp có timezone rõ ràng, ưu tiên UTC.

### Documentation

- [ ] README không còn mô tả sai rằng 2.1 chỉ hỗ trợ Servlet.
- [ ] Tất cả tham chiếu version được kiểm tra lại.
- [ ] Có hướng dẫn MVC và WebFlux riêng.
- [ ] Có capability matrix.
- [ ] Có migration guide từ 2.0 lên 2.1.
- [ ] Có ví dụ Gateway và R2DBC.

### Release

- [ ] `clean check` thành công.
- [ ] Sources JAR và Javadoc JAR được tạo.
- [ ] POM không kéo web stack không cần thiết vào consumer.
- [ ] Test thử bằng một MVC sample app.
- [ ] Test thử bằng một WebFlux sample app.
- [ ] Test thử bằng một Spring Cloud Gateway sample app.
- [ ] Changelog 2.1.0 hoàn chỉnh.
- [ ] Không có secret hoặc credential trong Git.

## 20. Thứ tự triển khai khuyến nghị

1. Tách response và exception thuần Java/Spring HTTP vào core.
2. Chuyển toàn bộ handler hiện tại vào adapter MVC mà không đổi hành vi.
3. Tạo auto-configuration WebFlux.
4. Viết WebFlux core exception handler.
5. Viết reactive security entry point và access denied handler.
6. Viết WebClient error mapping cho reactive application.
7. Viết Gateway error handler.
8. Bổ sung trace resolver cho Reactor Context.
9. Bổ sung error code và downstream mapping policy.
10. Bổ sung reactive/cursor pagination helper nếu API đã ổn định.
11. Hoàn thiện test matrix.
12. Cập nhật README, changelog và migration guide.
13. Test bằng ba sample: MVC, WebFlux và Gateway.
14. Chỉ publish sau khi toàn bộ quality gate thành công.

## 21. Kết luận

Hướng tốt nhất cho `atmin-library:2.1.0` trước khi phát hành là giữ một contract chung nhưng tách implementation theo adapter:

```text
Core contract
├── MVC adapter
└── WebFlux adapter
    └── Gateway integration tùy chọn
```

Cách này giúp:

- Monolith MVC tiếp tục hoạt động mà không phải migration lớn.
- Microservice WebFlux có exception/security handling đúng reactive stack.
- Gateway trả cùng error contract với downstream services.
- Tránh kéo nhầm Servlet vào WebFlux hoặc ngược lại.
- Dễ mở rộng thêm messaging, tracing và storage adapter sau này.

Nếu phạm vi 2.1 quá lớn, ưu tiên bắt buộc trước khi publish gồm: WebFlux core handler, reactive security, auto-configuration tách biệt, trace ID reactive và test classpath. Gateway integration, cursor pagination và messaging support có thể chuyển sang 2.2 mà không ảnh hưởng contract cốt lõi.
