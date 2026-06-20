package com.twitter.auth.service.controller;

import com.twitter.auth.service.api.ApiResponse;
import com.twitter.auth.service.dto.*;
import com.twitter.auth.service.service.AuthService;
import com.twitter.auth.service.service.EmailVerificationService;
import com.twitter.auth.service.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService; // ✅ FIXED

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity.ok(
                ApiResponse.<RegisterResponse>builder()
                        .success(true)
                        .message("User registered successfully")
                        .data(authService.register(request))
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {

        return ResponseEntity.ok(
                ApiResponse.<LoginResponse>builder()
                        .success(true)
                        .message("Login successful")
                        .data(authService.login(request, httpServletRequest))
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @RequestBody RefreshTokenRequest request) {

        return ResponseEntity.ok(
                ApiResponse.<LoginResponse>builder()
                        .success(true)
                        .message("Token refreshed")
                        .data(authService.refreshToken(request.getRefreshToken()))
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody RefreshTokenRequest request) {

        String accessToken = authHeader.substring(7);

        authService.logout(request.getRefreshToken(), accessToken);

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("Logged out successfully")
                        .data("SUCCESS")
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<String>> logoutAll(
            Authentication authentication
    ) {

        authService.logoutAllDevices(authentication.getName());

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("Logged out from all devices")
                        .data("SUCCESS")
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        String message = passwordResetService.createResetToken(request.getEmail());

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("Password reset token generated")
                        .data(message)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        passwordResetService.resetPassword(
                request.getToken(),
                request.getNewPassword()
        );

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("Password reset successful")
                        .data("SUCCESS")
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            Authentication authentication,
            @RequestBody ChangePasswordRequest request
    ) {

        authService.changePassword(authentication.getName(), request);

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("Password changed successfully")
                        .data("SUCCESS")
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(
            @RequestParam String token
    ) {
        emailVerificationService.confirmToken(token);
        return ResponseEntity.ok("Email verified successfully");
    }
}