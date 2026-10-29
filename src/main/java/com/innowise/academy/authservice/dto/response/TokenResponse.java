package com.innowise.academy.authservice.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
