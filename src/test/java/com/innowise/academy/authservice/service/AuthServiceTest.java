package com.innowise.academy.authservice.service;

import com.innowise.academy.authservice.client.UserServiceClient;
import com.innowise.academy.authservice.dto.client.request.UserProfileRequest;
import com.innowise.academy.authservice.dto.request.LoginRequest;
import com.innowise.academy.authservice.dto.request.RefreshRequest;
import com.innowise.academy.authservice.dto.request.RegisterRequest;
import com.innowise.academy.authservice.dto.response.CredentialResponse;
import com.innowise.academy.authservice.dto.response.TokenResponse;
import com.innowise.academy.authservice.entity.Role;
import com.innowise.academy.authservice.exception.InvalidTokenException;
import com.innowise.academy.authservice.exception.UserServiceException;
import com.innowise.academy.authservice.exception.UsernameAlreadyExistsException;
import com.innowise.academy.authservice.security.service.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final RegisterRequest REGISTER = new RegisterRequest("ann", "Password1!",
            "Ann", "Lee", LocalDate.of(1990, 1, 1), "ann@mail.com");
    private static final UserProfileRequest PROFILE = UserProfileRequest.from(REGISTER);
    private static final CredentialResponse ANN = new CredentialResponse(42L, "ann", Role.USER);

    @Mock
    private CredentialService credentialService;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void success_createsProfileFirst_thenSavesCredentials() {
        when(credentialService.usernameExists("ann"))
                .thenReturn(false);
        when(userServiceClient.createUser(PROFILE))
                .thenReturn(42L);
        when(credentialService.create(REGISTER, 42L)).thenReturn(ANN);

        assertThat(authService.register(REGISTER)).isEqualTo(ANN);

        verify(userServiceClient, never()).deleteUser(any());
    }

    @Test
    void usernameTaken_nothingIsCalled() {
        when(credentialService.usernameExists("ann"))
                .thenReturn(true);

        assertThatThrownBy(() -> authService.register(REGISTER))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verifyNoInteractions(userServiceClient);
        verify(credentialService, never()).create(any(), any());
    }

    @Test
    void userServiceFails_nothingToUndo() {
        when(credentialService.usernameExists("ann"))
                .thenReturn(false);
        when(userServiceClient.createUser(PROFILE))
                .thenThrow(new UserServiceException("down"));

        assertThatThrownBy(() -> authService.register(REGISTER)).isInstanceOf(UserServiceException.class);

        verify(credentialService, never()).create(any(), any());
        verify(userServiceClient, never()).deleteUser(any());
    }

    @Test
    void savingCredentialsFails_profileIsDeleted_andTheOriginalErrorIsThrown() {
        when(credentialService.usernameExists("ann"))
                .thenReturn(false);
        when(userServiceClient.createUser(PROFILE))
                .thenReturn(42L);
        when(credentialService.create(REGISTER, 42L)).thenThrow(new IllegalStateException("db is down"));

        assertThatThrownBy(() -> authService.register(REGISTER))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db is down");

        verify(userServiceClient).deleteUser(42L);
    }

    @Test
    void compensationFails_clientStillGetsTheOriginalError() {
        when(credentialService.usernameExists("ann"))
                .thenReturn(false);
        when(userServiceClient.createUser(PROFILE))
                .thenReturn(42L);
        when(credentialService.create(REGISTER, 42L))
                .thenThrow(new IllegalStateException("db is down"));
        doThrow(new UserServiceException("down")).when(userServiceClient).deleteUser(42L);

        assertThatThrownBy(() -> authService.register(REGISTER))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db is down");
    }

    @Test
    void correctPassword_returnsAccessAndRefreshTokens() {
        when(credentialService.authenticate("ann", "Password1!"))
                .thenReturn(ANN);
        when(jwtService.generateAccessToken(ANN)).thenReturn("access");
        when(jwtService.generateRefreshToken(ANN)).thenReturn("refresh");

        TokenResponse response = authService.login(new LoginRequest("ann", "Password1!"));

        assertThat(response).isEqualTo(new TokenResponse("access", "refresh"));
    }

    @Test
    void wrongPassword_noTokensAreCreated() {
        when(credentialService.authenticate("ann", "wrong"))
                .thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ann", "wrong")))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(jwtService);
    }

    @Test
    void validRefreshToken_returnsNewAccessToken() {
        when(jwtService.parseRefreshToken("refresh"))
                .thenReturn(ANN);
        when(jwtService.generateAccessToken(ANN))
                .thenReturn("new-access");

        TokenResponse response = authService.refresh(new RefreshRequest("refresh"));

        assertThat(response).isEqualTo(new TokenResponse("new-access", "refresh"));
    }

    @Test
    void invalidRefreshToken_noAccessTokenIsCreated() {
        when(jwtService.parseRefreshToken("access-token"))
                .thenThrow(new InvalidTokenException("Not a refresh token"));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("access-token")))
                .isInstanceOf(InvalidTokenException.class);

        verify(jwtService, never()).generateAccessToken(any());
    }
}
