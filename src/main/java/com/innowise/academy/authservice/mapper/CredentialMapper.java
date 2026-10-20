package com.innowise.academy.authservice.mapper;

import com.innowise.academy.authservice.dto.request.RegisterRequest;
import com.innowise.academy.authservice.dto.response.CredentialResponse;
import com.innowise.academy.authservice.entity.CredentialEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CredentialMapper {
    @Mapping(target = "role",constant = "USER")
    @Mapping(target = "id",ignore = true)
    @Mapping(target = "createdAt",ignore = true)
    @Mapping(target = "updatedAt",ignore = true)
    CredentialEntity requestToEntity(RegisterRequest request, Long userId, String passwordHash);


    CredentialResponse entityToResponse(CredentialEntity entity);
}
