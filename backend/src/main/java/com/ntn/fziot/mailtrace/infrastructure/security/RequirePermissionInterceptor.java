package com.ntn.fziot.mailtrace.infrastructure.security;

import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class RequirePermissionInterceptor implements HandlerInterceptor {

    private static final int CODE_FORBIDDEN = 40302;

    private final PermissionService permissionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequirePermission annotation = permissionAnnotation(handlerMethod);
        if (annotation == null) {
            return true;
        }

        CurrentUserPrincipal principal = currentPrincipal();
        if (principal == null) {
            throw new BusinessException(CODE_FORBIDDEN, "未登录");
        }

        String[] anyOf = annotation.anyOf().length > 0 ? annotation.anyOf() : new String[]{annotation.value()};
        boolean allowed = Arrays.stream(anyOf)
                .map(code -> code == null ? "" : code.trim())
                .filter(code -> !code.isEmpty())
                .anyMatch(code -> permissionService.hasPermission(principal, code));
        if (!allowed) {
            throw new BusinessException(CODE_FORBIDDEN, annotation.message());
        }
        return true;
    }

    private RequirePermission permissionAnnotation(HandlerMethod handlerMethod) {
        RequirePermission methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), RequirePermission.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequirePermission.class);
    }

    private CurrentUserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUserPrincipal principal)) {
            return null;
        }
        return principal;
    }
}
