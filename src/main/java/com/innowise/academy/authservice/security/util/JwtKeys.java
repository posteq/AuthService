package com.innowise.academy.authservice.security.util;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Component
@Getter
public class JwtKeys {

    private final String kid = UUID.randomUUID().toString();
    private final RSAPublicKey publicKey;
    private final PrivateKey privateKey;

    public JwtKeys() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            publicKey =(RSAPublicKey) keyPair.getPublic();
            privateKey = keyPair.getPrivate();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
