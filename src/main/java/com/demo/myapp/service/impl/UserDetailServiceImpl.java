package com.demo.myapp.service.impl;

import com.demo.myapp.mapper.PermissionMapper;
import com.demo.myapp.mapper.RoleMapper;
import com.demo.myapp.mapper.UserMapper;
import com.demo.myapp.pojo.LoginUser;
import com.demo.myapp.pojo.Permission;
import com.demo.myapp.pojo.Role;
import com.demo.myapp.pojo.User;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


/**
 * @Author: Yupeng Li
 * @Date: 1/7/2024 16:21
 * @Description:
 */
@Service
public class UserDetailServiceImpl implements UserDetailsService{
    @Resource
    UserMapper userMapper;
    @Resource
    PermissionMapper permissionMapper;
    @Resource
    RoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User does not exist");
        }else {
            // query user's permissions from database
            List<String> permissions = permissionMapper.findPermissionsByUsername(username);
            // query user's roles from database
            List<String> roles = roleMapper.findRolesByUserId(user.getId());
            return new LoginUser(user, permissions, roles);
        }
    }
}
