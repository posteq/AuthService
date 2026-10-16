package com.innowise.academy.authservice.repository;

import com.innowise.academy.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthRepository extends JpaRepository<User,Long> {
    @Override
    Optional<User> findById(Long id);
    boolean existsByUsername(String username);
}
