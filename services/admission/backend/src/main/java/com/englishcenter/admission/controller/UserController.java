package com.englishcenter.admission.controller;

import com.englishcenter.admission.common.dto.ApiResponse;
import com.englishcenter.admission.common.dto.PageResponse;
import com.englishcenter.admission.dto.user.CreateUserRequest;
import com.englishcenter.admission.dto.user.ToggleActiveRequest;
import com.englishcenter.admission.dto.user.UpdateUserRequest;
import com.englishcenter.admission.dto.user.UserResponse;
import com.englishcenter.admission.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý các yêu cầu quản lý người dùng và tài khoản hệ thống.
 * Phân quyền chặt chẽ: Các thao tác quản trị yêu cầu quyền ADMIN, xem/sửa hồ sơ cá nhân tuân thủ INV-SEC-01.
 */
@RestController
@RequestMapping("/api/admission/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Lookup giáo viên cho ADMIN/STAFF, không mở quyền danh sách người dùng đầy đủ. */
    @GetMapping("/teachers")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<java.util.List<UserService.TeacherOption>>> teachers() {
        return ResponseEntity.ok(ApiResponse.success(userService.listTeacherOptions()));
    }

    /**
     * Tạo mới tài khoản nhân sự (STAFF hoặc TEACHER) bởi Quản trị viên.
     *
     * @param request DTO dữ liệu tạo người dùng
     * @return ApiResponse chứa UserResponse vừa tạo (HTTP 201 Created)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo tài khoản nhân sự thành công"));
    }

    /**
     * Lấy danh sách tài khoản người dùng có phân trang và lọc theo vai trò, từ khóa.
     *
     * @param role    Lọc theo vai trò (ADMIN, STAFF, TEACHER, STUDENT)
     * @param keyword Từ khóa tìm kiếm theo email hoặc tên
     * @param page    Số trang (mặc định 0)
     * @param size    Kích thước trang (mặc định 20)
     * @return ApiResponse chứa PageResponse các tài khoản
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<UserResponse> pageResponse = userService.listUsers(role, keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    /**
     * Lấy chi tiết thông tin tài khoản người dùng theo ID.
     * Tuân thủ INV-SEC-01: Admin xem được mọi người, học viên/giáo viên chỉ xem được chính mình.
     *
     * @param id ID người dùng cần xem
     * @return ApiResponse chứa UserResponse
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Integer id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Cập nhật thông tin hồ sơ người dùng.
     * Tuân thủ INV-SEC-01: Admin hoặc chính chủ tài khoản mới được cập nhật.
     *
     * @param id      ID người dùng cần cập nhật
     * @param request DTO dữ liệu cập nhật
     * @return ApiResponse chứa UserResponse sau cập nhật
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật thông tin tài khoản thành công"));
    }

    /**
     * Khóa hoặc mở khóa tài khoản người dùng (chỉ ADMIN).
     *
     * @param id      ID người dùng cần khóa/mở khóa
     * @param request DTO chứa trạng thái is_active và lý do
     * @return ApiResponse chứa UserResponse sau khi thay đổi trạng thái
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(
            @PathVariable Integer id,
            @Valid @RequestBody ToggleActiveRequest request) {
        UserResponse response = userService.toggleUserStatus(id, request.getIsActive(), request.getReason());
        String msg = request.getIsActive() ? "Mở khóa tài khoản thành công" : "Khóa tài khoản thành công";
        return ResponseEntity.ok(ApiResponse.success(response, msg));
    }
}
