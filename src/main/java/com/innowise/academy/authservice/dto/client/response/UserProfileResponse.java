package com.innowise.academy.authservice.dto.client.response;

import java.time.LocalDate;

public record UserProfileResponse(
        Long id,
        String firstname,
        String surname,
        LocalDate birthday,
        String email,
        Boolean active
) {
}
