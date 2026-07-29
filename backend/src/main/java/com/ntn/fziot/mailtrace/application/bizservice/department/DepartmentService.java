package com.ntn.fziot.mailtrace.application.bizservice.department;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentCreateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentUpdateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.DepartmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.OperationLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserDepartmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.DepartmentMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserDepartmentMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_FORBIDDEN = 40302;
    private static final int CODE_NOT_FOUND = 40401;
    private static final int CODE_CONFLICT = 40901;
    private static final String DEFAULT_DEPT_CODE = "DEFAULT";

    private final DepartmentMapper departmentMapper;
    private final UserDepartmentMapper userDepartmentMapper;
    private final UserMapper userMapper;
    private final OperationLogMapper operationLogMapper;
    private final PermissionService permissionService;

    public List<DepartmentVO> listTree(CurrentUserPrincipal principal, Boolean enabled) {
        permissionService.assertPermission(principal, "department:read", "无权查看组织管理");

        LambdaQueryWrapper<DepartmentEntity> wrapper = new LambdaQueryWrapper<>();
        if (enabled != null) {
            wrapper.eq(DepartmentEntity::getEnabled, enabled);
        }
        wrapper.orderByAsc(DepartmentEntity::getSortOrder).orderByAsc(DepartmentEntity::getId);
        List<DepartmentEntity> departments = departmentMapper.selectList(wrapper);

        Map<Long, List<DepartmentEntity>> childrenByParent = departments.stream()
                .filter(department -> department.getParentId() != null)
                .collect(Collectors.groupingBy(DepartmentEntity::getParentId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, Long> memberCounts = memberCounts();
        Map<Long, String> leaderNames = leaderNames(departments);

        return departments.stream()
                .filter(department -> department.getParentId() == null)
                .map(department -> toVO(department, childrenByParent, memberCounts, leaderNames))
                .toList();
    }

    public DepartmentVO getDepartment(CurrentUserPrincipal principal, Long id) {
        permissionService.assertPermission(principal, "department:read", "无权查看组织管理");
        return singleVO(requireDepartment(id));
    }

    @Transactional
    public DepartmentVO createDepartment(CurrentUserPrincipal principal, DepartmentCreateRequest request) {
        permissionService.assertPermission(principal, "department:create", "无权新建部门");

        DepartmentEntity parent = request.getParentId() == null ? null : requireDepartment(request.getParentId());
        String deptCode = normalizeCode(request.getDeptCode());
        String deptName = requireName(request.getDeptName());
        ensureCodeUnique(deptCode);
        validateLeader(request.getLeaderUserId());

        DepartmentEntity department = new DepartmentEntity();
        department.setParentId(parent == null ? null : parent.getId());
        department.setDeptCode(deptCode);
        department.setDeptName(deptName);
        department.setDeptDesc(normalize(request.getDeptDesc()));
        department.setLeaderUserId(request.getLeaderUserId());
        department.setDeptPath(buildPath(parent, deptCode));
        department.setEnabled(request.getEnabled() == null || request.getEnabled());
        department.setSortOrder(request.getSortOrder() == null ? nextSortOrder(parent == null ? null : parent.getId()) : request.getSortOrder());
        department.setCreatedBy(principal.account());
        department.setUpdatedBy(principal.account());
        departmentMapper.insert(department);

        recordLog(principal, "CREATE", department.getId(), "新建部门：" + deptName);
        return singleVO(departmentMapper.selectById(department.getId()));
    }

    @Transactional
    public DepartmentVO updateDepartment(CurrentUserPrincipal principal, Long id, DepartmentUpdateRequest request) {
        permissionService.assertPermission(principal, "department:update", "无权编辑部门");
        DepartmentEntity existing = requireDepartment(id);
        validateLeader(request.getLeaderUserId());

        departmentMapper.update(null, new LambdaUpdateWrapper<DepartmentEntity>()
                .eq(DepartmentEntity::getId, id)
                .set(DepartmentEntity::getDeptName, requireName(request.getDeptName()))
                .set(DepartmentEntity::getDeptDesc, normalize(request.getDeptDesc()))
                .set(DepartmentEntity::getLeaderUserId, request.getLeaderUserId())
                .set(DepartmentEntity::getEnabled, request.getEnabled() == null || request.getEnabled())
                .set(DepartmentEntity::getSortOrder, request.getSortOrder() == null ? existing.getSortOrder() : request.getSortOrder())
                .set(DepartmentEntity::getUpdatedBy, principal.account()));

        recordLog(principal, "UPDATE", id, "编辑部门：" + existing.getDeptName());
        return singleVO(departmentMapper.selectById(id));
    }

    @Transactional
    public DepartmentVO updateEnabled(CurrentUserPrincipal principal, Long id, DepartmentEnabledRequest request) {
        permissionService.assertPermission(principal, "department:enable", "无权启停部门");
        DepartmentEntity existing = requireDepartment(id);
        if (DEFAULT_DEPT_CODE.equals(normalizeCode(existing.getDeptCode())) && !Boolean.TRUE.equals(request.getEnabled())) {
            throw new BusinessException(CODE_FORBIDDEN, "默认部门不可停用");
        }
        if (!Boolean.TRUE.equals(request.getEnabled()) && hasEnabledChildren(id)) {
            throw new BusinessException(CODE_BAD_REQUEST, "请先停用下级部门");
        }

        departmentMapper.update(null, new LambdaUpdateWrapper<DepartmentEntity>()
                .eq(DepartmentEntity::getId, id)
                .set(DepartmentEntity::getEnabled, request.getEnabled())
                .set(DepartmentEntity::getUpdatedBy, principal.account()));

        recordLog(principal, Boolean.TRUE.equals(request.getEnabled()) ? "ENABLE" : "DISABLE", id,
                (Boolean.TRUE.equals(request.getEnabled()) ? "启用部门：" : "停用部门：") + existing.getDeptName());
        return singleVO(departmentMapper.selectById(id));
    }

    private DepartmentVO singleVO(DepartmentEntity department) {
        Map<Long, Long> memberCounts = memberCounts();
        Map<Long, String> leaderNames = leaderNames(List.of(department));
        return toVO(department, Map.of(), memberCounts, leaderNames);
    }

    private DepartmentVO toVO(DepartmentEntity department, Map<Long, List<DepartmentEntity>> childrenByParent,
                              Map<Long, Long> memberCounts, Map<Long, String> leaderNames) {
        List<DepartmentVO> children = childrenByParent.getOrDefault(department.getId(), List.of()).stream()
                .sorted(Comparator.comparing(DepartmentEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DepartmentEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .map(child -> toVO(child, childrenByParent, memberCounts, leaderNames))
                .toList();
        return new DepartmentVO(
                department.getId(),
                department.getParentId(),
                department.getDeptCode(),
                department.getDeptName(),
                department.getDeptDesc(),
                department.getLeaderUserId(),
                department.getLeaderUserId() == null ? null : leaderNames.get(department.getLeaderUserId()),
                department.getDeptPath(),
                department.getEnabled(),
                department.getSortOrder(),
                memberCounts.getOrDefault(department.getId(), 0L),
                department.getCreatedAt(),
                department.getUpdatedAt(),
                children
        );
    }

    private Map<Long, Long> memberCounts() {
        return userDepartmentMapper.selectList(new LambdaQueryWrapper<UserDepartmentEntity>())
                .stream()
                .filter(row -> row.getDepartmentId() != null)
                .collect(Collectors.groupingBy(UserDepartmentEntity::getDepartmentId, LinkedHashMap::new, Collectors.counting()));
    }

    private Map<Long, String> leaderNames(List<DepartmentEntity> departments) {
        Set<Long> leaderIds = departments.stream()
                .map(DepartmentEntity::getLeaderUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (leaderIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(leaderIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getDisplayName, (left, right) -> left, LinkedHashMap::new));
    }

    private DepartmentEntity requireDepartment(Long id) {
        if (id == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "部门ID不能为空");
        }
        DepartmentEntity department = departmentMapper.selectById(id);
        if (department == null) {
            throw new BusinessException(CODE_NOT_FOUND, "部门不存在");
        }
        return department;
    }

    private void validateLeader(Long leaderUserId) {
        if (leaderUserId == null) {
            return;
        }
        UserEntity user = userMapper.selectById(leaderUserId);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(CODE_BAD_REQUEST, "负责人不存在或已停用");
        }
    }

    private void ensureCodeUnique(String deptCode) {
        if (deptCode.isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "请输入部门编码");
        }
        Long count = departmentMapper.selectCount(new LambdaQueryWrapper<DepartmentEntity>().eq(DepartmentEntity::getDeptCode, deptCode));
        if (count != null && count > 0) {
            throw new BusinessException(CODE_CONFLICT, "部门编码已存在");
        }
    }

    private boolean hasEnabledChildren(Long id) {
        Long count = departmentMapper.selectCount(new LambdaQueryWrapper<DepartmentEntity>()
                .eq(DepartmentEntity::getParentId, id)
                .eq(DepartmentEntity::getEnabled, true));
        return count != null && count > 0;
    }

    private Integer nextSortOrder(Long parentId) {
        LambdaQueryWrapper<DepartmentEntity> wrapper = new LambdaQueryWrapper<DepartmentEntity>()
                .orderByDesc(DepartmentEntity::getSortOrder)
                .orderByDesc(DepartmentEntity::getId)
                .last("LIMIT 1");
        if (parentId == null) {
            wrapper.isNull(DepartmentEntity::getParentId);
        } else {
            wrapper.eq(DepartmentEntity::getParentId, parentId);
        }
        DepartmentEntity latest = departmentMapper.selectOne(wrapper);
        return latest == null || latest.getSortOrder() == null ? 100 : latest.getSortOrder() + 10;
    }

    private String buildPath(DepartmentEntity parent, String deptCode) {
        String segment = normalizeCode(deptCode);
        if (parent == null) {
            return "/" + segment + "/";
        }
        String parentPath = normalize(parent.getDeptPath());
        if (parentPath.isEmpty()) {
            parentPath = "/" + normalizeCode(parent.getDeptCode()) + "/";
        }
        return parentPath.endsWith("/") ? parentPath + segment + "/" : parentPath + "/" + segment + "/";
    }

    private String requireName(String value) {
        String name = normalize(value);
        if (name.isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "请输入部门名称");
        }
        return name;
    }

    private String normalizeCode(String value) {
        return normalize(value).toUpperCase();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private void recordLog(CurrentUserPrincipal principal, String actionCode, Long bizId, String content) {
        OperationLogEntity log = new OperationLogEntity();
        log.setOperator(principal.account());
        log.setModuleCode("DEPARTMENT");
        log.setActionCode(actionCode);
        log.setBizId(String.valueOf(bizId));
        log.setContent(content);
        log.setCreatedBy(principal.account());
        log.setUpdatedBy(principal.account());
        operationLogMapper.insert(log);
    }
}
