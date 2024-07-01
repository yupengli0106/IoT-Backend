package com.demo.myapp.service.impl;

import com.demo.myapp.controller.response.Result;
import com.demo.myapp.mapper.UserMapper;
import com.demo.myapp.pojo.User;
import com.demo.myapp.service.LoginService;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @Author: Yupeng Li
 * @Date: 1/7/2024 15:34
 * @Description:
 */
@Service
public class LoginServiceImpl implements LoginService {
    @Resource
    UserMapper userMapper;
    @Resource
    BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    public Result login(User user) {
        String username = user.getUsername();
        String password = user.getPassword();
        String dbPassword = userMapper.getPasswordByUsername(username);

        if (bCryptPasswordEncoder.matches(password,dbPassword)){
            return Result.success("Login successfully");
        }else {
            return Result.error(401,"Password or Username is incorrect");
        }
    }

    @Override
    public Result register(User user) {
        String username = user.getUsername();
        String password = user.getPassword();
        String email = user.getEmail();
        // Check if the email already exists
        String dbEmail = userMapper.getEmailByEmail(email);

        if (email.equals(dbEmail)) {
            return Result.error(401,"This email already exists");
        }else {
            // encrypt the password and insert the user into the database
            User newUser = new User(username,bCryptPasswordEncoder.encode(password),email);
            userMapper.insertUser(newUser);
            return Result.success("Register successfully");
        }
    }
}
