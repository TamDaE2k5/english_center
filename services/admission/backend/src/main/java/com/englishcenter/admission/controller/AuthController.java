package com.englishcenter.admission.controller;

import com.englishcenter.admission.entity.User;
import com.englishcenter.admission.repository.UserRepository;
import com.englishcenter.admission.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public record LoginRequest(String email, String password) {}

    public record RegisterRequest(String email, String password, String full_name, String role, String phone) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (req.email() == null || req.password() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", Map.of("code", "AUTH_INVALID_INPUT", "message", "Email và mật khẩu không được để trống")
            ));
        }

        Optional<User> userOpt = userRepository.findByEmail(req.email());
        if (userOpt.isEmpty() || !userOpt.get().getPasswordHash().equals(req.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "error", Map.of("code", "AUTH_INVALID_CREDENTIALS", "message", "Email hoặc mật khẩu không chính xác")
            ));
        }

        User user = userOpt.get();
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole(), user.getFullName());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đăng nhập thành công",
                "data", Map.of(
                        "token", token,
                        "user", Map.of(
                                "id", user.getId(),
                                "email", user.getEmail(),
                                "name", user.getFullName(),
                                "role", user.getRole()
                        )
                )
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (userRepository.findByEmail(req.email()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false,
                    "error", Map.of("code", "AUTH_EMAIL_EXISTS", "message", "Email này đã được sử dụng")
            ));
        }

        User newUser = new User();
        newUser.setEmail(req.email());
        newUser.setPasswordHash(req.password());
        newUser.setFullName(req.full_name());
        newUser.setRole(req.role() != null ? req.role() : "STUDENT");
        newUser.setPhone(req.phone());

        User saved = userRepository.save(newUser);
        String token = jwtUtil.generateToken(saved.getId(), saved.getEmail(), saved.getRole(), saved.getFullName());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Đăng ký thành công",
                "data", Map.of("token", token, "id", saved.getId(), "email", saved.getEmail())
        ));
    }
}
