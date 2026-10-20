package com.innowise.academy.authservice.exception;

public class UserProfileConflictException extends RuntimeException {
    public UserProfileConflictException(String message) {
        super(message);
    }
}
