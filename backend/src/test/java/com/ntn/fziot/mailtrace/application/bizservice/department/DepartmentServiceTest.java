package com.ntn.fziot.mailtrace.application.bizservice.department;

import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentCreateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.DepartmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.OperationLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserDepartmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.DepartmentMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserDepartmentMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentMapper departmentMapper;
    @Mock
    private UserDepartmentMapper userDepartmentMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private OperationLogMapper operationLogMapper;
    @Mock
    private PermissionService permissionService;

    private DepartmentService departmentService;

    private final CurrentUserPrincipal admin = new CurrentUserPrincipal(
            1L, "admin", "管理员", "admin@example.com", "ADMIN");

    @BeforeEach
    void setUp() {
        departmentService = new DepartmentService(
                departmentMapper,
                userDepartmentMapper,
                userMapper,
                operationLogMapper,
                permissionService
        );
    }

    @Test
    void listTree_shouldBuildDepartmentTreeAndMemberCounts() {
        DepartmentEntity root = department(10L, null, "DEFAULT", "默认部门", "/DEFAULT/", true, 10);
        DepartmentEntity child = department(11L, 10L, "SUPPORT", "客服部", "/DEFAULT/SUPPORT/", true, 20);
        UserDepartmentEntity rootMember = userDepartment(100L, 10L);
        UserDepartmentEntity childMember = userDepartment(101L, 11L);
        UserDepartmentEntity childMember2 = userDepartment(102L, 11L);
        when(departmentMapper.selectList(any())).thenReturn(List.of(root, child));
        when(userDepartmentMapper.selectList(any())).thenReturn(List.of(rootMember, childMember, childMember2));

        List<DepartmentVO> tree = departmentService.listTree(admin, true);

        assertEquals(1, tree.size());
        assertEquals("DEFAULT", tree.get(0).deptCode());
        assertEquals(1L, tree.get(0).memberCount());
        assertEquals(1, tree.get(0).children().size());
        assertEquals("SUPPORT", tree.get(0).children().get(0).deptCode());
        assertEquals(2L, tree.get(0).children().get(0).memberCount());
        verify(permissionService).assertPermission(admin, "department:read", "无权查看组织管理");
    }

    @Test
    void createDepartment_shouldInsertNormalizedCodeAndPath() {
        DepartmentCreateRequest request = new DepartmentCreateRequest();
        request.setParentId(10L);
        request.setDeptCode(" support ");
        request.setDeptName(" 客服部 ");
        request.setDeptDesc("一线客服");
        request.setLeaderUserId(2L);
        request.setEnabled(true);

        DepartmentEntity parent = department(10L, null, "DEFAULT", "默认部门", "/DEFAULT/", true, 10);
        UserEntity leader = new UserEntity();
        leader.setId(2L);
        leader.setDisplayName("主管");
        leader.setEnabled(true);
        AtomicReference<DepartmentEntity> insertedRef = new AtomicReference<>();

        when(departmentMapper.selectById(10L)).thenReturn(parent);
        when(departmentMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectById(2L)).thenReturn(leader);
        when(departmentMapper.selectOne(any())).thenReturn(department(12L, 10L, "SALES", "销售部", "/DEFAULT/SALES/", true, 20));
        doAnswer(invocation -> {
            DepartmentEntity inserted = invocation.getArgument(0);
            inserted.setId(13L);
            insertedRef.set(inserted);
            return 1;
        }).when(departmentMapper).insert(any(DepartmentEntity.class));
        when(departmentMapper.selectById(13L)).thenAnswer(invocation -> insertedRef.get());
        when(userDepartmentMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(leader));

        DepartmentVO created = departmentService.createDepartment(admin, request);

        ArgumentCaptor<DepartmentEntity> captor = ArgumentCaptor.forClass(DepartmentEntity.class);
        verify(departmentMapper).insert(captor.capture());
        DepartmentEntity inserted = captor.getValue();
        assertEquals("SUPPORT", inserted.getDeptCode());
        assertEquals("客服部", inserted.getDeptName());
        assertEquals("/DEFAULT/SUPPORT/", inserted.getDeptPath());
        assertEquals(30, inserted.getSortOrder());
        assertTrue(inserted.getEnabled());
        assertEquals("主管", created.leaderDisplayName());
        verify(operationLogMapper).insert(any(OperationLogEntity.class));
    }

    @Test
    void updateEnabled_whenDefaultDepartmentDisabled_shouldReject() {
        DepartmentEnabledRequest request = new DepartmentEnabledRequest();
        request.setEnabled(false);
        when(departmentMapper.selectById(10L)).thenReturn(department(10L, null, "DEFAULT", "默认部门", "/DEFAULT/", true, 10));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> departmentService.updateEnabled(admin, 10L, request));

        assertEquals(40302, ex.getCode());
    }

    @Test
    void updateEnabled_whenHasEnabledChildren_shouldReject() {
        DepartmentEnabledRequest request = new DepartmentEnabledRequest();
        request.setEnabled(false);
        when(departmentMapper.selectById(11L)).thenReturn(department(11L, null, "SUPPORT", "客服部", "/SUPPORT/", true, 10));
        when(departmentMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> departmentService.updateEnabled(admin, 11L, request));

        assertEquals(40001, ex.getCode());
    }

    private DepartmentEntity department(Long id, Long parentId, String code, String name, String path, Boolean enabled, Integer sortOrder) {
        DepartmentEntity department = new DepartmentEntity();
        department.setId(id);
        department.setParentId(parentId);
        department.setDeptCode(code);
        department.setDeptName(name);
        department.setDeptPath(path);
        department.setEnabled(enabled);
        department.setSortOrder(sortOrder);
        return department;
    }

    private UserDepartmentEntity userDepartment(Long userId, Long departmentId) {
        UserDepartmentEntity userDepartment = new UserDepartmentEntity();
        userDepartment.setUserId(userId);
        userDepartment.setDepartmentId(departmentId);
        userDepartment.setPrimaryDepartment(true);
        return userDepartment;
    }
}
