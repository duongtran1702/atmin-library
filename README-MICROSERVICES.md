# atmin-library 2.1 cho Microservice, WebFlux và Gateway

Tài liệu này dành cho ứng dụng reactive, microservice và API Gateway. Nếu dự án là monolithic Spring MVC, xem [README chính](README.md).

`atmin-library:2.1.0` dùng chung một contract cho cả MVC và WebFlux nhưng tách implementation theo web application type. Consumer vẫn dùng đúng artifact cũ; không có artifact giả định như `atmin-library-webflux`.

## Mục lục

- [Khả năng](#khả-năng)
- [Cài đặt WebFlux](#cài-đặt-webflux)
- [Cách auto-configuration lựa chọn adapter](#cách-auto-configuration-lựa-chọn-adapter)
- [Response reactive](#response-reactive)
- [Exception WebFlux](#exception-webflux)
- [Reactive Security](#reactive-security)
- [Gọi downstream bằng WebClient](#gọi-downstream-bằng-webclient)
- [Spring Cloud Gateway](#spring-cloud-gateway)
- [Trace ID trong reactive chain](#trace-id-trong-reactive-chain)
- [Cấu hình](#cấu-hình)
- [Messaging](#messaging)
- [Giới hạn và lưu ý](#giới-hạn-và-lưu-ý)

## Khả năng

| Tính năng | WebFlux service | Spring Cloud Gateway |
|---|:---:|:---:|
| `ApiResponse<T>` | Có | Có thể dùng |
| `ApiErrorResponse` | Có | Có |
| Validation request body/method | Có | Không áp dụng |
| Custom exception 400–504 | Có | Có |
| WebClient response/transport errors | Có | Có |
| Reactive Security 401/403 JSON | Có | Có |
| Reactor Context trace ID | Có | Có |
| `X-Trace-Id` response header | Có | Có |
| Lỗi trong controller | Controller advice | Controller advice |
| Lỗi trong gateway filter chain | Không áp dụng | `ErrorWebExceptionHandler` |
| Thay handler mặc định | Bean cùng type | Tắt Atmin Gateway handler trước |

Không có `block()` hoặc `subscribe()` trong production handler của thư viện.

## Cài đặt WebFlux

### Gradle Groovy

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'io.github.duongtran1702:atmin-library:2.1.0'
}
```

### Gradle Kotlin

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("io.github.duongtran1702:atmin-library:2.1.0")
}
```

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    <dependency>
        <groupId>io.github.duongtran1702</groupId>
        <artifactId>atmin-library</artifactId>
        <version>2.1.0</version>
    </dependency>
</dependencies>
```

Thư viện không tự quyết định Netty, Tomcat hoặc Jetty và không tự thêm Spring Cloud Gateway, R2DBC, Kafka hay RabbitMQ.

## Cách auto-configuration lựa chọn adapter

Hai auto-configuration được đăng ký độc lập:

- `AtminExceptionAutoConfiguration`: chỉ chạy khi application type là `SERVLET`.
- `AtminReactiveExceptionAutoConfiguration`: chỉ chạy khi application type là `REACTIVE`.

Nếu ứng dụng MVC thêm WebFlux chỉ để dùng `WebClient`, Spring Boot mặc định vẫn chọn MVC. Khi đó handler Servlet tiếp tục hoạt động và chỉ integration WebClient client-side được thêm.

Không cần annotation trong chế độ auto-configuration:

```java
@SpringBootApplication
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

`@EnableAtminExceptionHandling` được giữ cho tương thích nhưng chỉ dành cho manual mode Servlet/MVC.

## Response reactive

Các factory method cũ dùng trực tiếp bên trong `Mono` hoặc `Flux`:

```java
@RestController
@RequestMapping("/api/orders")
class OrderController {

    private final OrderService orderService;

    @GetMapping("/{id}")
    Mono<ApiResponse<OrderDto>> findById(@PathVariable UUID id) {
        return orderService.findById(id)
                .map(order -> ApiResponse.success("Order found", order));
    }
}
```

Không cần tạo một loại response khác cho reactive. `ApiResponse`, `ApiErrorResponse` và `PageInfo` vẫn giữ nguyên package và JSON field.

`ApiResponse` không tự biến `Flux<T>` thành JSON streaming. Nếu endpoint cần NDJSON hoặc Server-Sent Events, application phải khai báo media type và contract streaming riêng. Ví dụ phân trang dưới đây chủ động gom đúng một page vào `List<T>`; kích thước page và giới hạn bộ nhớ vẫn do application kiểm soát.

Ví dụ phân trang R2DBC không phụ thuộc JPA:

```java
Mono<ApiResponse<List<OrderDto>>> findPage(int page, int size) {
    Mono<List<OrderDto>> content = repository.findPage(size, (long) page * size)
            .map(mapper::toDto)
            .collectList();
    Mono<Long> total = repository.count();

    return Mono.zip(content, total)
            .map(result -> ApiResponse.paginated(
                    "Orders loaded",
                    result.getT1(),
                    PageInfo.of(page, size, result.getT2())
            ));
}
```

Thư viện không tự gọi `collectList()` vì giới hạn memory và pagination policy thuộc về application.

## Exception WebFlux

Reactive controller advice xử lý:

| Exception/nhóm | Status |
|---|---:|
| `WebExchangeBindException` | 400 |
| `HandlerMethodValidationException` | 400 |
| `ServerWebInputException` và lỗi decode/input | 400 |
| `BadRequestException` | 400 |
| `UnauthorizedException` | 401 |
| `ForbiddenException` | 403 |
| `ResourceNotFoundException` | 404 |
| `ResponseStatusException` | Giữ status gốc |
| `DuplicateResourceException`, `ConflictException` | 409 |
| `RateLimitExceededException` | 429, có thể có `Retry-After` |
| `DownstreamServiceException` | 502 |
| `ServiceUnavailableException`, `CircuitBreakerOpenException` | 503 |
| `GatewayTimeoutException` | 504 |
| Exception không xác định | 500 với message an toàn |

Ví dụ:

```java
return repository.findById(id)
        .switchIfEmpty(Mono.error(
                new ResourceNotFoundException("Order", "id", id)
        ));
```

Validation errors giữ nested property path, dùng thứ tự ổn định và không trả rejected value.

## Reactive Security

Khi Reactive Spring Security và Jackson 3 có mặt, thư viện tạo:

- `ServerAuthenticationEntryPoint` trả 401.
- `ServerAccessDeniedHandler` trả 403.

Gắn chúng vào filter chain:

```java
@Configuration
@EnableWebFluxSecurity
class ReactiveSecurityConfiguration {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ServerAuthenticationEntryPoint authenticationEntryPoint,
            ServerAccessDeniedHandler accessDeniedHandler) {

        return http
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/public/**").permitAll()
                        .anyExchange().authenticated()
                )
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .build();
    }
}
```

Nếu application tự khai báo bean cùng type, thư viện nhường quyền bằng `@ConditionalOnMissingBean`.

JWT exception ở controller được sanitize thành message `security-unauthorized`. Không trả token, signature error hoặc parser detail ra client.

## Gọi downstream bằng WebClient

```java
Mono<InventoryDto> inventory(UUID productId) {
    return webClient.get()
            .uri("http://inventory-service/api/inventory/{id}", productId)
            .retrieve()
            .bodyToMono(InventoryDto.class);
}
```

Nếu exception được để lan qua reactive chain, thư viện phân loại:

| Lỗi | Public status |
|---|---:|
| Upstream 400/404/409/422/429 | Giữ status; 429 giữ `Retry-After` dạng số |
| Upstream status khác | 502 |
| `UnknownHostException`, DNS/service name | 503 |
| `ConnectException`, `NoRouteToHostException` | 503 |
| `SSLException`, TLS handshake/certificate | 503 |
| `WebClientRequestException` transport | 503 |
| `SocketTimeoutException`, `HttpTimeoutException`, timeout cause | 504 |

Body, hostname và thông tin hạ tầng downstream không được chuyển tiếp trong mapping tự động.

Chế độ `downstream-preserve-client-errors=true` giữ downstream 404 theo mặc định. Nếu 404 thể hiện route/deployment downstream cấu hình sai thay vì một resource nghiệp vụ không tồn tại, application phải chủ động chuyển lỗi đó thành `DownstreamServiceException` để public response là 502.

Với OpenFeign hoặc client khác, bọc lỗi tại boundary:

```java
try {
    return paymentClient.pay(request);
}
catch (FeignException exception) {
    throw new DownstreamServiceException(
            "payment-service",
            exception.status(),
            exception
    );
}
```

Fallback circuit breaker không phụ thuộc vendor:

```java
Mono<PaymentResult> fallback(PaymentRequest request, Throwable cause) {
    return Mono.error(CircuitBreakerOpenException.forService(
            "payment-service", cause));
}
```

## Spring Cloud Gateway

Khi Spring Cloud Gateway có trên classpath và application type là reactive, thư viện tự đăng ký `atminGatewayErrorWebExceptionHandler`.

Handler này dành cho lỗi phát sinh trong gateway filter chain trước controller:

- rate limit → 429 và `Retry-After`;
- downstream 400/404/409/422/429 → giữ status; status khác → 502;
- DNS/connection/service discovery failure → 503;
- circuit breaker mở → 503;
- timeout → 504;
- `ResponseStatusException` → giữ status;
- response đã committed → không ghi lần hai và chuyển lỗi cho handler tiếp theo.

Handler chỉ chuẩn hóa các nhóm lỗi Gateway mà thư viện nhận diện. Exception không nhận diện được được chuyển cho error handler tiếp theo của application.

Thứ tự mặc định là `-2`, có thể đổi:

```yaml
atmin:
  exceptions:
    gateway-enabled: true
    gateway-order: -2
```

Thư viện không tự tạo route, Redis rate limiter, circuit breaker hoặc retry policy.

### Dùng Gateway error handler riêng

Atmin không dò mọi bean `ErrorWebExceptionHandler` để quyết định back-off, vì một Gateway application có thể hợp lệ khi có nhiều handler với order và phạm vi khác nhau. Nếu application muốn thay thế handler Gateway của Atmin bằng implementation riêng, hãy vô hiệu hóa handler Atmin một cách tường minh:

```yaml
atmin:
  exceptions:
    gateway-enabled: false
```

Sau đó application có thể đăng ký custom `ErrorWebExceptionHandler` và tự chọn `Ordered` phù hợp. Property này chỉ tắt `AtminGatewayErrorWebExceptionHandler`; WebFlux controller advice, reactive security handler và trace filter vẫn hoạt động.

## Trace ID trong reactive chain

`AtminReactiveTraceFilter` chạy trước security filter và xử lý trace ID theo thứ tự:

1. Header cấu hình, mặc định `X-Trace-Id`.
2. MDC nếu còn giá trị hợp lệ.
3. UUID mới.

Trace ID được:

- kiểm tra bằng safe pattern và giới hạn 128 ký tự;
- lưu trong exchange attribute;
- đưa vào Reactor Context với key `atmin.traceId`;
- trả trong body `ApiErrorResponse`;
- echo qua response header `X-Trace-Id` mặc định.

Đọc trong reactive service:

```java
Mono<String> currentTraceId() {
    return Mono.deferContextual(context ->
            Mono.just(context.getOrDefault("atmin.traceId", "unknown"))
    );
}
```

Khi gọi downstream, có thể truyền tiếp:

```java
Mono<Result> callDownstream() {
    return Mono.deferContextual(context -> webClient.get()
            .uri("http://service-b/api/data")
            .header("X-Trace-Id", context.getOrDefault("atmin.traceId", ""))
            .retrieve()
            .bodyToMono(Result.class));
}
```

## Cấu hình

```yaml
atmin:
  exceptions:
    enabled: true
    security-enabled: true
    security-filter-enabled: true
    microservice-enabled: true
    downstream-preserve-client-errors: true
    downstream-expose-service-name: false
    gateway-enabled: true
    gateway-order: -2

    trace-id-mdc-key: traceId
    trace-id-header: X-Trace-Id
    echo-trace-id-header: true

    validation-failed: Dữ liệu đầu vào không hợp lệ
    malformed-request: Request body không hợp lệ
    downstream-service-error: Dịch vụ phụ thuộc trả về lỗi
    downstream-service-unavailable: Dịch vụ phụ thuộc tạm thời không khả dụng
    downstream-timeout: Dịch vụ phụ thuộc phản hồi quá chậm
    unexpected-error: Hệ thống đang gặp sự cố
    security-unauthorized: Vui lòng đăng nhập
    security-access-denied: Bạn không có quyền truy cập
```

Tắt Gateway handler để dùng custom `ErrorWebExceptionHandler`, đồng thời vẫn giữ WebFlux controller advice:

```properties
atmin.exceptions.gateway-enabled=false
```

Tắt toàn bộ:

```properties
atmin.exceptions.enabled=false
```

## Messaging

Exception trong Kafka/RabbitMQ consumer không đi qua WebFlux hoặc MVC controller advice. Thư viện không nuốt exception và không tự áp đặt retry count.

Ứng dụng cần dùng error handler của broker/framework để cấu hình:

- retry và backoff;
- dead-letter topic/queue;
- idempotency;
- trace ID trong event headers;
- phân loại lỗi retryable và non-retryable.

## Giới hạn và lưu ý

- Bản 2.1 chỉ hỗ trợ Spring Boot 4.x, Java 21 và package `jakarta.*`.
- Không thêm đồng thời cả MVC và WebFlux starter trừ khi MVC cần `WebClient`; Spring Boot mặc định ưu tiên MVC khi cả hai có mặt.
- Reactive handler không gọi blocking database/storage API thay cho application.
- Gateway integration chỉ được tạo khi Spring Cloud Gateway thực sự có mặt.
- `ApiErrorResponse` vẫn dùng timestamp cũ để giữ JSON contract; chưa đổi sang `Instant` trong minor release.
- `code`, `retryable`, `service` là field tùy chọn chỉ có trên các mapping mới. Factory và exception cũ để chúng là `null`, vì vậy Jackson không serialize và payload cũ không đổi.
- `service` mặc định bị ẩn. Chỉ bật `downstream-expose-service-name=true` khi đó là logical name an toàn do ứng dụng chủ động cung cấp; thư viện không tự đưa hostname vào response.
- Multi-module và messaging contract được để cho phiên bản sau vì có tác động lớn hơn đến cấu trúc artifact.

## Kiểm thử thư viện

```powershell
.\gradlew.bat clean check
```

Test matrix bao gồm MVC, WebFlux, auto-configuration, downstream client, trace propagation, reactive validation/security registration và Gateway handler độc lập.

## Tài liệu liên quan

- [README monolithic/MVC và API chung](README.md)
- [Exception reference](docs/exceptions.md)
- [Handler reference](docs/handlers.md)
- [Publishing guide](docs/publishing.md)
