package com.sfonda.mailtrace.interfaces.api.auth;

import com.sfonda.mailtrace.application.bizservice.auth.AuthService;
import com.sfonda.mailtrace.infrastructure.basic.BasicResult;
import com.sfonda.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.sfonda.mailtrace.interfaces.vo.auth.CurrentUserVO;
import com.sfonda.mailtrace.interfaces.vo.auth.LoginRequest;
import com.sfonda.mailtrace.interfaces.vo.auth.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证", description = "登录、当前用户与退出登录")
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录", description = "默认开发账号 admin / admin123")
    @PostMapping("/login")
    public BasicResult<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return BasicResult.ok(authService.login(request));
    }

    @Operation(summary = "当前用户", description = "根据 Bearer Token 返回当前登录用户")
    @GetMapping("/me")
    public BasicResult<CurrentUserVO> me(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return BasicResult.ok(authService.currentUser(principal));
    }

    @Operation(summary = "退出登录", description = "第一版无状态退出，前端清理 Token")
    @PostMapping("/logout")
    public BasicResult<Void> logout() {
        return BasicResult.ok();
    }
}
