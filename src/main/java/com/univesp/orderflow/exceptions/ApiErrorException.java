package com.univesp.orderflow.exceptions;

import org.springframework.http.HttpStatus;

public class ApiErrorException extends RuntimeException {
    private final HttpStatus status;

    public ApiErrorException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
