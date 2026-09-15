# Phát hành atmin-library 2.1.0

Tài liệu dành cho chủ thư viện. Người dùng bình thường chọn hướng dẫn cài đặt từ [trang tài liệu](../README.md).

## 1. Kiểm tra trước khi phát hành

```powershell
.\gradlew.bat clean check
```

Xác nhận:

- version trong `build.gradle` là `2.1.0`;
- test và Javadoc thành công;
- README không còn version cũ;
- Git working tree chỉ có thay đổi dự định phát hành;
- namespace `io.github.duongtran1702` đã được xác minh trên Sonatype Central Portal.

Không tái sử dụng một version đã phát hành vì Maven Central không cho ghi đè artifact.

## 2. Tạo token Maven Central

1. Đăng nhập [Sonatype Central Portal](https://central.sonatype.com/).
2. Mở account và tạo user token.
3. Lưu token username/password vào password manager.

Không dùng mật khẩu tài khoản thông thường và không commit token.

## 3. Tạo khóa GPG

Tạo khóa nếu chưa có:

```bash
gpg --full-generate-key
```

Xem key ID:

```bash
gpg --list-secret-keys --keyid-format LONG
```

Đưa public key lên key server:

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_LONG_KEY_ID
```

Nếu dùng Gradle signing dạng secret key ring:

```bash
gpg --export-secret-keys YOUR_LONG_KEY_ID > secring.gpg
```

Giữ `secring.gpg` ngoài repository.

## 4. Cấu hình local

Tạo hoặc cập nhật file user-level:

```text
C:\Users\<username>\.gradle\gradle.properties
```

Nội dung:

```properties
signing.keyId=YOUR_GPG_KEY_ID
signing.password=YOUR_GPG_PASSWORD
signing.secretKeyRingFile=C:/Users/<username>/.gnupg/secring.gpg

centralUsername=YOUR_CENTRAL_TOKEN_USERNAME
centralPassword=YOUR_CENTRAL_TOKEN_PASSWORD
```

Task upload cũng nhận hai environment variable:

```text
MAVEN_CENTRAL_USERNAME
MAVEN_CENTRAL_PASSWORD
```

Tên property cũ `username` và `password` vẫn được chấp nhận để không làm hỏng cấu hình 1.x, nhưng `centralUsername`/`centralPassword` rõ nghĩa và an toàn hơn.

## 5. Test artifact ở local

```powershell
.\gradlew.bat publishToMavenLocal
```

Trong một project Spring Boot 4 khác:

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    implementation 'io.github.duongtran1702:atmin-library:2.1.0'
}
```

Kiểm tra ít nhất:

- một success response;
- một custom exception;
- validation body;
- Security filter nếu project dùng Security;
- `Page` hoặc `Slice` nếu project dùng Spring Data.

## 6. Tạo Central bundle

```powershell
.\gradlew.bat clean check zipBundle
```

Kết quả:

```text
build/distributions/atmin-library-2.1.0-bundle.zip
```

Bundle chứa:

- main JAR;
- sources JAR;
- Javadoc JAR;
- POM;
- chữ ký ASCII-armored `.asc`;
- checksum do Maven publishing tạo.

`zipBundle` yêu cầu signing hợp lệ. Build thường và test không bắt buộc khóa ký.

## 7. Upload

### Cách A: task deploy

```powershell
.\gradlew.bat deploy
```

Task:

1. tạo lại local repository;
2. ký artifact;
3. đóng ZIP;
4. upload qua Central Portal Publisher API với `USER_MANAGED`.

Sau khi upload:

1. vào trang Deployments trên Central Portal;
2. đợi validation;
3. đọc và sửa mọi validation error nếu có;
4. bấm Publish;
5. đợi Maven Central đồng bộ.

### Cách B: upload thủ công

Có thể upload file ZIP ở bước 6 bằng giao diện Central Portal. Cách này hữu ích khi muốn kiểm tra bundle trước khi gửi.

## 8. Sau khi phát hành

Kiểm tra artifact:

```text
https://central.sonatype.com/artifact/io.github.duongtran1702/atmin-library/2.1.0
```

Sau đó:

1. tạo Git tag `v2.1.0`;
2. push tag;
3. tạo GitHub Release từ changelog;
4. chạy smoke test với dependency lấy từ Maven Central, không dùng `mavenLocal()`.

## Lỗi thường gặp

### Missing signing key

Kiểm tra `signing.keyId`, `signing.password`, `signing.secretKeyRingFile` và quyền đọc file key.

### Invalid signature

Đảm bảo public key đã được gửi lên key server và key ID đúng với khóa dùng ký bundle.

### Missing credentials

Dùng `centralUsername`/`centralPassword` hoặc hai environment variable được tài liệu ở trên.

### Namespace validation failed

Kiểm tra group ID trong `build.gradle` là `io.github.duongtran1702` và namespace đó đã được verify trên đúng tài khoản Central Portal.

### Version already exists

Đổi sang version mới. Maven Central artifact đã phát hành là immutable.
