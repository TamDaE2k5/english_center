---
name: spring-boot-java-expert
description: >-
  Use this skill whenever developing, refactoring, or debugging Java 21 and Spring Boot 3.3 services
  (such as the Admission & Identity Service). Covers 3-tier layered architecture, REST controllers,
  DTO validation, JPA repositories, Spring Security 6 with JWT, Jackson snake_case serialization,
  global exception handling, and transaction management.
---

# Spring Boot 3.3 & Java 21 Engineering Skill

## 1. Architectural Principles (Nguyên tắc Kiến trúc)

Tuân thủ nghiêm ngặt mô hình **3 Tầng (3-Layered Architecture)**:

```
[HTTP Request] 
      │
      ▼
Controller  ───> (@Valid RequestDTO, ApiResponse<T>)
      │
      ▼
Service     ───> (@Transactional, Business Logic, Invariant Validation)
      │
      ▼
Repository  ───> (Spring Data JPA, Schema-isolated Queries)
      │
      ▼
[PostgreSQL Database (admission.*)]
```

### Quy tắc phân tầng:
1. **Controller Layer**:
   - Chỉ đảm nhiệm: Nhận request, validate dữ liệu đầu vào qua `@Valid`, điều hướng sang Service layer, và bọc kết quả trả về trong `ResponseEntity<ApiResponse<T>>`.
   - **Tuyệt đối không** viết logic nghiệp vụ, tính toán học phí hay kiểm tra điều kiện database trực tiếp trong Controller.
2. **Service Layer**:
   - Trái tim của ứng dụng: Xử lý toàn bộ logic nghiệp vụ (Kiểm tra xung đột phòng/giáo viên, kiểm tra sĩ số `max_students`, xử lý hạn 48h).
   - Thao tác ghi/sửa nhiều bước bắt buộc khai báo `@Transactional(rollbackFor = Exception.class)`.
   - Bắt buộc comment chi tiết bằng tiếng Việt giải thích từng bước logic.
3. **Repository Layer**:
   - Kế thừa `JpaRepository<Entity, ID>`.
   - Chỉ truy vấn trong schema `admission`. Tuyệt đối không JOIN sang schema khác.

---

## 2. API Conventions & Snake Case Serialization

Toàn bộ API request/response bắt buộc dùng định dạng **`snake_case`**.

### Cấu hình Jackson toàn cục:
Trong `application.yml` hoặc `application.properties`:
```yaml
spring:
  jackson:
    property-naming-strategy: SNAKE_CASE
    serialization:
      write-dates-as-timestamps: false
```

### Chuẩn hóa Response Wrapper:
```java
package vn.edu.english.admission.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private ApiError error;

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(new ApiError(code, message))
                .build();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ApiError {
        private String code;
        private String message;
    }
}
```

---

## 3. Global Exception Handling (@RestControllerAdvice)

Tuyệt đối không để lộ Java StackTrace HTTP 500 ra client. Xử lý tập trung qua Advice:

```java
package vn.edu.english.admission.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.edu.english.admission.dto.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Bắt lỗi nghiệp vụ chủ động ném ra
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    // Bắt lỗi dữ liệu đầu vào không hợp lệ từ @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Dữ liệu đầu vào không hợp lệ");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("VALIDATION_ERROR", errorMessage));
    }

    // Bắt mọi lỗi hệ thống ngoài dự kiến (An toàn bảo mật)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "Đã có lỗi hệ thống xảy ra, vui lòng thử lại sau"));
    }
}
```

---

## 4. DTO & Validation Best Practices

1. Sử dụng Bean Validation: `@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max`, `@Email`.
2. Tách biệt DTO Request và DTO Response, không tái sử dụng trực tiếp JPA Entity làm output API để tránh leak thông tin bảo mật (như `password_hash`) và lỗi vòng lặp tuần tự hóa (`LazyInitializationException`).

Ví dụ:
```java
@Data
public class CreateCourseRequest {
    @NotBlank(message = "Mã khóa học không được để trống")
    @Size(max = 50, message = "Mã khóa học tối đa 50 ký tự")
    private String code;

    @NotBlank(message = "Tên khóa học không được để trống")
    private String name;

    @NotNull(message = "Học phí không được để trống")
    @Min(value = 0, message = "Học phí phải >= 0")
    private BigDecimal tuitionAmount;
}
```

---

## 5. Security & JWT Filter Pattern

1. Cấp phát cặp JWT Token:
   - Access Token: Thời hạn 15 phút, stateless, mang claims (`sub`, `role`, `email`, `name`).
   - Refresh Token: Lưu trong database `admission.refresh_tokens`, thời hạn 7 ngày, hỗ trợ Token Rotation.
2. Bộ lọc `OncePerRequestFilter`:
   - Trích xuất token từ header `Authorization: Bearer <token>`.
   - Validate chữ ký và hạn dùng.
   - Nạp `UsernamePasswordAuthenticationToken` vào `SecurityContextHolder`.

---

## 6. Checklist Kiểm tra Code trước khi Hoàn tất

- [ ] Controller chỉ gọi Service, không có code logic database.
- [ ] Mọi method thay đổi dữ liệu có annotation `@Transactional`.
- [ ] Đã thêm comment tiếng Việt chi tiết cho các bước logic quan trọng.
- [ ] Không có emoji trong log hoặc mã nguồn.
- [ ] Response JSON trả về đúng chuẩn `{"success": ..., "data": ...}`.
- [ ] Đã chạy kiểm thử Invariant Test pass 100%:
  ```bash
  python3 -m unittest discover -s tests/invariants -p "test_*.py" -v
  ```
