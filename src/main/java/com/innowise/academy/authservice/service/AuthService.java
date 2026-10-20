package com.innowise.academy.authservice.service;

import com.innowise.academy.authservice.client.UserServiceClient;
import com.innowise.academy.authservice.dto.client.request.UserProfileRequest;
import com.innowise.academy.authservice.dto.client.response.UserProfileResponse;
import com.innowise.academy.authservice.dto.request.LoginRequest;
import com.innowise.academy.authservice.dto.request.RefreshRequest;
import com.innowise.academy.authservice.dto.request.RegisterRequest;
import com.innowise.academy.authservice.dto.response.CredentialResponse;
import com.innowise.academy.authservice.dto.response.TokenResponse;
import com.innowise.academy.authservice.exception.RegistrationFailedException;
import com.innowise.academy.authservice.exception.UsernameAlreadyExistsException;
import com.innowise.academy.authservice.security.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserServiceClient userClient;
    private final CredentialService credentialService;
    private final JwtService jwtService;

    public CredentialResponse register(RegisterRequest request){
        if (credentialService.usernameExists(request.username())) {
            throw new UsernameAlreadyExistsException(request.username());
        }

        UserProfileResponse profile = userClient.createProfile(UserProfileRequest.from(request));

        try {
            return credentialService.create(request, profile.id());
        }catch(RuntimeException e){
            try {
                userClient.deleteProfile(profile.id());
            } catch (RuntimeException additionalError) {
                e.addSuppressed(additionalError);
            }
            throw new RegistrationFailedException(e.getMessage());
        }
    }

    public TokenResponse login(LoginRequest request){
        CredentialResponse authenticated = credentialService.authenticate(request.username(), request.password());
        return new TokenResponse(jwtService.generateAccessToken(authenticated),
                                jwtService.generateRefreshToken(authenticated));
    }

    public TokenResponse refresh(RefreshRequest request) {
        CredentialResponse credential = jwtService.parseRefreshToken(request.refreshToken());

        return new TokenResponse(jwtService.generateAccessToken(credential), request.refreshToken() );
    }
}
