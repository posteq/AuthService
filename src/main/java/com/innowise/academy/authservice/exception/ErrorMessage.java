package com.innowise.academy.authservice.exception;

import org.springframework.http.HttpStatus;

public record ErrorMessage (HttpStatus status,String message){
}
