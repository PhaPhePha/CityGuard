package com.cityguard.backend.controller;

import com.cityguard.backend.dto.request.AuthRequest;
import com.cityguard.backend.dto.response.ApiResponse;
import com.cityguard.backend.dto.response.AuthResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Demo auth boundary. Replace the store with UserRepository/JWT before production. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody AuthRequest request) {
        if (users.containsKey(request.username())) return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.<AuthResponse>failure("Tên đăng nhập đã tồn tại"));
        User user = new User(UUID.randomUUID().toString(), request.username(), hash(request.password()));
        users.put(request.username(), user);
        return ResponseEntity.ok(ApiResponse.success("Đăng ký thành công", createSession(user)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        User user = users.get(request.username());
        if (user == null || !user.passwordHash.equals(hash(request.password()))) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.<AuthResponse>failure("Thông tin đăng nhập không đúng"));
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", createSession(user)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization != null) sessions.remove(authorization.replace("Bearer ", ""));
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }

    private AuthResponse createSession(User user) { String token = UUID.randomUUID().toString(); sessions.put(token, user.id); return new AuthResponse(token, user.id, user.username); }
    private String hash(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
    private record User(String id, String username, String passwordHash) {}
}
