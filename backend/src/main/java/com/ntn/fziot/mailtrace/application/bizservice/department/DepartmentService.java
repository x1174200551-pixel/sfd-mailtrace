package com.ntn.fziot.mailtrace.application.bizservice.department;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.application.bizservice.security.OperationLogService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentCreateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentLeaderRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentMemberAddRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentMemberPageResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentMoveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentStatsVO;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentUpdateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentVO;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.DepartmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.RoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserDepartmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.DepartmentMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.RoleMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserRoleMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserDepartmentMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final OperationLogService operationLogService;
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

    public DepartmentStatsVO stats(CurrentUserPrincipal principal) {
        permissionService.assertPermission(principal, "department:read", "无权查看组织管理");
        List<DepartmentEntity> departments = departmentMapper.selectList(new LambdaQueryWrapper<>());
        List<UserEntity> users = userMapper.selectList(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getEnabled, true));
        Set<Long> assignedUserIds = userDepartmentMapper.selectList(new LambdaQueryWrapper<UserDepartmentEntity>())
                .stream()
                .map(UserDepartmentEntity::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        long enabledDepartments = departments.stream().filter(department -> Boolean.TRUE.equals(department.getEnabled())).count();
        long leaderCount = departments.stream()
                .map(DepartmentEntity::getLeaderUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet())
                .size();
        long memberCount = users.stream().filter(user -> assignedUserIds.contains(user.getId())).count();
        long unassignedUserCount = users.stream().filter(user -> !assignedUserIds.contains(user.getId())).count();
        return new DepartmentStatsVO(
                departments.size(),
                enabledDepartments,
                departments.size() - enabledDepartments,
                leaderCount,
                memberCount,
                unassignedUserCount
        );
    }

    @Transactional
    public DepartmentVO createDepartment(CurrentUserPrincipal principal, DepartmentCreateRequest request) {
        permissionService.assertPermission(principal, "department:create", "无权新建部门");

        DepartmentEntity parent = request.getParentId() == null ? null : requireDepartment(request.getParentId());
        if (parent == null && hasRootDepartment()) {
            throw new BusinessException(CODE_BAD_REQUEST, "顶级部门只能有一个，请在现有顶级部门下新建子部门");
        }
        String deptName = requireName(request.getDeptName());
        String deptCode = normalizeCode(request.getDeptCode());
        if (deptCode.isEmpty()) {
            deptCode = generateDepartmentCode();
        }
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

        operationLogService.record(principal, "DEPARTMENT", "CREATE", department.getId(), "新建部门：" + deptName);
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

        operationLogService.record(principal, "DEPARTMENT", "UPDATE", id, "编辑部门：" + existing.getDeptName());
        return singleVO(departmentMapper.selectById(id));
    }

    @Transactional
    public DepartmentVO moveDepartment(CurrentUserPrincipal principal, Long id, DepartmentMoveRequest request) {
        permissionService.assertPermission(principal, "department:update", "无权移动部门");
        DepartmentEntity existing = requireDepartment(id);
        DepartmentEntity parent = request.getParentId() == null ? null : requireDepartment(request.getParentId());
        if (parent == null && existing.getParentId() != null && hasRootDepartment()) {
            throw new BusinessException(CODE_BAD_REQUEST, "顶级部门只能有一个，不能将该部门移动为顶级部门");
        }
        if (parent != null && parent.getId().equals(id)) {
            throw new BusinessException(CODE_BAD_REQUEST, "不能移动到当前部门下");
        }
        if (parent != null && isDescendant(parent.getId(), id)) {
            throw new BusinessException(CODE_BAD_REQUEST, "不能移动到下级部门下");
        }

        departmentMapper.update(null, new LambdaUpdateWrapper<DepartmentEntity>()
                .eq(DepartmentEntity::getId, id)
                .set(DepartmentEntity::getParentId, parent == null ? null : parent.getId())
                .set(DepartmentEntity::getDeptPath, buildPath(parent, existing.getDeptCode()))
                .set(DepartmentEntity::getSortOrder, nextSortOrder(parent == null ? null : parent.getId()))
                .set(DepartmentEntity::getUpdatedBy, principal.account()));
        refreshChildPaths(departmentMapper.selectById(id), principal.account());

        operationLogService.record(principal, "DEPARTMENT", "MOVE", id, "移动部门：" + existing.getDeptName());
        return singleVO(departmentMapper.selectById(id));
    }

    @Transactional
    public DepartmentVO updateLeader(CurrentUserPrincipal principal, Long id, DepartmentLeaderRequest request) {
        permissionService.assertPermission(principal, "department:update", "无权设置部门负责人");
        DepartmentEntity existing = requireDepartment(id);
        validateLeader(request.getLeaderUserId());
        if (!isMemberOfDepartment(request.getLeaderUserId(), id)) {
            throw new BusinessException(CODE_BAD_REQUEST, "负责人需先加入当前部门");
        }

        departmentMapper.update(null, new LambdaUpdateWrapper<DepartmentEntity>()
                .eq(DepartmentEntity::getId, id)
                .set(DepartmentEntity::getLeaderUserId, request.getLeaderUserId())
                .set(DepartmentEntity::getUpdatedBy, principal.account()));

        operationLogService.record(principal, "DEPARTMENT", "SET_LEADER", id, "设置部门负责人：" + existing.getDeptName());
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

        operationLogService.record(principal, "DEPARTMENT", Boolean.TRUE.equals(request.getEnabled()) ? "ENABLE" : "DISABLE", id,
                (Boolean.TRUE.equals(request.getEnabled()) ? "启用部门：" : "停用部门：") + existing.getDeptName());
        return singleVO(departmentMapper.selectById(id));
    }

    public DepartmentMemberPageResponse pageMembers(CurrentUserPrincipal principal, Long departmentId, String keyword,
                                                    String roleCode, Integer page, Integer size) {
        permissionService.assertPermission(principal, "department:read", "无权查看组织管理");
        requireDepartment(departmentId);
        Set<Long> userIds = userDepartmentMapper.selectList(new LambdaQueryWrapper<UserDepartmentEntity>()
                        .eq(UserDepartmentEntity::getDepartmentId, departmentId))
                .stream()
                .map(UserDepartmentEntity::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return pageUserIds(userIds, keyword, roleCode, page, size);
    }

    public DepartmentMemberPageResponse pageMemberCandidates(CurrentUserPrincipal principal, Long departmentId, String keyword,
                                                             Integer page, Integer size) {
        permissionService.assertPermission(principal, "department:read", "无权查看组织管理");
        requireDepartment(departmentId);
        Set<Long> existingUserIds = userDepartmentMapper.selectList(new LambdaQueryWrapper<UserDepartmentEntity>()
                        .eq(UserDepartmentEntity::getDepartmentId, departmentId))
                .stream()
                .map(UserDepartmentEntity::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> candidateIds = userMapper.selectList(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getEnabled, true))
                .stream()
                .map(UserEntity::getId)
                .filter(id -> id != null && !existingUserIds.contains(id))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return pageUserIds(candidateIds, keyword, null, page, size);
    }

    @Transactional
    public List<UserVO> addMembers(CurrentUserPrincipal principal, Long departmentId, DepartmentMemberAddRequest request) {
        permissionService.assertPermission(principal, "department:update", "无权添加部门成员");
        DepartmentEntity department = requireDepartment(departmentId);
        List<Long> userIds = request.getUserIds().stream().filter(id -> id != null).distinct().toList();
        if (userIds.isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "请选择要添加的成员");
        }
        for (Long userId : userIds) {
            UserEntity user = requireEnabledUser(userId);
            userDepartmentMapper.physicalDeleteByUserId(user.getId());
            UserDepartmentEntity relation = new UserDepartmentEntity();
            relation.setUserId(user.getId());
            relation.setDepartmentId(departmentId);
            relation.setPrimaryDepartment(true);
            relation.setCreatedBy(principal.account());
            relation.setUpdatedBy(principal.account());
            userDepartmentMapper.insert(relation);
        }

        operationLogService.record(principal, "DEPARTMENT", "ADD_MEMBER", departmentId,
                "添加部门成员：" + department.getDeptName() + "，共 " + userIds.size() + " 人");
        return userMapper.selectBatchIds(userIds).stream().map(this::toUserVO).toList();
    }

    @Transactional
    public void removeMember(CurrentUserPrincipal principal, Long departmentId, Long userId) {
        permissionService.assertPermission(principal, "department:update", "无权移出部门成员");
        DepartmentEntity department = requireDepartment(departmentId);
        requireEnabledUser(userId);
        if (!isMemberOfDepartment(userId, departmentId)) {
            throw new BusinessException(CODE_BAD_REQUEST, "成员不在当前部门");
        }
        if (department.getLeaderUserId() != null && department.getLeaderUserId().equals(userId)) {
            departmentMapper.update(null, new LambdaUpdateWrapper<DepartmentEntity>()
                    .eq(DepartmentEntity::getId, departmentId)
                    .set(DepartmentEntity::getLeaderUserId, null)
                    .set(DepartmentEntity::getUpdatedBy, principal.account()));
        }
        userDepartmentMapper.delete(new LambdaQueryWrapper<UserDepartmentEntity>()
                .eq(UserDepartmentEntity::getDepartmentId, departmentId)
                .eq(UserDepartmentEntity::getUserId, userId));

        operationLogService.record(principal, "DEPARTMENT", "REMOVE_MEMBER", departmentId,
                "移出部门成员：" + department.getDeptName());
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

    private boolean hasRootDepartment() {
        Long count = departmentMapper.selectCount(new LambdaQueryWrapper<DepartmentEntity>()
                .isNull(DepartmentEntity::getParentId));
        return count != null && count > 0;
    }

    private boolean isDescendant(Long targetId, Long ancestorId) {
        Set<Long> visited = new HashSet<>();
        Long currentId = targetId;
        while (currentId != null && visited.add(currentId)) {
            DepartmentEntity current = departmentMapper.selectById(currentId);
            if (current == null) {
                return false;
            }
            if (ancestorId.equals(current.getParentId())) {
                return true;
            }
            currentId = current.getParentId();
        }
        return false;
    }

    private void refreshChildPaths(DepartmentEntity parent, String operator) {
        if (parent == null) {
            return;
        }
        List<DepartmentEntity> children = departmentMapper.selectList(new LambdaQueryWrapper<DepartmentEntity>()
                .eq(DepartmentEntity::getParentId, parent.getId()));
        for (DepartmentEntity child : children) {
            departmentMapper.update(null, new LambdaUpdateWrapper<DepartmentEntity>()
                    .eq(DepartmentEntity::getId, child.getId())
                    .set(DepartmentEntity::getDeptPath, buildPath(parent, child.getDeptCode()))
                    .set(DepartmentEntity::getUpdatedBy, operator));
            DepartmentEntity refreshed = departmentMapper.selectById(child.getId());
            refreshChildPaths(refreshed == null ? child : refreshed, operator);
        }
    }

    private boolean isMemberOfDepartment(Long userId, Long departmentId) {
        Long count = userDepartmentMapper.selectCount(new LambdaQueryWrapper<UserDepartmentEntity>()
                .eq(UserDepartmentEntity::getUserId, userId)
                .eq(UserDepartmentEntity::getDepartmentId, departmentId));
        return count != null && count > 0;
    }

    private UserEntity requireEnabledUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "用户ID不能为空");
        }
        UserEntity user = userMapper.selectById(userId);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(CODE_BAD_REQUEST, "用户不存在或已停用");
        }
        return user;
    }

    private DepartmentMemberPageResponse pageUserIds(Set<Long> userIds, String keyword, String roleCode, Integer page, Integer size) {
        long currentPage = normalizePage(page);
        long pageSize = normalizeSize(size);
        String normalizedKeyword = normalize(keyword);
        String normalizedRole = normalizeCode(roleCode);
        List<UserVO> all = userIds.isEmpty() ? List.of() : userMapper.selectBatchIds(userIds).stream()
                .filter(user -> normalizedRole.isEmpty() || normalizedRole.equals(normalizeCode(user.getRoleCode())))
                .filter(user -> normalizedKeyword.isEmpty()
                        || normalize(user.getAccount()).contains(normalizedKeyword)
                        || normalize(user.getDisplayName()).contains(normalizedKeyword)
                        || normalize(user.getEmail()).contains(normalizedKeyword))
                .sorted(Comparator.comparing(UserEntity::getDisplayName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(UserEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::toUserVO)
                .toList();
        int from = (int) Math.min((currentPage - 1) * pageSize, all.size());
        int to = (int) Math.min(from + pageSize, all.size());
        long pages = all.isEmpty() ? 0 : (all.size() + pageSize - 1) / pageSize;
        return new DepartmentMemberPageResponse(all.subList(from, to), all.size(), currentPage, pageSize, pages);
    }

    private UserVO toUserVO(UserEntity user) {
        DepartmentEntity department = primaryDepartment(user.getId());
        return new UserVO(
                user.getId(),
                user.getAccount(),
                user.getDisplayName(),
                user.getEmail(),
                user.getRoleCode(),
                roleCodes(user.getId(), user.getRoleCode()),
                department == null ? null : department.getId(),
                department == null ? null : department.getDeptName(),
                department == null ? null : department.getDeptPath(),
                user.getEnabled(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private DepartmentEntity primaryDepartment(Long userId) {
        UserDepartmentEntity relation = userDepartmentMapper.selectList(new LambdaQueryWrapper<UserDepartmentEntity>()
                        .eq(UserDepartmentEntity::getUserId, userId)
                        .orderByDesc(UserDepartmentEntity::getPrimaryDepartment)
                        .orderByAsc(UserDepartmentEntity::getId))
                .stream()
                .findFirst()
                .orElse(null);
        if (relation == null || relation.getDepartmentId() == null) {
            return null;
        }
        return departmentMapper.selectById(relation.getDepartmentId());
    }

    private List<String> roleCodes(Long userId, String fallbackRoleCode) {
        Long roleId = userRoleMapper.selectList(new LambdaQueryWrapper<com.ntn.fziot.mailtrace.repox.mysql.entity.UserRoleEntity>()
                        .eq(com.ntn.fziot.mailtrace.repox.mysql.entity.UserRoleEntity::getUserId, userId)
                        .orderByDesc(com.ntn.fziot.mailtrace.repox.mysql.entity.UserRoleEntity::getPrimaryRole)
                        .orderByAsc(com.ntn.fziot.mailtrace.repox.mysql.entity.UserRoleEntity::getId))
                .stream()
                .map(com.ntn.fziot.mailtrace.repox.mysql.entity.UserRoleEntity::getRoleId)
                .filter(id -> id != null)
                .findFirst()
                .orElse(null);
        if (roleId == null) {
            String normalizedFallback = normalizeCode(fallbackRoleCode);
            return normalizedFallback.isEmpty() ? List.of() : List.of(normalizedFallback);
        }
        RoleEntity role = roleMapper.selectById(roleId);
        String roleCode = role == null ? null : normalizeCode(role.getRoleCode());
        return roleCode == null || roleCode.isEmpty() ? List.of() : List.of(roleCode);
    }

    private long normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private long normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 100);
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

    private String generateDepartmentCode() {
        String base = "DEPT_" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        String code = base;
        int suffix = 1;
        while (departmentMapper.selectCount(new LambdaQueryWrapper<DepartmentEntity>().eq(DepartmentEntity::getDeptCode, code)) > 0) {
            suffix++;
            code = base + "_" + suffix;
        }
        return code;
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

}
