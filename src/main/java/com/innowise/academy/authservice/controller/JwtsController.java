package com.innowise.academy.authservice.controller;

import com.innowise.academy.authservice.security.dto.JwkSet;
import com.innowise.academy.authservice.security.service.JwksService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class JwtsController {
    private final JwksService jwksService;

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<JwkSet> jwks(){
        return ResponseEntity.ok()
                .body(jwksService.getJwkSet());
    }
}
