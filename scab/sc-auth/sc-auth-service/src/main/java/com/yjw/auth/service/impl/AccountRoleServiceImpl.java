package com.yjw.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yjw.auth.domain.po.AccountRole;
import com.yjw.auth.mapper.AccountRoleMapper;
import com.yjw.auth.service.IAccountRoleService;
import org.springframework.stereotype.Service;

/**
 * 账户、角色关联表 服务实现类
 */
@Service
public class AccountRoleServiceImpl extends ServiceImpl<AccountRoleMapper, AccountRole> implements IAccountRoleService {

}
