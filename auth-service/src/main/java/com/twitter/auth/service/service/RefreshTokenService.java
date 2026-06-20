package com.twitter.auth.service.service;

import com.twitter.auth.service.Model.RefreshToken;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(String email);

    RefreshToken verifyExpiration(RefreshToken token);

    RefreshToken findByToken(String token);

    void deleteByToken(String token);

    void logoutAllDevices(String email);
}