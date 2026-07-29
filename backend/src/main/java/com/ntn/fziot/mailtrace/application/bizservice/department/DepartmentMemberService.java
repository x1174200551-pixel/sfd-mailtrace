package com.ntn.fziot.mailtrace.application.bizservice.department;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntn.fziot.mailtrace.repox.mysql.entity.DepartmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserDepartmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.DepartmentMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserDepartmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 部门成员解析服务，用于数据权限中的 DEPT / DEPT_AND_CHILDREN 范围。
 */
@Service
@RequiredArgsConstructor
public class DepartmentMemberService {

    private final UserDepartmentMapper userDepartmentMapper;
    private final DepartmentMapper departmentMapper;

    /**
     * 查询用户所属部门及所有下级部门的成员 ID 集合（包含用户本人）。
     * 如果用户未分配部门，返回空集合。
     */
    public Set<Long> resolveDeptAndChildrenMemberIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        Long departmentId = findPrimaryDepartmentId(userId);
        if (departmentId == null) {
            return Collections.emptySet();
        }
        Set<Long> deptIds = new HashSet<>();
        collectDeptAndChildren(departmentId, deptIds);
        return findMemberIdsByDepartments(deptIds);
    }

    /**
     * 查询用户所属部门的成员 ID 集合（仅直属部门，不包含下级）。
     * 如果用户未分配部门，返回空集合。
     */
    public Set<Long> resolveDeptMemberIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        Long departmentId = findPrimaryDepartmentId(userId);
        if (departmentId == null) {
            return Collections.emptySet();
        }
        return findMemberIdsByDepartments(Collections.singleton(departmentId));
    }

    /**
     * 查找用户的主部门 ID。
     */
    private Long findPrimaryDepartmentId(Long userId) {
        List<UserDepartmentEntity> relations = userDepartmentMapper.selectList(
                new LambdaQueryWrapper<UserDepartmentEntity>()
                        .eq(UserDepartmentEntity::getUserId, userId)
                        .orderByDesc(UserDepartmentEntity::getPrimaryDepartment)
                        .orderByAsc(UserDepartmentEntity::getId)
                        .last("LIMIT 1"));
        return relations.isEmpty() ? null : relations.get(0).getDepartmentId();
    }

    /**
     * 递归收集部门 ID 及所有下级部门 ID。
     */
    private void collectDeptAndChildren(Long parentId, Set<Long> result) {
        if (parentId == null || result.contains(parentId)) {
            return;
        }
        result.add(parentId);
        List<DepartmentEntity> children = departmentMapper.selectList(
                new LambdaQueryWrapper<DepartmentEntity>()
                        .eq(DepartmentEntity::getParentId, parentId)
                        .eq(DepartmentEntity::getEnabled, true));
        for (DepartmentEntity child : children) {
            collectDeptAndChildren(child.getId(), result);
        }
    }

    /**
     * 根据部门 ID 集合查询所有成员（用户 ID）。
     */
    private Set<Long> findMemberIdsByDepartments(Set<Long> deptIds) {
        if (deptIds.isEmpty()) {
            return Collections.emptySet();
        }
        return userDepartmentMapper.selectList(
                        new LambdaQueryWrapper<UserDepartmentEntity>()
                                .in(UserDepartmentEntity::getDepartmentId, deptIds))
                .stream()
                .map(UserDepartmentEntity::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
