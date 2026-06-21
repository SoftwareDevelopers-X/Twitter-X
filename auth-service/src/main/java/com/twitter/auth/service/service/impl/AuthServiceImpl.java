package com.twitter.auth.service.service.impl;

import com.twitter.auth.service.Enum.AuditAction;
import com.twitter.auth.service.Enum.Role;
import com.twitter.auth.service.Model.RefreshToken;
import com.twitter.auth.service.Model.User;
import com.twitter.auth.service.dto.*;
import com.twitter.auth.service.exception.InvalidCredentialsException;
import com.twitter.auth.service.exception.UserAlreadyExistsException;
import com.twitter.auth.service.repository.UserRepository;
import com.twitter.auth.service.security.CustomUserDetailsService;
import com.twitter.auth.service.security.JwtService;
import com.twitter.auth.service.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

import com.twitter.auth.service.constant.AuthConstants;
import com.twitter.auth.service.exception.AccountLockedException;
import com.twitter.auth.service.exception.UserNotFoundException;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuditLogService auditLogService;
    private final RateLimitService rateLimitService;
    private final DeviceSessionService deviceSessionService;

    @Override
    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(true)   // IMPORTANT CHANGE
                .build();

        User saved = userRepository.save(user);

        // TODO: generate verification token (next step)
        auditLogService.log(
                saved.getEmail(),
                AuditAction.REGISTER
        );

        return RegisterResponse.builder()
                .userId(saved.getUserId())
                .username(saved.getUsername())
                .email(saved.getEmail())
                .build();
    }

    @Override
    public LoginResponse login(LoginRequest request, HttpServletRequest servletRequest) {

        // 1. RATE LIMIT CHECK
        rateLimitService.validateLoginAttempt(request.getEmail());

        // 2. FETCH USER
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // 3. ACCOUNT LOCK CHECK
        unlockWhenTimeExpired(user);

        if (user.isAccountLocked()) {
            throw new AccountLockedException(
                    "Account is locked. Try again after 15 minutes."
            );
        }

        try {

            // 4. AUTHENTICATION
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            // 5. RESET FAILED ATTEMPTS (IMPORTANT FIX)
            resetFailedAttempts(user);

            // 6. RESET RATE LIMIT ON SUCCESS (IMPORTANT FIX)
            rateLimitService.resetAttempts(request.getEmail());

        } catch (Exception ex) {

            // 7. INCREASE FAILED LOGIN COUNT
            increaseFailedAttempts(user);

            auditLogService.log(
                    user.getEmail(),
                    AuditAction.LOGIN_FAILED
            );

            throw ex;
        }

        // 8. LOAD USER DETAILS
        UserDetails userDetails =
                userDetailsService.loadUserByUsername(
                        request.getEmail()
                );

        // 9. GENERATE JWT
        String accessToken =
                jwtService.generateAccessToken(
                        userDetails,
                        user.getUserId(),
                        user.getRole().name()
                );

        // 10. CREATE REFRESH TOKEN
        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(
                        request.getEmail()
                );

        // 11. DEVICE INFO CAPTURE
        String deviceName =
                servletRequest.getHeader("User-Agent");

        String ipAddress =
                servletRequest.getRemoteAddr();

        // 12. SAVE DEVICE SESSION
        deviceSessionService.createSession(
                user.getEmail(),
                refreshToken.getToken(),
                deviceName,
                ipAddress
        );

        // 13. AUDIT LOG SUCCESS
        auditLogService.log(
                user.getEmail(),
                AuditAction.LOGIN_SUCCESS
        );

//        14. RESPONSE
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .build();
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {

        RefreshToken existingToken =
                refreshTokenService.findByToken(refreshToken);

        refreshTokenService.verifyExpiration(existingToken);

        User user = existingToken.getUser();

        refreshTokenService.deleteByToken(refreshToken);

        RefreshToken newRefreshToken =
                refreshTokenService.createRefreshToken(
                        user.getEmail()
                );

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(
                        user.getEmail()
                );

        String newAccessToken =
                jwtService.generateAccessToken(
                        userDetails,
                        user.getUserId(),
                        user.getRole().name()
                );

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .build();
    }

    @Override
    public void logout(String refreshToken, String accessToken) {

        refreshTokenService.deleteByToken(refreshToken);

        deviceSessionService.logoutDevice(refreshToken);

        long expiry = jwtService.getAccessTokenExpiration();

        tokenBlacklistService.blacklistToken(
                accessToken,
                expiry
        );

        String email =
                jwtService.extractUsername(accessToken);

        auditLogService.log(
                email,
                AuditAction.LOGOUT
        );
    }

    private void unlockWhenTimeExpired(User user) {

        if (!user.isAccountLocked()) {
            return;
        }

        if (user.getLockTime() == null) {
            return;
        }

        if (user.getLockTime()
                .plusMinutes(AuthConstants.LOCK_DURATION_MINUTES)
                .isBefore(LocalDateTime.now())) {

            user.setAccountLocked(false);
            user.setFailedAttempts(0);
            user.setLockTime(null);

            userRepository.save(user);
        }
    }

    private void increaseFailedAttempts(User user) {

        int attempts = user.getFailedAttempts() + 1;

        user.setFailedAttempts(attempts);

        if (attempts >= AuthConstants.MAX_FAILED_ATTEMPTS) {

            user.setAccountLocked(true);
            user.setLockTime(LocalDateTime.now());

            auditLogService.log(
                    user.getEmail(),
                    AuditAction.ACCOUNT_LOCKED
            );
        }

        userRepository.save(user);
    }

    private void resetFailedAttempts(User user) {

        if (user.getFailedAttempts() > 0) {

            user.setFailedAttempts(0);

            userRepository.save(user);
        }
    }

    @Override
    public void logoutAllDevices(String email) {

        deviceSessionService.removeAllSessions(email);

        refreshTokenService.logoutAllDevices(email);
    }

    @Override
    public void changePassword(
            String email,
            ChangePasswordRequest request
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        // 1. verify old password
        if (!passwordEncoder.matches(
                request.getOldPassword(),
                user.getPassword()
        )) {
            throw new InvalidCredentialsException("Old password is incorrect");
        }

        // 2. check if new password is same as old
        if (passwordEncoder.matches(
                request.getNewPassword(),
                user.getPassword()
        )) {
            throw new IllegalArgumentException("New password cannot be same as old password");
        }

        // 3. update password
        user.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        // 4. OPTIONAL SECURITY HARDENING
        // reset failed attempts
        user.setFailedAttempts(0);
        user.setAccountLocked(false);
        user.setLockTime(null);

        userRepository.save(user);

        // 5. revoke all refresh tokens (force re-login everywhere)
        refreshTokenService.logoutAllDevices(email);

        // 6. blacklist all active JWT sessions (if needed in your system)
        // (optional if you track sessions in Redis/device table)

        // 7. audit log
        auditLogService.log(
                email,
                AuditAction.PASSWORD_CHANGED
        );
    }
}