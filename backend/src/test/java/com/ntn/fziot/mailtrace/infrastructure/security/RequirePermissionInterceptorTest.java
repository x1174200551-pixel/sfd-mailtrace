package com.ntn.fziot.mailtrace.infrastructure.security;

import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequirePermissionInterceptorTest {

    private static final CurrentUserPrincipal PRINCIPAL =
            new CurrentUserPrincipal(1L, "agent", "Agent", "agent@example.com", "AGENT");

    @Mock
    private PermissionService permissionService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void preHandle_whenNoAnnotation_shouldAllowWithoutPermissionLookup() throws Exception {
        RequirePermissionInterceptor interceptor = new RequirePermissionInterceptor(permissionService);

        assertTrue(interceptor.preHandle(null, null, handler(NoAnnotationController.class, "list")));

        verify(permissionService, never()).hasPermission(PRINCIPAL, "ticket:read");
    }

    @Test
    void preHandle_whenMethodPermissionAllowed_shouldAllow() throws Exception {
        login();
        when(permissionService.hasPermission(PRINCIPAL, "ticket:read")).thenReturn(true);
        RequirePermissionInterceptor interceptor = new RequirePermissionInterceptor(permissionService);

        assertTrue(interceptor.preHandle(null, null, handler(MethodAnnotationController.class, "list")));

        verify(permissionService).hasPermission(PRINCIPAL, "ticket:read");
    }

    @Test
    void preHandle_whenAnyOfPermissionAllowed_shouldAllow() throws Exception {
        login();
        when(permissionService.hasPermission(PRINCIPAL, "ticket:reply")).thenReturn(false);
        when(permissionService.hasPermission(PRINCIPAL, "ticket:note")).thenReturn(true);
        RequirePermissionInterceptor interceptor = new RequirePermissionInterceptor(permissionService);

        assertTrue(interceptor.preHandle(null, null, handler(MethodAnnotationController.class, "reply")));

        verify(permissionService).hasPermission(PRINCIPAL, "ticket:reply");
        verify(permissionService).hasPermission(PRINCIPAL, "ticket:note");
    }

    @Test
    void preHandle_whenClassPermissionAllowed_shouldAllow() throws Exception {
        login();
        when(permissionService.hasPermission(PRINCIPAL, "role:read")).thenReturn(true);
        RequirePermissionInterceptor interceptor = new RequirePermissionInterceptor(permissionService);

        assertTrue(interceptor.preHandle(null, null, handler(ClassAnnotationController.class, "list")));

        verify(permissionService).hasPermission(PRINCIPAL, "role:read");
    }

    @Test
    void preHandle_whenNoLogin_shouldReject() throws Exception {
        RequirePermissionInterceptor interceptor = new RequirePermissionInterceptor(permissionService);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> interceptor.preHandle(null, null, handler(MethodAnnotationController.class, "list")));

        assertEquals(40302, exception.getCode());
        assertEquals("未登录", exception.getMessage());
    }

    @Test
    void preHandle_whenPermissionDenied_shouldRejectWithAnnotationMessage() throws Exception {
        login();
        when(permissionService.hasPermission(PRINCIPAL, "ticket:read")).thenReturn(false);
        RequirePermissionInterceptor interceptor = new RequirePermissionInterceptor(permissionService);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> interceptor.preHandle(null, null, handler(MethodAnnotationController.class, "list")));

        assertEquals(40302, exception.getCode());
        assertEquals("无权查看工单", exception.getMessage());
    }

    private void login() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(PRINCIPAL, null, null));
    }

    private HandlerMethod handler(Class<?> controllerType, String methodName) throws Exception {
        Object bean = controllerType.getDeclaredConstructor().newInstance();
        Method method = controllerType.getDeclaredMethod(methodName);
        return new HandlerMethod(bean, method);
    }

    static class NoAnnotationController {
        void list() {
        }
    }

    static class MethodAnnotationController {
        @RequirePermission(value = "ticket:read", message = "无权查看工单")
        void list() {
        }

        @RequirePermission(anyOf = {"ticket:reply", "ticket:note"}, message = "无权回复或备注工单")
        void reply() {
        }
    }

    @RequirePermission(value = "role:read", message = "无权查看角色")
    static class ClassAnnotationController {
        void list() {
        }
    }
}
