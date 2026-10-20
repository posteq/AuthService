package com.innowise.academy.authservice.entity;

import com.innowise.academy.authservice.entity.audition.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "credentials")
public class CredentialEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "user_id",unique = true,nullable = false)
    private Long userId;
    @Column(name = "username",unique = true,nullable = false)
    private String username;
    @Column(name = "password",nullable = false)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role = Role.USER;

}
