package com.innowise.academy.authservice.security.service;

import com.innowise.academy.authservice.dto.response.CredentialResponse;
import com.innowise.academy.authservice.entity.Role;
import com.innowise.academy.authservice.exception.InvalidTokenException;
import com.innowise.academy.authservice.security.util.JwtKeys;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";

    private final JwtKeys keys;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;
    private final JwtParser parser;

    public JwtService(JwtKeys keys,
                      @Value("${app.security.jwt.access-token-ttl}")Duration accessTokenTtl,
                      @Value("${app.security.jwt.refresh-token-ttl}") Duration refreshTokenTtl) {

        this.keys = keys;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
        this.parser = Jwts.parser().verifyWith(keys.getPublicKey()).build();
    }

    public String generateAccessToken(CredentialResponse credential) {
        return buildToken(credential,ACCESS,accessTokenTtl);
    }

    public String generateRefreshToken(CredentialResponse credential) {
        return buildToken(credential,REFRESH,refreshTokenTtl);
    }

    public String generateServiceToken() {
        return buildToken(new CredentialResponse(null, "auth-service", Role.ADMIN), ACCESS, Duration.ofMinutes(1));
    }

    private String buildToken(CredentialResponse credential, String type, Duration accessTokenTtl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .header().keyId(keys.getKid()).and()
                .subject(credential.username())
                .claim("user_id",credential.userId())
                .claim("role",credential.role())
                .claim("type",type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(keys.getPrivateKey(),Jwts.SIG.RS256)
                .compact();
    }

    public CredentialResponse parseRefreshToken(String token) {
        Claims claims = parser.parseSignedClaims(token).getPayload();
        if(!REFRESH.equals(claims.get("type",String.class))) {
            throw new InvalidTokenException("Invalid type of token");
        }
        return new CredentialResponse(
                claims.get("user_id",Number.class).longValue(),
                claims.getSubject(),
                Role.valueOf(claims.get("role",String.class)));
    }
}
