package com.yjw.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yjw.auth.domain.po.Role;
import com.yjw.auth.mapper.RoleMapper;
import com.yjw.auth.service.IRoleMenuService;
import com.yjw.auth.service.IRolePrivilegeService;
import com.yjw.auth.service.IRoleService;
import com.yjw.auth.util.PrivilegeCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色表 服务实现类
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements IRoleService {

    private final IRoleMenuService roleMenuService;
    private final IRolePrivilegeService rolePrivilegeService;
    private final PrivilegeCache privilegeCache;

    @Override
    public boolean exists(Long roleId) {
        Long count = lambdaQuery().eq(Role::getId, roleId).count();
        return count > 0;
    }

    @Override
    public boolean exists(List<Long> roleIds) {
        int count = lambdaQuery().in(Role::getId, roleIds).count().intValue();
        return count != roleIds.size();
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        // 1.删除角色
        removeById(id);
        // 2.删除角色与权限的关联信息
        roleMenuService.removeByRoleId(id);
        rolePrivilegeService.removeByRoleId(id);
        // 3.清理缓存
        privilegeCache.removeCacheByRoleId(id);
    }
}
