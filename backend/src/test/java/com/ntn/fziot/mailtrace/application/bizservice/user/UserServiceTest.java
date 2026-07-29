package com.ntn.fziot.mailtrace.application.bizservice.user;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ntn.fziot.mailtrace.application.bizservice.security.OperationLogService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserCreateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserUpdateRequest;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.repox.mysql.entity.DepartmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.RoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserDepartmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserRoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.DepartmentMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.RoleMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserDepartmentMapper;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private OperationLogService operationLogService;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private DepartmentMapper departmentMapper;
    @Mock
    private UserDepartmentMapper userDepartmentMapper;
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
        userService = new UserService(
                userMapper,
                operationLogService,
                roleMapper,
                userRoleMapper,
                departmentMapper,
                userDepartmentMapper,
                passwordEncoder,
                permissionService
        );
    }

    @Test
    void createUser_shouldInsertSingleRoleAndPrimaryDepartment() {
        UserCreateRequest request = new UserCreateRequest();
        request.setAccount("agent01");
        request.setDisplayName("客服一号");
        request.setEmail("agent01@example.com");
        request.setRoleCode("AGENT");
        request.setRoleCodes(List.of("AGENT"));
        request.setDepartmentId(10L);
        request.setPassword("agent123");
        request.setEnabled(true);

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("agent123")).thenReturn("encoded");
        when(roleMapper.selectOne(any())).thenReturn(role(20L, "AGENT"));
        when(departmentMapper.selectById(10L)).thenReturn(department(10L, "客服部"));
        doAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(99L);
            return 1;
        }).when(userMapper).insert(any(UserEntity.class));
        when(userMapper.selectById(99L)).thenReturn(user(99L, "agent01", "AGENT"));
        when(userRoleMapper.selectList(any())).thenReturn(List.of(userRole(20L, true)));
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(role(20L, "AGENT")));
        when(userDepartmentMapper.selectList(any())).thenReturn(List.of(userDepartment(10L)));

        userService.createUser(admin, request);

        ArgumentCaptor<UserRoleEntity> userRoleCaptor = ArgumentCaptor.forClass(UserRoleEntity.class);
        verify(userRoleMapper).physicalDeleteByUserId(99L);
        verify(userRoleMapper).insert(userRoleCaptor.capture());
        List<UserRoleEntity> userRoles = userRoleCaptor.getAllValues();
        assertEquals(99L, userRoles.get(0).getUserId());
        assertEquals(20L, userRoles.get(0).getRoleId());
        assertTrue(userRoles.get(0).getPrimaryRole());
        assertEquals("admin", userRoles.get(0).getCreatedBy());
        ArgumentCaptor<UserDepartmentEntity> userDepartmentCaptor = ArgumentCaptor.forClass(UserDepartmentEntity.class);
        verify(userDepartmentMapper).physicalDeleteByUserId(99L);
        verify(userDepartmentMapper).insert(userDepartmentCaptor.capture());
        assertEquals(10L, userDepartmentCaptor.getValue().getDepartmentId());
        assertTrue(userDepartmentCaptor.getValue().getPrimaryDepartment());
    }

    @Test
    void updateUser_shouldUpdateSingleRoleAndPrimaryDepartment() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setDisplayName("客服一号");
        request.setEmail("agent01@example.com");
        request.setRoleCode("AGENT");
        request.setRoleCodes(List.of("AGENT"));
        request.setDepartmentId(10L);
        request.setEnabled(true);

        when(userMapper.selectById(99L))
                .thenReturn(user(99L, "agent01", "ADMIN"))
                .thenReturn(user(99L, "agent01", "AGENT"));
        when(roleMapper.selectOne(any())).thenReturn(role(20L, "AGENT"));
        when(departmentMapper.selectById(10L)).thenReturn(department(10L, "客服部"));
        when(userRoleMapper.selectList(any())).thenReturn(List.of(userRole(20L, true)));
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(role(20L, "AGENT")));
        when(userDepartmentMapper.selectList(any())).thenReturn(List.of(userDepartment(10L)));

        userService.updateUser(admin, 99L, request);

        verify(userMapper).update(eq(null), any());
        ArgumentCaptor<UserRoleEntity> userRoleCaptor = ArgumentCaptor.forClass(UserRoleEntity.class);
        verify(userRoleMapper).physicalDeleteByUserId(99L);
        verify(userRoleMapper).insert(userRoleCaptor.capture());
        assertEquals(20L, userRoleCaptor.getAllValues().get(0).getRoleId());
        assertTrue(userRoleCaptor.getAllValues().get(0).getPrimaryRole());
        verify(userDepartmentMapper).physicalDeleteByUserId(99L);
        verify(userDepartmentMapper).insert(any(UserDepartmentEntity.class));
        verify(operationLogService).record(any(), eq("USER"), eq("UPDATE"), any(), any());
    }

    @Test
    void createUser_whenExtraRoleProvided_shouldReject() {
        UserCreateRequest request = new UserCreateRequest();
        request.setAccount("agent01");
        request.setDisplayName("客服一号");
        request.setEmail("agent01@example.com");
        request.setRoleCode("AGENT");
        request.setRoleCodes(List.of("AGENT", "QUALITY_CHECKER"));
        request.setDepartmentId(10L);
        request.setPassword("agent123");
        request.setEnabled(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.createUser(admin, request));

        assertEquals(40001, ex.getCode());
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

    private DepartmentEntity department(Long id, String name) {
        DepartmentEntity department = new DepartmentEntity();
        department.setId(id);
        department.setDeptName(name);
        department.setDeptPath("/DEFAULT/");
        department.setEnabled(true);
        return department;
    }

    private UserDepartmentEntity userDepartment(Long departmentId) {
        UserDepartmentEntity userDepartment = new UserDepartmentEntity();
        userDepartment.setDepartmentId(departmentId);
        userDepartment.setPrimaryDepartment(true);
        return userDepartment;
    }
}
