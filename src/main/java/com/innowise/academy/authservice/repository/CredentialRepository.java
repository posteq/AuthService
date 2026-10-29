package com.innowise.academy.authservice.repository;

import com.innowise.academy.authservice.entity.CredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CredentialRepository extends JpaRepository<CredentialEntity,Long> {
    Optional<CredentialEntity> findByUsername(String Username);
    boolean existsByUsername(String username);
}
