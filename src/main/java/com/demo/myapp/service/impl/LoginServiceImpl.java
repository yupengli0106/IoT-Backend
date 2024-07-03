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
        String username = user.getUsername();
        String password = user.getPassword();
        String email = user.getEmail();
        // Check if the email already exists
        String dbEmail = userMapper.getEmailByEmail(email);

        if (email.equals(dbEmail)) {
            return ResponseEntity.status(409).body(Result.error(409,"Email already exists"));
        }else {
            // encrypt the password and insert the user into the database
            User newUser = new User(username,bCryptPasswordEncoder.encode(password),email);
            userMapper.insertUser(newUser);

            // assign the user with default role(user) and permission(read) after registration
            Long userId = newUser.getId();

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
