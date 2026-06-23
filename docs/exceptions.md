# 📋 Exception Classes Documentation

Thư viện cung cấp các exception phổ biến, sẵn sàng sử dụng cho mọi project Spring Boot.
Tất cả exception đều extends `RuntimeException` và có annotation `@ResponseStatus` tương ứng. Toàn bộ các phản hồi lỗi từ những exception này đều được tự động đính kèm mã truy vết **`traceId`** phục vụ cho việc gỡ lỗi (debugging).

## Tổng quan

| Exception | HTTP Status | Mô tả |
|---|---|---|
| `BadRequestException` | 400 Bad Request | Request không hợp lệ |
| `UnauthorizedException` | 401 Unauthorized | Chưa xác thực / token hết hạn |
| `ForbiddenException` | 403 Forbidden | Không có quyền truy cập |
| `ResourceNotFoundException` | 404 Not Found | Tài nguyên không tồn tại |
| `DuplicateResourceException` | 409 Conflict | Dữ liệu đã tồn tại (trùng lặp) |
| `ConflictException` | 409 Conflict | Xung đột dữ liệu |
| `CloudStorageException` | 503 Service Unavailable | Lỗi cloud storage (S3, GCS...) |
| `ServiceUnavailableException` | 503 Service Unavailable | Service bên ngoài không khả dụng |

## Import

```java
import atmin.common.exception.*;
```

---

## Chi tiết từng Exception

### BadRequestException (400)

Dùng khi request từ client không hợp lệ.

```java
// Ví dụ: validate dữ liệu đầu vào
if (email == null || email.isEmpty()) {
    throw new BadRequestException("Email không được để trống");
}

// Ví dụ: format dữ liệu sai
if (!email.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")) {
    throw new BadRequestException("Email format is invalid");
}

// Với cause (exception gốc)
try {
    Integer.parseInt(input);
} catch (NumberFormatException e) {
    throw new BadRequestException("Giá trị phải là số nguyên", e);
}
```

**JSON Response:**
```json
{
    "timestamp": "2026-06-19T14:30:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Email không được để trống",
    "path": "/api/users",
    "traceId": "fa7a4e6b-a25f-4ce9-8973-2e06cbe19e7a"
}
```

---

### UnauthorizedException (401)

Dùng khi người dùng chưa đăng nhập hoặc token không hợp lệ.

```java
// Ví dụ: token hết hạn
throw new UnauthorizedException("Token has expired. Please login again.");

// Ví dụ: thiếu header Authorization
if (authHeader == null) {
    throw new UnauthorizedException("Missing Authorization header");
}

// Với cause
throw new UnauthorizedException("Invalid credentials", originalException);
```

**JSON Response:**
```json
{
    "timestamp": "2026-06-19T14:30:00",
    "status": 401,
    "error": "Unauthorized",
    "message": "Token has expired. Please login again.",
    "path": "/api/profile",
    "traceId": "fa7a4e6b-a25f-4ce9-8973-2e06cbe19e7a"
}
```

---

### ForbiddenException (403)

Dùng khi người dùng đã đăng nhập nhưng không có quyền.

```java
// Ví dụ: phân quyền
if (!currentUser.getRole().equals("ADMIN")) {
    throw new ForbiddenException("Only administrators can perform this action");
}

// Ví dụ: truy cập tài nguyên của người khác
if (!order.getUserId().equals(currentUserId)) {
    throw new ForbiddenException("You do not have permission to view this order");
}
```

**JSON Response:**
```json
{
    "timestamp": "2026-06-19T14:30:00",
    "status": 403,
    "error": "Forbidden",
    "message": "Only administrators can perform this action",
    "path": "/api/admin/users",
    "traceId": "fa7a4e6b-a25f-4ce9-8973-2e06cbe19e7a"
}
```

---

### ResourceNotFoundException (404)

Dùng khi tìm kiếm tài nguyên nhưng không tồn tại. Hỗ trợ 3 kiểu constructor.

```java
// Cách 1: Message đơn giản
throw new ResourceNotFoundException("User not found");

// Cách 2: Chi tiết với resource name, field, value (KHUYÊN DÙNG)
throw new ResourceNotFoundException("User", "id", 123);
// → Message tự động: "User not found with id: '123'"

throw new ResourceNotFoundException("Product", "sku", "ABC-001");
// → Message tự động: "Product not found with sku: 'ABC-001'"

// Cách 3: Với cause
throw new ResourceNotFoundException("Database connection lost", dbException);

// Dùng phổ biến nhất: trong Service layer với Optional
User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
```

**Getter methods (chỉ có khi dùng constructor 3 tham số):**

```java
try { ... }
catch (ResourceNotFoundException ex) {
    ex.getResourceName();  // "User"
    ex.getFieldName();     // "id"
    ex.getFieldValue();    // 123
}
```

**JSON Response:**
```json
{
    "timestamp": "2026-06-19T14:30:00",
    "status": 404,
    "error": "Not Found",
    "message": "User not found with id: '123'",
    "path": "/api/users/123",
    "traceId": "fa7a4e6b-a25f-4ce9-8973-2e06cbe19e7a"
}
```

---

### DuplicateResourceException (409)

Dùng khi tạo tài nguyên bị trùng (unique constraint).

```java
// Ví dụ: email đã tồn tại
if (userRepository.existsByEmail(email)) {
    throw new DuplicateResourceException("User with email '" + email + "' already exists");
}

// Ví dụ: mã sản phẩm trùng
if (productRepository.existsBySku(sku)) {
    throw new DuplicateResourceException("Product with SKU '" + sku + "' already exists");
}
```

**JSON Response:**
```json
{
    "timestamp": "2026-06-19T14:30:00",
    "status": 409,
    "error": "Conflict",
    "message": "User with email 'test@domain.com' already exists",
    "path": "/api/users",
    "traceId": "fa7a4e6b-a25f-4ce9-8973-2e06cbe19e7a"
}
```

---

### ConflictException (409)

Tương tự `DuplicateResourceException`, dùng cho các xung đột dữ liệu chung.

```java
// Ví dụ: trạng thái không hợp lệ
if (order.getStatus().equals("CANCELLED")) {
    throw new ConflictException("Cannot update a cancelled order");
}

// Ví dụ: concurrent modification
throw new ConflictException("This resource has been modified by another user. Please refresh.");
```

**JSON Response:**
```json
{
    "timestamp": "2026-06-19T14:30:00",
    "status": 409,
    "error": "Conflict",
    "message": "Cannot update a cancelled order",
    "path": "/api/orders/1",
    "traceId": "fa7a4e6b-a25f-4ce9-8973-2e06cbe19e7a"
}
```

---

### ServiceUnavailableException (503)

Dùng khi gọi service bên ngoài bị lỗi (microservices, third-party API).

```java
// Cách 1: Message tùy chọn
throw new ServiceUnavailableException("Payment gateway is not responding");

// Cách 2: Với tên service (tự tạo message)
throw new ServiceUnavailableException("payment-service", true);
// → Message: "Service 'payment-service' is currently unavailable. Please try again later."

// Cách 3: Với cause (dùng khi bắt lỗi từ RestTemplate/WebClient)
try {
    restTemplate.getForObject(url, User.class);
} catch (Exception e) {
    throw new ServiceUnavailableException("user-service", true, e);
}
```

**Getter method:**
```java
ex.getServiceName();  // "payment-service" (null nếu dùng constructor message)
```

**JSON Response:**
```json
{
    "timestamp": "2026-06-19T14:30:00",
    "status": 503,
    "error": "Service Unavailable",
    "message": "Service 'payment-service' is currently unavailable. Please try again later.",
    "path": "/api/payments",
    "traceId": "fa7a4e6b-a25f-4ce9-8973-2e06cbe19e7a"
}
```

---

### CloudStorageException (503)

Dùng khi thao tác với cloud storage (upload/download file) bị lỗi.

```java
// Ví dụ: upload file lỗi
try {
    s3Client.putObject(request, file);
} catch (S3Exception e) {
    throw new CloudStorageException("Failed to upload file to S3", e);
}

// Ví dụ: delete file lỗi
throw new CloudStorageException("Failed to delete file: " + filename);
```

**JSON Response:**
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

## Tạo Exception mới (mở rộng thư viện)

Nếu cần exception riêng, tạo theo mẫu:

```java
package atmin.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)  // 429
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }

    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

Sau đó thêm handler vào `CoreExceptionHandler` để tự động bắt lỗi.
