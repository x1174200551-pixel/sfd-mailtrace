package com.ntn.fziot.mailtrace.application.bizservice.auth;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.infrastructure.security.JwtTokenService;
import com.ntn.fziot.mailtrace.interfaces.vo.auth.CurrentUserVO;
import com.ntn.fziot.mailtrace.interfaces.vo.auth.LoginRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.auth.LoginResponse;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private AuthService authService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "AuthServiceTest.UserEntity");
        TableInfoHelper.initTableInfo(assistant, UserEntity.class);
    }

    @Test
    void login_shouldReturnLegacyRoleCodeAndPermissionLists() {
        UserEntity user = user(1L, "admin", "ADMIN");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("admin123", "hash")).thenReturn(true);
        when(jwtTokenService.createToken(user)).thenReturn("token-1");
        when(jwtTokenService.getExpiresInSeconds()).thenReturn(7200L);
        when(permissionService.getUserPermissions(1L, "ADMIN")).thenReturn(permissionContext(1L));

        LoginResponse response = authService.login(loginRequest(" admin ", " admin123 "));

        assertEquals("token-1", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(7200L, response.getExpiresIn());
        assertEquals("ADMIN", response.getUser().getRoleCode());
        assertTrue(response.getUser().getRoles().contains("ADMIN"));
        assertTrue(response.getUser().getPermissions().contains("ticket:read"));
        assertTrue(response.getUser().getDataScopes().get("TICKET").contains("ALL"));
        verify(userMapper).update(any(), any());
    }

    @Test
    void currentUser_shouldReturnPermissionLists() {
        CurrentUserPrincipal principal = new CurrentUserPrincipal(
                2L, "agent", "处理人", "agent@example.com", "AGENT");
        when(permissionService.getCurrentPermissions(principal)).thenReturn(new PermissionService.PermissionContext(
                2L,
                Set.of("AGENT"),
                Set.of("menu:tickets", "ticket:claim"),
                Map.of("TICKET", Set.of("SELF"))
        ));

        CurrentUserVO currentUser = authService.currentUser(principal);

        assertEquals(2L, currentUser.getId());
        assertEquals("AGENT", currentUser.getRoleCode());
        assertTrue(currentUser.getRoles().contains("AGENT"));
        assertTrue(currentUser.getPermissions().contains("ticket:claim"));
        assertTrue(currentUser.getDataScopes().get("TICKET").contains("SELF"));
    }

    private PermissionService.PermissionContext permissionContext(Long userId) {
        return new PermissionService.PermissionContext(
                userId,
                Set.of("ADMIN"),
                Set.of("menu:tickets", "ticket:read"),
                Map.of("TICKET", Set.of("ALL"))
        );
    }

    private LoginRequest loginRequest(String account, String password) {
        LoginRequest request = new LoginRequest();
        request.setAccount(account);
        request.setPassword(password);
        return request;
    }

    private UserEntity user(Long id, String account, String roleCode) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setAccount(account);
        user.setPasswordHash("hash");
        user.setDisplayName("系统管理员");
        user.setEmail("admin@example.com");
        user.setRoleCode(roleCode);
        user.setEnabled(true);
        return user;
    }
}
