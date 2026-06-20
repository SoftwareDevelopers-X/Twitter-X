package com.twitter.auth.service.service;

import com.twitter.auth.service.dto.*;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(
            LoginRequest request,
            HttpServletRequest servletRequest
    );

    LoginResponse refreshToken(String refreshToken);

    void logout(String refreshToken, String accessToken);

    void logoutAllDevices(String email);

    void changePassword(
            String email,
            ChangePasswordRequest request
    );
}