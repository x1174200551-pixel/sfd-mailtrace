package com.ntn.fziot.mailtrace.application.bizservice.auth;

import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;

public class AuthBusinessException extends BusinessException {

    public AuthBusinessException(int code, String message) {
        super(code, message);
    }
}
