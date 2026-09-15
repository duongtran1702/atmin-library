# atmin-library 2.1.0

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F.svg)](https://spring.io/projects/spring-boot)
[![Version](https://img.shields.io/badge/version-2.1.0-blue.svg)](https://central.sonatype.com/artifact/io.github.duongtran1702/atmin-library/2.1.0)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

Thư viện infrastructure dùng chung cho Spring Boot 4, cung cấp response contract, exception handling, validation, security error và trace ID cho cả Servlet MVC lẫn Reactive WebFlux/Gateway.

## Chọn tài liệu

Mỗi cụm có hai file tách biệt: `README.md` giải thích thư viện làm được gì; `USAGE.md` là hướng dẫn triển khai đầy đủ để người dùng hoặc AI có thể áp dụng ngay.

| Web stack | Khả năng | Hướng dẫn sử dụng |
|---|---|---|
| Spring MVC / Servlet | [docs/mvc/README.md](docs/mvc/README.md) | [docs/mvc/USAGE.md](docs/mvc/USAGE.md) |
| WebFlux / Microservice / Gateway | [docs/reactive/README.md](docs/reactive/README.md) | [docs/reactive/USAGE.md](docs/reactive/USAGE.md) |

Chỉ cần đọc cụm tương ứng với application type; không cần kết hợp tài liệu MVC và reactive.

## Cài đặt nhanh

Gradle:

```groovy
implementation 'io.github.duongtran1702:atmin-library:2.1.0'
```

Maven:

```xml
<dependency>
    <groupId>io.github.duongtran1702</groupId>
    <artifactId>atmin-library</artifactId>
    <version>2.1.0</version>
</dependency>
```

Yêu cầu Java 21 và Spring Boot 4.x. Các integration là tùy chọn và chỉ kích hoạt khi dependency tương ứng có trên classpath.

## Trạng thái phát hành

Phiên bản `2.1.0` đã được phát hành trên Maven Central. Quality gate gồm compile, Javadoc và 52 test.

Phát hành theo [Apache License 2.0](LICENSE).
