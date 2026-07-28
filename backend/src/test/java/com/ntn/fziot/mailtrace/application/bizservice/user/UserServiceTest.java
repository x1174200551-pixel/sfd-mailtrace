package com.ntn.fziot.mailtrace.application.bizservice.user;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserCreateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserUpdateRequest;
import com.ntn.fziot.mailtrace.repox.mysql.entity.OperationLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.RoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserRoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.RoleMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private OperationLogMapper operationLogMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PermissionService permissionService;

    private UserService userService;

    private final CurrentUserPrincipal admin = new CurrentUserPrincipal(
            1L, "admin", "管理员", "admin@example.com", "ADMIN");

    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "UserServiceTest.UserEntity");
        TableInfoHelper.initTableInfo(assistant, UserEntity.class);
        userService = new UserService(userMapper, operationLogMapper, roleMapper, userRoleMapper, passwordEncoder, permissionService);
    }

    @Test
    void createUser_shouldInsertPrimaryAndSecondaryUserRoles() {
        UserCreateRequest request = new UserCreateRequest();
        request.setAccount("agent01");
        request.setDisplayName("客服一号");
        request.setEmail("agent01@example.com");
        request.setRoleCode("AGENT");
        request.setRoleCodes(List.of("AGENT", "QUALITY_CHECKER"));
        request.setPassword("agent123");
        request.setEnabled(true);

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("agent123")).thenReturn("encoded");
        when(roleMapper.selectOne(any()))
                .thenReturn(role(20L, "AGENT"))
                .thenReturn(role(30L, "QUALITY_CHECKER"));
        doAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(99L);
            return 1;
        }).when(userMapper).insert(any(UserEntity.class));
        when(userMapper.selectById(99L)).thenReturn(user(99L, "agent01", "AGENT"));
        when(userRoleMapper.selectList(any())).thenReturn(List.of(userRole(20L, true), userRole(30L, false)));
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(role(20L, "AGENT"), role(30L, "QUALITY_CHECKER")));

        userService.createUser(admin, request);

        ArgumentCaptor<UserRoleEntity> userRoleCaptor = ArgumentCaptor.forClass(UserRoleEntity.class);
        verify(userRoleMapper).physicalDeleteByUserId(99L);
        verify(userRoleMapper, times(2)).insert(userRoleCaptor.capture());
        List<UserRoleEntity> userRoles = userRoleCaptor.getAllValues();
        assertEquals(99L, userRoles.get(0).getUserId());
        assertEquals(20L, userRoles.get(0).getRoleId());
        assertTrue(userRoles.get(0).getPrimaryRole());
        assertEquals(30L, userRoles.get(1).getRoleId());
        assertEquals(false, userRoles.get(1).getPrimaryRole());
        assertEquals("admin", userRoles.get(0).getCreatedBy());
    }

    @Test
    void updateUser_shouldUpdatePrimaryUserRole() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setDisplayName("客服一号");
        request.setEmail("agent01@example.com");
        request.setRoleCode("AGENT");
        request.setRoleCodes(List.of("AGENT", "QUALITY_CHECKER"));
        request.setEnabled(true);

        when(userMapper.selectById(99L))
                .thenReturn(user(99L, "agent01", "ADMIN"))
                .thenReturn(user(99L, "agent01", "AGENT"));
        when(roleMapper.selectOne(any()))
                .thenReturn(role(20L, "AGENT"))
                .thenReturn(role(30L, "QUALITY_CHECKER"));
        when(userRoleMapper.selectList(any())).thenReturn(List.of(userRole(20L, true), userRole(30L, false)));
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(role(20L, "AGENT"), role(30L, "QUALITY_CHECKER")));

        userService.updateUser(admin, 99L, request);

        verify(userMapper).update(eq(null), any());
        ArgumentCaptor<UserRoleEntity> userRoleCaptor = ArgumentCaptor.forClass(UserRoleEntity.class);
        verify(userRoleMapper).physicalDeleteByUserId(99L);
        verify(userRoleMapper, times(2)).insert(userRoleCaptor.capture());
        assertEquals(20L, userRoleCaptor.getAllValues().get(0).getRoleId());
        assertTrue(userRoleCaptor.getAllValues().get(0).getPrimaryRole());
        assertEquals(30L, userRoleCaptor.getAllValues().get(1).getRoleId());
        ArgumentCaptor<OperationLogEntity> logCaptor = ArgumentCaptor.forClass(OperationLogEntity.class);
        verify(operationLogMapper).insert(logCaptor.capture());
        assertEquals("UPDATE", logCaptor.getValue().getActionCode());
    }

    private RoleEntity role(Long id, String roleCode) {
        RoleEntity role = new RoleEntity();
        role.setId(id);
        role.setRoleCode(roleCode);
        role.setEnabled(true);
        return role;
    }

    private UserEntity user(Long id, String account, String roleCode) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setAccount(account);
        user.setDisplayName("客服一号");
        user.setEmail("agent01@example.com");
        user.setRoleCode(roleCode);
        user.setEnabled(true);
        return user;
    }

    private UserRoleEntity userRole(Long roleId, Boolean primaryRole) {
        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setRoleId(roleId);
        userRole.setPrimaryRole(primaryRole);
        return userRole;
    }
}
