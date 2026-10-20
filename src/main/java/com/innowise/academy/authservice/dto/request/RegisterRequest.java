package com.innowise.academy.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.NonNull;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank
        String username,
        @NotBlank
        @Size(min = 8)
        String password,
        @NotBlank
        String firstname,
        @NotBlank
        String surname,
        @NonNull
        @Past
        LocalDate birthday,
        @NotBlank
        @Email
        String email
        ) {
}
