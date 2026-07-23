package com.ntn.fziot.mailtrace.interfaces.api.user;

import com.ntn.fziot.mailtrace.application.bizservice.user.UserService;
import com.ntn.fziot.mailtrace.infrastructure.basic.BasicResult;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserCreateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserPageResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserResetPasswordRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserUpdateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户管理", description = "用户列表、新建、编辑、启停和重置密码")
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户分页查询")
    @GetMapping
    public BasicResult<UserPageResponse> pageUsers(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return BasicResult.ok(userService.pageUsers(principal, keyword, roleCode, enabled, page, size));
    }

    @Operation(summary = "新建用户")
    @PostMapping
    public BasicResult<UserVO> createUser(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody UserCreateRequest request) {
        return BasicResult.ok(userService.createUser(principal, request));
    }

    @Operation(summary = "编辑用户")
    @PutMapping("/{id}")
    public BasicResult<UserVO> updateUser(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return BasicResult.ok(userService.updateUser(principal, id, request));
    }

    @Operation(summary = "启用或停用用户")
    @PatchMapping("/{id}/enabled")
    public BasicResult<UserVO> updateEnabled(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UserEnabledRequest request) {
        return BasicResult.ok(userService.updateEnabled(principal, id, request));
    }

    @Operation(summary = "重置用户密码")
    @PostMapping("/{id}/reset-password")
    public BasicResult<Void> resetPassword(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UserResetPasswordRequest request) {
        userService.resetPassword(principal, id, request);
        return BasicResult.ok();
    }
}
