package com.innowise.academy.authservice.exception;

import org.springframework.http.HttpStatus;

public record ErrorMessage (int status,String message){
    public ErrorMessage (HttpStatus status , String message){
        this(status.value(),message);
    }
}
