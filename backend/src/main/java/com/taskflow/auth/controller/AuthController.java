package com.taskflow.auth.controller;

import com.taskflow.auth.dto.AuthResponse;
import com.taskflow.auth.dto.LoginRequest;
import com.taskflow.auth.dto.RegisterRequest;
import com.taskflow.auth.dto.UserResponse;
import com.taskflow.security.UserPrincipal;
import com.taskflow.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, authentication, and profile")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account and returns a JWT token")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user credentials and returns a JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current authenticated user", description = "Returns profile of the currently logged-in user")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserResponse response = authService.getCurrentUser(userPrincipal.getId());
        return ResponseEntity.ok(response);
    }
}
