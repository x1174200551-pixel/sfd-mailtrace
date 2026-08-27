package com.ntn.fziot.mailtrace.application.bizservice.user;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ntn.fziot.mailtrace.application.bizservice.security.OperationLogService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.application.bizservice.security.EnterpriseMailboxAccessService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserCreateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserUpdateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserDataGrantSaveRequest;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.repox.mysql.entity.DepartmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.RoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserDepartmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserRoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserDataGrantEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.EnterpriseEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.DepartmentMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.RoleMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserDepartmentMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserRoleMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserDataGrantMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.EnterpriseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.AssignmentRuleGroupMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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
    @Mock
    private EnterpriseMailboxAccessService enterpriseMailboxAccessService;
    @Mock
    private UserDataGrantMapper userDataGrantMapper;
    @Mock
    private EnterpriseMapper enterpriseMapper;
    @Mock
    private MailboxMapper mailboxMapper;
    @Mock
    private AssignmentRuleGroupMapper assignmentRuleGroupMapper;

    private UserService userService;

    private final CurrentUserPrincipal admin = new CurrentUserPrincipal(
            1L, "admin", "管理员", "admin@example.com", "ADMIN");

    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "UserServiceTest.UserEntity");
        TableInfoHelper.initTableInfo(assistant, UserEntity.class);
        TableInfoHelper.initTableInfo(assistant, UserDataGrantEntity.class);
        lenient().when(enterpriseMailboxAccessService.isAdmin(admin)).thenReturn(true);
        userService = new UserService(
                userMapper,
                operationLogService,
                roleMapper,
                userRoleMapper,
                departmentMapper,
                userDepartmentMapper,
                passwordEncoder,
                permissionService,
                enterpriseMailboxAccessService,
                userDataGrantMapper,
                enterpriseMapper,
                mailboxMapper,
                assignmentRuleGroupMapper
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
        request.setEnterpriseIds(Set.of(10L));

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("agent123")).thenReturn("encoded");
        when(roleMapper.selectOne(any())).thenReturn(role(20L, "AGENT"));
        when(departmentMapper.selectById(10L)).thenReturn(department(10L, "客服部"));
        when(enterpriseMapper.selectBatchIds(any())).thenReturn(List.of(enterprise(10L)));
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
        verify(userDataGrantMapper).insert(any(UserDataGrantEntity.class));
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
        request.setEnterpriseIds(Set.of(10L));

        when(userMapper.selectById(99L))
                .thenReturn(user(99L, "agent01", "ADMIN"))
                .thenReturn(user(99L, "agent01", "AGENT"));
        when(roleMapper.selectOne(any())).thenReturn(role(20L, "AGENT"));
        when(departmentMapper.selectById(10L)).thenReturn(department(10L, "客服部"));
        when(enterpriseMapper.selectBatchIds(any())).thenReturn(List.of(enterprise(10L)));
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

    @Test
    void saveDataGrants_whenOrdinaryUserHasNoGrant_shouldReject() {
        when(userMapper.selectById(99L)).thenReturn(user(99L, "agent01", "AGENT"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.saveDataGrants(admin, 99L, new UserDataGrantSaveRequest()));

        assertEquals(40001, ex.getCode());
        assertTrue(ex.getMessage().contains("至少需要一条"));
        verify(userDataGrantMapper, never()).physicalDeleteByUserId(any());
    }

    @Test
    void saveDataGrants_shouldRemoveMailboxCoveredByEnterpriseGrant() {
        when(userMapper.selectById(99L)).thenReturn(user(99L, "agent01", "AGENT"));
        EnterpriseEntity enterprise = new EnterpriseEntity();
        enterprise.setId(10L);
        when(enterpriseMapper.selectBatchIds(any())).thenReturn(List.of(enterprise));
        MailboxEntity coveredMailbox = mailbox(11L, 10L);
        MailboxEntity standaloneMailbox = mailbox(12L, 20L);
        when(mailboxMapper.selectBatchIds(any())).thenReturn(List.of(coveredMailbox, standaloneMailbox));
        when(userDataGrantMapper.selectList(any())).thenReturn(List.of());
        UserDataGrantSaveRequest request = new UserDataGrantSaveRequest();
        request.setEnterpriseIds(Set.of(10L));
        request.setMailboxIds(Set.of(11L, 12L));

        userService.saveDataGrants(admin, 99L, request);

        ArgumentCaptor<UserDataGrantEntity> captor = ArgumentCaptor.forClass(UserDataGrantEntity.class);
        verify(userDataGrantMapper).physicalDeleteByUserId(99L);
        verify(userDataGrantMapper, times(2)).insert(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(grant -> "ENTERPRISE".equals(grant.getGrantType()) && Long.valueOf(10L).equals(grant.getEnterpriseId())));
        assertTrue(captor.getAllValues().stream().anyMatch(grant -> "MAILBOX".equals(grant.getGrantType()) && Long.valueOf(12L).equals(grant.getMailboxId())));
        assertTrue(captor.getAllValues().stream().noneMatch(grant -> Long.valueOf(11L).equals(grant.getMailboxId())));
    }

    @Test
    void saveDataGrants_whenAdmin_shouldClearRowsAndKeepAllDataVisible() {
        when(userMapper.selectById(1L)).thenReturn(user(1L, "admin", "ADMIN"));

        var detail = userService.saveDataGrants(admin, 1L, new UserDataGrantSaveRequest());

        assertTrue(detail.allDataVisible());
        assertTrue(detail.grants().isEmpty());
        verify(userDataGrantMapper).physicalDeleteByUserId(1L);
        verify(userDataGrantMapper, never()).insert(any());
    }

    @Test
    void updateUser_whenNonAdminPromotesSelfToAdmin_shouldReject() {
        CurrentUserPrincipal operator = new CurrentUserPrincipal(
                2L, "operator", "配置人员", "operator@example.com", "CUSTOM_MANAGER");
        when(userMapper.selectById(2L)).thenReturn(user(2L, "operator", "CUSTOM_MANAGER"));
        when(enterpriseMailboxAccessService.isAdmin(operator)).thenReturn(false);
        UserUpdateRequest request = new UserUpdateRequest();
        request.setDisplayName("配置人员");
        request.setEmail("operator@example.com");
        request.setRoleCode("ADMIN");
        request.setEnabled(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.updateUser(operator, 2L, request));

        assertEquals(40302, exception.getCode());
        verify(userMapper, never()).update(any(), any());
    }

    @Test
    void saveDataGrants_whenNonAdminGrantsSelf_shouldReject() {
        CurrentUserPrincipal operator = new CurrentUserPrincipal(
                2L, "operator", "配置人员", "operator@example.com", "CUSTOM_MANAGER");
        when(userMapper.selectById(2L)).thenReturn(user(2L, "operator", "CUSTOM_MANAGER"));
        when(enterpriseMailboxAccessService.isAdmin(operator)).thenReturn(false);
        UserDataGrantSaveRequest request = new UserDataGrantSaveRequest();
        request.setEnterpriseIds(Set.of(10L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.saveDataGrants(operator, 2L, request));

        assertEquals(40302, exception.getCode());
        verify(userDataGrantMapper, never()).physicalDeleteByUserId(any());
    }

    @Test
    void saveDataGrants_whenNonAdminExceedsOwnScope_shouldReject() {
        CurrentUserPrincipal operator = new CurrentUserPrincipal(
                2L, "operator", "配置人员", "operator@example.com", "CUSTOM_MANAGER");
        when(userMapper.selectById(3L)).thenReturn(user(3L, "target", "AGENT"));
        when(enterpriseMailboxAccessService.isAdmin(operator)).thenReturn(false);
        when(enterpriseMailboxAccessService.resolveVisibleEnterpriseIds(operator)).thenReturn(Set.of(10L));
        UserDataGrantSaveRequest request = new UserDataGrantSaveRequest();
        request.setEnterpriseIds(Set.of(20L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.saveDataGrants(operator, 3L, request));

        assertEquals(40302, exception.getCode());
        verify(userDataGrantMapper, never()).physicalDeleteByUserId(any());
    }

    @Test
    void listAssignableUsers_shouldKeepOnlyUsersAllowedForTargetEnterprise() {
        UserEntity eligible = user(2L, "eligible", "AGENT");
        UserEntity denied = user(3L, "denied", "AGENT");
        when(userMapper.selectList(any())).thenReturn(List.of(eligible, denied));
        doAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            if (Long.valueOf(3L).equals(userId)) {
                throw new BusinessException(40302, "无企业授权");
            }
            return null;
        }).when(enterpriseMailboxAccessService).assertAssigneeCanAccessEnterprise(any(), eq(10L));
        when(userDepartmentMapper.selectList(any())).thenReturn(List.of());
        when(userRoleMapper.selectList(any())).thenReturn(List.of());

        var result = userService.listAssignableUsers(admin, 10L, null, null);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).id());
    }

    private RoleEntity role(Long id, String roleCode) {
        RoleEntity role = new RoleEntity();
        role.setId(id);
        role.setRoleCode(roleCode);
        role.setEnabled(true);
        return role;
    }

    private EnterpriseEntity enterprise(Long id) {
        EnterpriseEntity enterprise = new EnterpriseEntity();
        enterprise.setId(id);
        enterprise.setEnabled(true);
        return enterprise;
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

    private MailboxEntity mailbox(Long id, Long enterpriseId) {
        MailboxEntity mailbox = new MailboxEntity();
        mailbox.setId(id);
        mailbox.setEnterpriseId(enterpriseId);
        mailbox.setEnabled(true);
        return mailbox;
    }
}
