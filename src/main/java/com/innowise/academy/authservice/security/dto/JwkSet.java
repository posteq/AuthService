package com.innowise.academy.authservice.security.dto;

import java.util.List;

public record JwkSet(
        List<Jwk> keys
) {
}
