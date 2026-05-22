package com.yourcompany.board.controller;

import com.yourcompany.board.dto.*;
import com.yourcompany.board.entity.User;
import com.yourcompany.board.repository.UserRepository;
import com.yourcompany.board.utils.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "用户名已存在"));
        }
        User user = new User(
                req.getUsername(),
                passwordEncoder.encode(req.getPassword()),
                req.getEmail()
        );
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getUsername());
        AuthResponse authResponse = new AuthResponse(token, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(authResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername()).orElse(null);
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "用户名或密码错误"));
        }
        String token = jwtUtil.generateToken(user.getUsername());
        AuthResponse authResponse = new AuthResponse(token, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(authResponse));
    }
}