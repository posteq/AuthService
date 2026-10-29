package com.innowise.academy.authservice.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorMessage> handleBadCredentials() {
        return ResponseEntity.status(UNAUTHORIZED)
                .body(new ErrorMessage(UNAUTHORIZED, "Invalid username or password"));
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorMessage> handleExpiredToken() {
        return ResponseEntity.status(UNAUTHORIZED)
                .body(new ErrorMessage(UNAUTHORIZED,"Token has expired"));
    }

    @ExceptionHandler({JwtException.class, InvalidTokenException.class})
    public ResponseEntity<ErrorMessage> handleInvalidToken() {
        return ResponseEntity.status(UNAUTHORIZED)
                .body(new ErrorMessage(UNAUTHORIZED,"Invalid token"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorMessage> handleAccessDenied() {
        return ResponseEntity.status(FORBIDDEN)
                .body(new ErrorMessage(FORBIDDEN,"Access denied"));
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ErrorMessage> handleUsernameTaken(UsernameAlreadyExistsException ex) {
        return ResponseEntity.status(CONFLICT).body(new ErrorMessage(CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorMessage> handleDataIntegrity() {
        return ResponseEntity.status(CONFLICT).body(new ErrorMessage(CONFLICT, "Account already exists"));
    }

    @ExceptionHandler(UserServiceException.class)
    public ResponseEntity<ErrorMessage> handleUserServiceFailure(UserServiceException ex) {
        return ResponseEntity.status(BAD_GATEWAY)
                .body(new ErrorMessage(BAD_GATEWAY,ex.getMessage()));
    }

    @ExceptionHandler(UserProfileConflictException.class)
    public ResponseEntity<ErrorMessage> handleUserProfileConflictFailure(UserProfileConflictException ex) {
        return ResponseEntity.status(CONFLICT)
                .body(new ErrorMessage(CONFLICT,ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class,IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorMessage> handleNotValid(Exception e){
        return ResponseEntity.status(BAD_REQUEST)
                .body(new ErrorMessage(BAD_REQUEST,e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleUnexpected(Exception e) {
        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(new ErrorMessage(INTERNAL_SERVER_ERROR,e.getMessage()));
    }

}
