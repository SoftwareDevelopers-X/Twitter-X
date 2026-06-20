package com.twitter.auth.service.service;

public interface EmailVerificationService {

    String createVerificationToken(String email);

    void confirmToken(String token);
}