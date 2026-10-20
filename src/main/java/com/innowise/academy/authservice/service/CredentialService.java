package com.innowise.academy.authservice.service;

import com.innowise.academy.authservice.dto.request.RegisterRequest;
import com.innowise.academy.authservice.dto.response.CredentialResponse;
import com.innowise.academy.authservice.entity.CredentialEntity;
import com.innowise.academy.authservice.mapper.CredentialMapper;
import com.innowise.academy.authservice.repository.CredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CredentialService {

    private final CredentialMapper mapper;
    private final CredentialRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public CredentialResponse create(RegisterRequest request , Long userId){
        String passwordHash = passwordEncoder.encode(request.password());
        CredentialEntity credentialEntity = mapper.requestToEntity(request, userId, passwordHash);
        return mapper.entityToResponse(repository.save(credentialEntity));
    }

    public CredentialResponse authenticate(String username, String rawPassword){
        return repository.findByUsername(username)
                .filter(c -> passwordEncoder.matches(rawPassword, c.getPasswordHash()))
                .map(mapper::entityToResponse)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
    }

    public boolean usernameExists(String username) {
        return repository.existsByUsername(username);
    }
}
