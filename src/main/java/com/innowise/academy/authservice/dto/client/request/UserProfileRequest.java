package com.innowise.academy.authservice.dto.client.request;

import com.innowise.academy.authservice.dto.request.RegisterRequest;

import java.time.LocalDate;

public record UserProfileRequest(
        String firstname,
        String surname,
        LocalDate birthday,
        String email
) {
    public static UserProfileRequest from(RegisterRequest request) {
        return new UserProfileRequest(request.firstname(), request.surname(), request.birthday(), request.email());
    }
}
