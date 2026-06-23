# 🚀 Hướng dẫn Đăng tải & Phát hành Thư viện lên Maven Central (Sonatype Central Portal)

Tài liệu này hướng dẫn chi tiết cách cấu hình ký số GPG, thiết lập tài khoản và quy trình phát hành thư viện `atmin-library` lên Maven Central. Quy trình đã được tự động hóa tối đa thông qua Gradle.

---

## 📌 PHẦN 1: HƯỚNG DẪN CẤU HÌNH LẦN ĐẦU (Chỉ cần làm 1 lần duy nhất)

Nếu bạn đổi máy tính mới hoặc thiết lập lại từ đầu, hãy thực hiện theo các bước sau:

### Bước 1: Đăng ký tài khoản & Xác minh Namespace (Group ID)

1. Truy cập vào trang [central.sonatype.com](https://central.sonatype.com/) và đăng ký tài khoản.
2. Tại mục **Namespace**, thêm Namespace là `io.github.duongtran1702`.
3. Xác minh quyền sở hữu: Tạo một kho lưu trữ trống (repository) trên tài khoản GitHub cá nhân của bạn theo yêu cầu của Sonatype để xác nhận bạn là chủ sở hữu tài khoản `duongtran1702`.
4. Khi quá trình xác minh hoàn tất, trạng thái Namespace sẽ chuyển sang màu xanh lá (**Verified**).

### Bước 2: Tạo và cấu hình khóa ký số GPG (GPG Signing Key)

Maven Central bắt buộc tất cả các tệp tải lên phải được ký số điện tử GPG để xác thực nguồn gốc.

1. **Tạo khóa mới**:
   Mở terminal (ví dụ: Git Bash) và chạy lệnh:

   ```bash
   gpg --generate-key
   ```

   * Nhập Họ tên (`Real name`), Email của bạn.
   * Tạo một mật khẩu khóa (`Passphrase`) mạnh và ghi nhớ mật khẩu này.
2. **Lấy mã khóa (Key ID)**:
   Xem danh sách các khóa trên máy bằng lệnh:

   ```bash
   gpg --list-keys
   ```

   Tìm khóa vừa tạo. Nó là một chuỗi ký tự hex dài (ví dụ: `YOUR_GPG_KEY_ID...`). Hãy sao chép **8 ký tự cuối cùng** của mã khóa (ví dụ: `YOUR_GPG_KEY_ID`).
3. **Gửi khóa công khai lên server**:
   Đưa khóa công khai của bạn lên máy chủ để Maven Central có thể đối chiếu chữ ký:

   ```bash
   gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_GPG_KEY_ID
   ```

   *(Thay `YOUR_GPG_KEY_ID` bằng 8 ký tự khóa của bạn)*.
4. **Xuất khóa bí mật (Secret Key Ring File)**:
   Để Gradle có thể ký tự động khi build, bạn cần xuất file khóa bí mật sang định dạng kế thừa `.gpg`:

   ```bash
   gpg --keyring secring.gpg --export-secret-keys > C:/Users/<your_username>/.gnupg/secring.gpg
   ```

### Bước 3: Tạo User Token trên Sonatype

1. Đăng nhập vào [Sonatype Central](https://central.sonatype.com/).
2. Chọn ảnh đại diện ở góc phải -> Chọn **View Account** -> Click **Generate User Token**.
3. Bạn sẽ nhận được 2 thông tin:
   * **Username Token** (ví dụ: `YOUR_SONATYPE_USERNAME`)
   * **Password Token** (ví dụ: `kIQJbNas...`)

### Bước 4: Thiết lập file gradle.properties trên máy tính

Để bảo mật thông tin tài khoản và khóa GPG, các thông tin này tuyệt đối không được đưa vào mã nguồn git. Thay vào đó, hãy lưu tại file `gradle.properties` toàn cục của máy tính bạn.

📂 Đường dẫn file: `C:\Users\<your_username>\.gradle\gradle.properties`

Nội dung file:

```properties
# 1. Cấu hình ký số khóa GPG
signing.keyId=YOUR_GPG_KEY_ID
signing.password=MẬT_KHẨU_KHÓA_GPG_CỦA_BẠN
signing.secretKeyRingFile=C:/Users/<your_username>/.gnupg/secring.gpg

# 2. Cấu hình tài khoản Token Sonatype Central
username=YOUR_SONATYPE_USERNAME
password=MẬT_KHẨU_TOKEN_SONATYPE_CỦA_BẠN
```

---

## 📌 PHẦN 2: QUY TRÌNH PHÁT HÀNH CÁC LẦN SAU (Cực kỳ đơn giản)

Khi bạn đã hoàn thành cấu hình ở Phần 1, mỗi khi bạn cập nhật code và muốn phát hành phiên bản mới (ví dụ từ `1.0.2` lên `1.0.3.Beta`), hãy làm theo các bước sau:

### Bước 1: Cập nhật phiên bản dự án

Mở file [build.gradle](file:///d:/atmin-library/build.gradle) và thay đổi giá trị phiên bản mong muốn:

```groovy
version = '1.0.3.Beta'
```

### Bước 2: Chạy lệnh phát hành tự động

Mở terminal tại thư mục gốc của dự án (`d:\atmin-library`) và chạy duy nhất lệnh dưới đây:

```powershell
.\gradlew deploy
```

**Hệ thống sẽ tự động thực hiện**:

1. Biên dịch toàn bộ mã nguồn Java.
2. Tạo tệp mã nguồn (`-sources.jar`) và tài liệu API (`-javadoc.jar`).
3. Thực hiện ký số điện tử GPG lên tất cả các tệp bằng khóa bí mật của bạn.
4. Đóng gói tất cả tệp tin và chữ ký vào một tệp ZIP bundle duy nhất tại `build/distributions/atmin-library-X.Y.Z-bundle.zip`.
5. Tải trực tiếp tệp ZIP lên Sonatype Central API bằng Java HttpClient tích hợp sẵn.

### Bước 3: Kiểm tra trạng thái đồng bộ

Do lệnh chạy với cấu hình loại xuất bản là `USER_MANAGED` (trong API Upload link), Sonatype sẽ kiểm duyệt (validate) dự án của bạn (kiểm tra đầy đủ chữ ký GPG, định dạng file POM, thông tin bản quyền).

* Nếu kiểm tra thành công, bạn cần truy cập giao diện điều khiển của Sonatype Central Portal để nhấn nút **Publish** thủ công nhằm phát hành thư viện ra công chúng.
* Quá trình đồng bộ hóa toàn cầu mất khoảng **15 - 30 phút**.
* Bạn có thể theo dõi tiến trình trực tuyến tại: 👉 [Sonatype Central Portal - Deployments](https://central.sonatype.com/)

---

## 🛠️ CHI TIẾT CẤU HÌNH Gradle (Dành cho việc bảo trì)

Trong [build.gradle](file:///d:/atmin-library/build.gradle), hai task chính được định nghĩa ở cuối file để nén gói và tải lên cổng thông tin:

```groovy
// 1. Task đóng gói tất cả artifact cục bộ và chữ ký GPG thành file ZIP
tasks.register('zipBundle', Zip) {
    dependsOn 'cleanRepo', 'publishMavenJavaPublicationToLocalBundleRepository'
    from layout.buildDirectory.dir("repo")
    archiveFileName = "atmin-library-${version}-bundle.zip"
    destinationDirectory = layout.buildDirectory.dir("distributions")
}

// 2. Task gọi REST API để đẩy thẳng file ZIP lên Sonatype Central Portal
tasks.register('deploy') {
    dependsOn 'zipBundle'
    doLast {
        def zipFile = layout.buildDirectory.file("distributions/atmin-library-${version}-bundle.zip").get().asFile
        if (!zipFile.exists()) {
            throw new GradleException("Zip bundle file not found: ${zipFile.absolutePath}")
        }
        
        def username = project.properties["username"] ?: System.getenv("username")
        def password = project.properties["password"] ?: System.getenv("password")
        
        if (!username || !password) {
            throw new GradleException("Maven Central credentials (username/password) are not configured in gradle.properties.")
        }
        
        def authString = "${username}:${password}"
        def encodedAuth = java.util.Base64.getEncoder().encodeToString(authString.getBytes("UTF-8"))
        
        println "Uploading bundle to Sonatype Central Portal..."
        
        def boundary = "---------------------------" + System.currentTimeMillis()
        def newline = "\r\n"
        
        def byteOutputStream = new java.io.ByteArrayOutputStream()
        byteOutputStream.write(("--" + boundary + newline).getBytes("UTF-8"))
        byteOutputStream.write(("Content-Disposition: form-data; name=\"bundle\"; filename=\"" + zipFile.name + "\"" + newline).getBytes("UTF-8"))
        byteOutputStream.write(("Content-Type: application/zip" + newline + newline).getBytes("UTF-8"))
        byteOutputStream.write(java.nio.file.Files.readAllBytes(zipFile.toPath()))
        byteOutputStream.write(newline.getBytes("UTF-8"))
        byteOutputStream.write(("--" + boundary + "--" + newline).getBytes("UTF-8"))
        
        def client = java.net.http.HttpClient.newHttpClient()
        def uri = java.net.URI.create("https://central.sonatype.com/api/v1/publisher/upload?name=atmin-library-${version}&publishingType=USER_MANAGED")
        
        def request = java.net.http.HttpRequest.newBuilder()
            .uri(uri)
            .header("Authorization", "Bearer " + encodedAuth)
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(byteOutputStream.toByteArray()))
            .build()
            
        def response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
        
        println "HTTP Status: " + response.statusCode()
        println "Response: " + response.body()
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            println "Upload successful! Check status on https://central.sonatype.com/"
        } else {
            throw new GradleException("Failed to upload bundle: HTTP ${response.statusCode()} - ${response.body()}")
        }
    }
}
```
