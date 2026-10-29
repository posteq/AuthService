package com.innowise.academy.authservice.service;

import com.innowise.academy.authservice.dto.request.RegisterRequest;
import com.innowise.academy.authservice.dto.response.CredentialResponse;
import com.innowise.academy.authservice.entity.CredentialEntity;
import com.innowise.academy.authservice.entity.Role;
import com.innowise.academy.authservice.mapper.CredentialMapper;
import com.innowise.academy.authservice.repository.CredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialServiceTest {

    private static final RegisterRequest REQUEST = new RegisterRequest("ann", "Password1!",
            "Ann", "Lee", LocalDate.of(1990, 1, 1), "ann@mail.com");

    @Mock
    private CredentialRepository repository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private CredentialService service;

    @BeforeEach
    void setUp() {
        service = new CredentialService(Mappers.getMapper(CredentialMapper.class),repository, passwordEncoder);
    }

    private CredentialEntity savedEntity() {
        ArgumentCaptor<CredentialEntity> captor = ArgumentCaptor.forClass(CredentialEntity.class);

        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void create_storesBcryptHashInsteadOfThePassword_andRoleUser() {
        when(repository.save(any(CredentialEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CredentialResponse response = service.create(REQUEST, 42L);

        CredentialEntity saved = savedEntity();

        assertThat(saved.getUserId()).isEqualTo(42L);
        assertThat(saved.getUsername()).isEqualTo("ann");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.getPasswordHash()).isNotEqualTo("Password1!").startsWith("$2");
        assertThat(passwordEncoder.matches("Password1!", saved.getPasswordHash())).isTrue();
        assertThat(response).isEqualTo(new CredentialResponse(42L, "ann", Role.USER));
    }

    @Test
    void create_samePasswordTwice_givesDifferentHashes() {
        when(repository.save(any(CredentialEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(REQUEST, 1L);
        service.create(REQUEST, 2L);

        ArgumentCaptor<CredentialEntity> captor = ArgumentCaptor.forClass(CredentialEntity.class);

        verify(repository, times(2)).save(captor.capture());

        List<CredentialEntity> saved = captor.getAllValues();

        assertThat(saved).hasSize(2);

        assertThat(saved.get(0).getPasswordHash()).isNotEqualTo(saved.get(1).getPasswordHash());
    }

    @Test
    void usernameExists_asksTheRepository() {
        when(repository.existsByUsername("ann")).thenReturn(true);
        when(repository.existsByUsername("bob")).thenReturn(false);

        assertThat(service.usernameExists("ann")).isTrue();
        assertThat(service.usernameExists("bob")).isFalse();
    }

    @Test
    void authenticate_correctPassword_returnsTheCredential() {
        CredentialEntity entity = new CredentialEntity();
        entity.setUserId(42L);
        entity.setUsername("ann");
        entity.setPasswordHash(passwordEncoder.encode("Password1!"));
        entity.setRole(Role.ADMIN);
        when(repository.findByUsername("ann")).thenReturn(Optional.of(entity));

        assertThat(service.authenticate("ann", "Password1!"))
                .isEqualTo(new CredentialResponse(42L, "ann", Role.ADMIN));
    }

    @Test
    void authenticate_wrongPassword_throwsBadCredentials() {
        CredentialEntity entity = new CredentialEntity();
        entity.setPasswordHash(passwordEncoder.encode("Password1!"));
        when(repository.findByUsername("ann")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.authenticate("ann", "WrongPassword"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void authenticate_unknownUser_throwsTheSameBadCredentials() {
        when(repository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate("ghost", "Password1!"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");
    }
}
