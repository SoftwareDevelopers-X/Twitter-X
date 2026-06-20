package com.twitter.auth.service.dto;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String refreshToken;
}