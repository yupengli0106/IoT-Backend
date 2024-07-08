package com.demo.myapp.service.impl;

import com.demo.myapp.controller.response.Result;
import com.demo.myapp.mapper.RoleMapper;
import com.demo.myapp.mapper.UserMapper;
import com.demo.myapp.pojo.LoginUser;
import com.demo.myapp.pojo.User;
import com.demo.myapp.service.LoginService;
import com.demo.myapp.utils.JwtUtil;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    RoleMapper roleMapper;
    @Resource
    BCryptPasswordEncoder bCryptPasswordEncoder;
    @Resource
    AuthenticationManager authenticationManager;
    @Resource
    RedisTemplate<String, Object> redisTemplate;

    @Override
    public ResponseEntity<Result> login(User user) {
        // use SpringSecurity's AuthenticationManager to authenticate the user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));

        //pass the authentication
        if (authentication.isAuthenticated()){
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            // Generate a token
            Map<String, Object> claims = new HashMap<>();
            claims.put("username", loginUser.getUsername());
            claims.put("password", loginUser.getPassword());
            String token = JwtUtil.generateToken(claims);
            // Store the token in the redis for 1 hour
            redisTemplate.opsForValue().set(token,loginUser, 1, TimeUnit.HOURS);
            return ResponseEntity.ok(Result.success(token));
        }else {
            // If the authentication fails, return an error message
            return ResponseEntity.status(403).body(Result.error(403,"Password or Username is incorrect"));
        }
    }

    @Override
    public ResponseEntity<Result> register(User user) {
        String username = user.getUsername().trim();
        // TODO: 后续可以改进，比如密码长度限制
        String password = user.getPassword().trim();
        String email = user.getEmail().trim();
        // Check if the username already exists
        String dbUsername = userMapper.getUsernameByUsername(username);
        // Check if the email already exists
        String dbEmail = userMapper.getEmailByEmail(email);


        if (dbUsername != null && dbUsername.equals(username)) {
            return ResponseEntity.status(409).body(Result.error(409,"Username already exists"));
        } else if (dbEmail != null && email.equals(dbEmail)) {
            return ResponseEntity.status(409).body(Result.error(409,"Email already exists"));
        } else {
            // encrypt the password and insert the user into the database
            User newUser = new User(username,bCryptPasswordEncoder.encode(password),email);
            userMapper.insertUser(newUser);

            // assign the user with default role(‘ROLE_USER’) and permission(‘READ_PRIVILEGE’) after registration
            Long userId = newUser.getId();
            /*
            * 这里设置了默认的角色为‘ROLE_USER’，同时会根据‘ROLE_USER’这个角色给用户分配一个默认的权限，这个权限是在数据库中的，不是在代码中写死的
             */
            Long roleId = roleMapper.getRoleIdByRoleName("ROLE_USER");
            roleMapper.insertUserRole(userId, roleId);

            return ResponseEntity.ok(Result.success("Register successfully"));
        }
    }

    @Override
    public ResponseEntity<Result> logout(String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(403).body(Result.error(403,"Token is empty"));
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        // Delete the token from Redis
        Boolean wasDeleted = redisTemplate.delete(token);
        // Check if the token was successfully deleted
        if (wasDeleted == null || !wasDeleted) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Result.error(500, "Failed to delete token"));
        }

        // Clear the SecurityContextHolder
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(Result.success("Logout successfully"));
    }



}
