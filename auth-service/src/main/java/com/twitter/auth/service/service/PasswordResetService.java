package com.twitter.auth.service.service;

public interface PasswordResetService {

    String createResetToken(String email);

    void resetPassword(String token, String newPassword);
}