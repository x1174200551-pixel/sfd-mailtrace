package com.ntn.fziot.mailtrace.interfaces.api.common;

import com.ntn.fziot.mailtrace.application.bizservice.auth.AuthBusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.infrastructure.basic.BasicResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_UNAUTHORIZED_MIN = 40100;
    private static final int CODE_FORBIDDEN_MIN = 40300;
    private static final int CODE_NOT_FOUND_MIN = 40400;
    private static final int CODE_CONFLICT_MIN = 40900;

    @ExceptionHandler(AuthBusinessException.class)
    public ResponseEntity<BasicResult<Void>> handleAuthBusinessException(AuthBusinessException exception) {
        return handleBusinessException(exception);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BasicResult<Void>> handleBusinessException(BusinessException exception) {
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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BasicResult<Void>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(BasicResult.fail(CODE_BAD_REQUEST,
                        "参数格式错误：" + exception.getName() + " 需要 " +
                                (exception.getRequiredType() != null ? exception.getRequiredType().getSimpleName() : "正确格式")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BasicResult<Void>> handleException(Exception exception) {
        log.error("未捕获的运行时异常", exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BasicResult.fail(50000, "服务器繁忙，请稍后重试"));
    }

    private HttpStatus resolveStatus(int code) {
        if (code >= CODE_UNAUTHORIZED_MIN && code < CODE_UNAUTHORIZED_MIN + 100) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code >= CODE_FORBIDDEN_MIN && code < CODE_FORBIDDEN_MIN + 100) {
            return HttpStatus.FORBIDDEN;
        }
        if (code >= CODE_NOT_FOUND_MIN && code < CODE_NOT_FOUND_MIN + 100) {
            return HttpStatus.NOT_FOUND;
        }
        if (code >= CODE_CONFLICT_MIN && code < CODE_CONFLICT_MIN + 100) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
