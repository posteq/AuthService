package com.innowise.academy.authservice.service;


import com.innowise.academy.authservice.dto.response.CredentialResponse;
import com.innowise.academy.authservice.entity.Role;
import com.innowise.academy.authservice.exception.InvalidTokenException;
import com.innowise.academy.authservice.security.service.JwtService;
import com.innowise.academy.authservice.security.util.JwtKeys;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final CredentialResponse ADMIN_ANN = new CredentialResponse(42L, "ann", Role.ADMIN);
    private static final CredentialResponse USER_ANN = new CredentialResponse(42L, "ann", Role.USER);

    private static JwtKeys keys;

    @BeforeAll
    static void generateKeys() {
        keys = new JwtKeys();
    }

    private static JwtService service() {
        return new JwtService(keys, Duration.ofMinutes(15), Duration.ofDays(7));
    }

    @Test
    void accessToken_isSignedWithRs256_andContainsUserIdRoleAndType() {
        String token = service().generateAccessToken(ADMIN_ANN);


        Jws<Claims> jws = Jwts.parser().verifyWith(keys.getPublicKey()).build().parseSignedClaims(token);

        Claims claims = jws.getPayload();

        assertThat(jws.getHeader().getAlgorithm()).isEqualTo("RS256");
        assertThat(jws.getHeader().getKeyId()).isEqualTo(keys.getKid());
        assertThat(claims.getSubject()).isEqualTo("ann");
        assertThat(claims.get("user_id", Number.class).longValue()).isEqualTo(42L);
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
        assertThat(claims.getExpiration().toInstant())
                .isEqualTo(claims.getIssuedAt().toInstant().plus(Duration.ofMinutes(15)));
    }

    @Test
    void refreshToken_livesLongerThanAccessToken() {
        String token = service().generateRefreshToken(ADMIN_ANN);

        Claims claims = Jwts.parser().verifyWith(keys.getPublicKey()).build().parseSignedClaims(token).getPayload();

        assertThat(claims.get("type", String.class)).isEqualTo("refresh");
        assertThat(claims.getExpiration().toInstant())
                .isEqualTo(claims.getIssuedAt().toInstant().plus(Duration.ofDays(7)));
    }

    @Test
    void parseRefreshToken_validToken_returnsWhoItWasIssuedFor() {
        String token = service().generateRefreshToken(ADMIN_ANN);

        assertThat(service().parseRefreshToken(token)).isEqualTo(ADMIN_ANN);
    }

    @Test
    void parseRefreshToken_accessToken_isRejected() {
        String accessToken = service().generateAccessToken(ADMIN_ANN);

        assertThatThrownBy(() -> service().parseRefreshToken(accessToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void parseRefreshToken_expiredToken_throwsExpiredJwtException() {
        JwtService expiring = new JwtService(keys, Duration.ofMinutes(15), Duration.ofSeconds(-10));
        String token = expiring.generateRefreshToken(ADMIN_ANN);

        assertThatThrownBy(() -> expiring.parseRefreshToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void parseRefreshToken_changedPayload_isRejected() {
        String token = service().generateRefreshToken(USER_ANN);
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String forgedPayload = payload.replace("\"USER\"", "\"ADMIN\"");

        String forgedToken = parts[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(forgedPayload.getBytes(StandardCharsets.UTF_8))
                + "." + parts[2];

        assertThat(forgedPayload).isNotEqualTo(payload);
        assertThatThrownBy(() -> service().parseRefreshToken(forgedToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void parseRefreshToken_tokenSignedWithAnotherKey_isRejected() {
        JwtService attacker = new JwtService(new JwtKeys(), Duration.ofMinutes(15), Duration.ofDays(7));
        String forged = attacker.generateRefreshToken(ADMIN_ANN);

        assertThatThrownBy(() -> service().parseRefreshToken(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void parseRefreshToken_garbage_isRejected() {
        assertThatThrownBy(() -> service().parseRefreshToken("not-a-jwt")).isInstanceOf(JwtException.class);
    }
}
