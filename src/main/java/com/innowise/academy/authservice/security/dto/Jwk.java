package com.innowise.academy.authservice.security.dto;

public record Jwk(
        String kty,
        String use,
        String alg,
        String kid,
        String n,
        String e
) {
}
