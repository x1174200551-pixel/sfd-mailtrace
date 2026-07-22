package com.sfonda.mailtrace.interfaces.api.common;

import com.sfonda.mailtrace.application.bizservice.auth.AuthBusinessException;
import com.sfonda.mailtrace.infrastructure.basic.BasicResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_BAD_CREDENTIALS = 40101;
    private static final int CODE_ACCOUNT_DISABLED = 40301;

    @ExceptionHandler(AuthBusinessException.class)
    public ResponseEntity<BasicResult<Void>> handleAuthBusinessException(AuthBusinessException exception) {
        return ResponseEntity
                .status(resolveStatus(exception.getCode()))
                .body(BasicResult.fail(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BasicResult<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("请求参数不正确");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(BasicResult.fail(CODE_BAD_REQUEST, message));
    }

    private HttpStatus resolveStatus(int code) {
        if (code == CODE_BAD_CREDENTIALS) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code == CODE_ACCOUNT_DISABLED) {
            return HttpStatus.FORBIDDEN;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
