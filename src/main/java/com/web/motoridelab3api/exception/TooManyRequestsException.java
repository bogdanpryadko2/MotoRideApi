package com.web.motoridelab3api.exception;

public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException() {
        super("Too many attempts; try again later");
    }
}
