# 📦 Response Classes Documentation

Thư viện cung cấp các class response chuẩn để đảm bảo **format JSON đồng nhất** trên toàn bộ API.

## Tổng quan

| Class | Mục đích |
|---|---|
| `ApiResponse<T>` | Response wrapper chung (success / error / paginated / slice paginated) |
| `ApiErrorResponse` | Response lỗi chi tiết (dùng trong exception handler, tự động có trace ID) |
| `PageInfo` | Thông tin phân trang (hỗ trợ cả cơ chế đếm Page và không đếm Slice) |

## Import

```java
import atmin.common.response.ApiResponse;
import atmin.common.response.ApiErrorResponse;
import atmin.common.response.PageInfo;
```

---

## 1. ApiResponse\<T\> — Response chung

Class wrapper chung cho mọi API response. Hỗ trợ generic type `<T>` cho data payload.

### Cấu trúc

```java
public class ApiResponse<T> {
    private boolean success;     // true = thành công, false = lỗi
    private int status;          // HTTP status code (200, 201, 400...)
    private String message;      // Thông điệp mô tả
    private T data;              // Dữ liệu trả về (null nếu không có)
    private PageInfo page;       // Thông tin phân trang (null nếu không phân trang)
}
```

> **Lưu ý:** `data` và `page` tự động **ẩn khỏi JSON** khi giá trị là `null` (nhờ `@JsonInclude(NON_NULL)`).

### Factory Methods

#### ✅ Success Methods

```java
// 200 OK — với data
ApiResponse.success("Lấy user thành công", user)

// 200 OK — không data
ApiResponse.success("Xóa thành công")

// 200 OK — với cấu hình HttpStatus
ApiResponse.success(HttpStatus.ACCEPTED, "Đang xử lý", requestId)

// 201 Created — tạo mới
ApiResponse.created("Tạo user thành công", savedUser)

// 202 Accepted — xử lý bất đồng bộ
ApiResponse.accepted("Yêu cầu đang được xử lý", jobId)

// 204 No Content
ApiResponse.noContent("Đã xóa")
```

#### 📄 Paginated Methods

```java
// 1. Phân trang dạng Page (Spring Data Page — Có truy vấn COUNT)
Page<User> page = userRepository.findAll(pageable);
ApiResponse.paginated("Danh sách users", page.getContent(), PageInfo.from(page))

// Factory rút gọn mới trong 2.0
ApiResponse.pagePaginated("Danh sách users", page)

// 2. Phân trang dạng Slice (Spring Data Slice — Không COUNT, tối ưu cho bảng lớn)
Slice<User> slice = userRepository.findAllSlice(pageable);
ApiResponse.slicePaginated("Danh sách users", slice)
```

#### ❌ Error Methods

```java
// Với status code
ApiResponse.error(400, "Dữ liệu không hợp lệ")

// Với HttpStatus
ApiResponse.error(HttpStatus.NOT_FOUND, "User not found")

// Shortcut methods
ApiResponse.badRequest("Email đã tồn tại")
ApiResponse.notFound("User not found")
ApiResponse.internalError("Server error")
```

### Ví dụ sử dụng trong Controller

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    // GET /api/users/{id} — Lấy 1 user
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(
            ApiResponse.success("Lấy user thành công", user)
        );
    }

    // POST /api/users — Tạo user mới
    @PostMapping
    public ResponseEntity<ApiResponse<User>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        User user = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.created("Tạo user thành công", user)
        );
    }

    // GET /api/users — Danh sách user (phân trang Page - có COUNT)
    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<User> userPage = userService.findAll(PageRequest.of(page, size));
        return ResponseEntity.ok(
            ApiResponse.paginated(
                "Danh sách users",
                userPage.getContent(),
                PageInfo.from(userPage)
            )
        );
    }

    // GET /api/users/infinite — Danh sách user (phân trang Slice - không COUNT)
    @GetMapping("/infinite")
    public ResponseEntity<ApiResponse<List<User>>> getUsersInfinite(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Slice<User> userSlice = userService.findAllSlice(PageRequest.of(page, size));
        return ResponseEntity.ok(
            ApiResponse.slicePaginated(
                "Danh sách users",
                userSlice
            )
        );
    }
}
```

### JSON Response mẫu

**Success với data:**
```json
{
    "success": true,
    "status": 200,
    "message": "Lấy user thành công",
    "data": {
        "id": 1,
        "name": "Nguyen Van A",
        "email": "a@example.com"
    }
}
```

**Paginated (với Page — đầy đủ thông số):**
```json
{
    "success": true,
    "status": 200,
    "message": "Danh sách users",
    "data": [
        { "id": 1, "name": "User 1" },
        { "id": 2, "name": "User 2" }
    ],
    "page": {
        "pageNumber": 0,
        "pageSize": 10,
        "totalElements": 25,
        "totalPages": 3,
        "hasNext": true,
        "first": true,
        "last": false
    }
}
```

**Paginated (với Slice — tối giản, ẩn các trường null):**
```json
{
    "success": true,
    "status": 200,
    "message": "Danh sách users",
    "data": [
        { "id": 1, "name": "User 1" },
        { "id": 2, "name": "User 2" }
    ],
    "page": {
        "pageNumber": 0,
        "pageSize": 10,
        "hasNext": true
    }
}
```

---

## 2. ApiErrorResponse — Response lỗi chi tiết

Class chuyên dùng cho **exception handler**. Cung cấp thông tin lỗi chi tiết hơn `ApiResponse` và mặc định tự động đính kèm `traceId`.

### Cấu trúc

```java
public class ApiErrorResponse {
    private LocalDateTime timestamp;       // Thời điểm xảy ra lỗi
    private int status;                    // HTTP status code
    private String error;                  // Tên lỗi (e.g., "Not Found")
    private String message;                // Chi tiết lỗi
    private String path;                   // URI gây lỗi
    private Map<String, String> errors;    // Lỗi từng field (validation) — nullable
    private String traceId;                // Trace ID phục vụ truy vết lỗi — luôn tự sinh/đọc từ MDC
}
```

### Factory Methods

```java
// Generic — từ HttpStatus
ApiErrorResponse.of(HttpStatus.NOT_FOUND, "User not found", "/api/users/1")

// Shortcut methods
ApiErrorResponse.badRequest("Invalid email", "/api/users")
ApiErrorResponse.unauthorized("Token expired", "/api/profile")
ApiErrorResponse.forbidden("No permission", "/api/admin")
ApiErrorResponse.notFound("User not found", "/api/users/1")
ApiErrorResponse.conflict("Email already exists", "/api/users")
ApiErrorResponse.internalServerError("Unexpected error", "/api/users")
ApiErrorResponse.serviceUnavailable("Service down", "/api/orders")

// Validation errors — với chi tiết từng field
Map<String, String> fieldErrors = Map.of(
    "email", "Email is required",
    "name", "Name must be at least 2 characters"
);
ApiErrorResponse.validationError("/api/users", fieldErrors)

// Tùy chỉnh validation summary trong 2.0
ApiErrorResponse.validationError("/api/users", "Dữ liệu không hợp lệ", fieldErrors)
```

### JSON Response mẫu

**Lỗi đơn giản (Ví dụ: 404 Not Found):**
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

**Lỗi validation (nhiều field):**
```json
{
    "timestamp": "2026-06-19T14:30:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Validation failed",
    "path": "/api/users",
    "errors": {
        "email": "Email is required",
        "name": "Name must be at least 2 characters",
        "age": "Age must be >= 18"
    },
    "traceId": "c8b9d0e1-f2a3-4b5c-6d7e-8f9a0b1c2d3e"
}
```

### Cấu hình MDC Tracing Key tại Ứng dụng
Khóa MDC mặc định dùng để trích xuất `traceId` là `"traceId"`. Nếu ứng dụng của bạn cấu hình traceId thông qua hệ thống khác sử dụng khóa khác (ví dụ: `correlationId`, `x-request-id`), bạn có thể ghi đè lúc khởi chạy:

```java
// Cấu hình một lần duy nhất tại hàm main hoặc class Config khởi tạo
// Khuyên dùng trong atmin-library 2.0:
// atmin.exceptions.trace-id-mdc-key=correlationId

// API 1.x này vẫn được hỗ trợ:
ApiErrorResponse.setMdcKey("correlationId");
```

---

## 3. PageInfo — Thông tin phân trang

Class chứa metadata phân trang, hỗ trợ cả hai loại phân trang Page (đầy đủ thông tin) và Slice (tối giản).

### Cấu trúc

```java
public class PageInfo {
    private int pageNumber;        // Trang hiện tại (bắt đầu từ 0)
    private int pageSize;          // Số items mỗi trang
    private boolean hasNext;       // Còn trang kế tiếp hay không (luôn có)

    // Các trường dưới đây sẽ ẩn khỏi JSON nếu mang giá trị null
    @JsonInclude(Include.NON_NULL)
    private Long totalElements;    // Tổng số items
    @JsonInclude(Include.NON_NULL)
    private Integer totalPages;    // Tổng số trang
    @JsonInclude(Include.NON_NULL)
    private Boolean first;         // Có phải trang đầu?
    @JsonInclude(Include.NON_NULL)
    private Boolean last;          // Có phải trang cuối?
}
```

### Factory Methods

```java
// 1. Tạo từ Spring Data Page (Có đầy đủ thông số đếm phần tử)
Page<User> page = userRepository.findAll(pageable);
PageInfo pageInfo = PageInfo.from(page);

// 2. Tạo từ Spring Data Slice (Chỉ có pageNumber, pageSize, hasNext)
Slice<User> slice = userRepository.findAllSlice(pageable);
PageInfo pageInfo = PageInfo.from(slice);

// 3. Tạo thủ công (không cần Spring Data, mặc định sinh đầy đủ thông số)
PageInfo pageInfo = PageInfo.of(0, 10, 100);
// → pageNumber=0, pageSize=10, totalElements=100, totalPages=10, hasNext=true, first=true, last=false

// 4. Dùng Builder (tùy chỉnh hoàn toàn)
PageInfo pageInfo = PageInfo.builder()
        .pageNumber(2)
        .pageSize(20)
        .hasNext(true)
        .build();

// 5. Tạo Slice metadata thủ công, không cần Spring Data
PageInfo sliceInfo = PageInfo.slice(0, 20, true);
```

> Từ 2.0, `PageInfo.of` và `PageInfo.slice` kiểm tra dữ liệu đầu vào:
> `pageNumber >= 0`, `pageSize > 0`, `totalElements >= 0`.
