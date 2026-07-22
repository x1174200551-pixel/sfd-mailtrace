package com.sfonda.mailtrace.application.bizservice.auth;

import lombok.Getter;

@Getter
public class AuthBusinessException extends RuntimeException {

    private final int code;

    public AuthBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
