package com.innowise.academy.authservice.security.service;

import com.innowise.academy.authservice.security.dto.Jwk;
import com.innowise.academy.authservice.security.dto.JwkSet;
import com.innowise.academy.authservice.security.util.JwtKeys;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Service
@Getter
public class JwksService {

    private final JwkSet jwkSet;

    public JwksService(JwtKeys keys) {
        RSAPublicKey publicKey = keys.getPublicKey();
        Jwk jwk = new Jwk("RSA", "sig", "RS256", keys.getKid(),
                toBase64Url(publicKey.getModulus()),
                toBase64Url(publicKey.getPublicExponent()));
        this.jwkSet = new JwkSet(List.of(jwk));
    }

    static String toBase64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if(bytes.length > 1 && bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

}
