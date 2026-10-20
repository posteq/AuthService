package com.innowise.academy.authservice.dto.response;

import com.innowise.academy.authservice.entity.Role;

public record CredentialResponse(
        Long userId,
        String username,
        Role role
) {
}
