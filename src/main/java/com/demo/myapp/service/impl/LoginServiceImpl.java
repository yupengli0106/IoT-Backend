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
import org.springframework.transaction.annotation.Transactional;

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
    @Resource
    EmailService emailService;

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

            // 创建包含token和roles的响应
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("roles", loginUser.getRoles());

            return ResponseEntity.ok(Result.success(response));
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
            // verify the email by sending a verification code
            String code = null;
            try {
                code = emailService.sendVerificationCode(email);
            } catch (Exception e) {
                // TODO: log the exception
                return ResponseEntity.status(500).body(Result.error(500, "Failed to send verification code"));
            }
            // 临时存储验证码
            redisTemplate.opsForValue().set(email, code, 3, TimeUnit.MINUTES);
            // 临时存储用户信息
            redisTemplate.opsForValue().set("temp_user:" + email, user, 4, TimeUnit.MINUTES);

            return ResponseEntity.ok(Result.success("Verification code sent to your email"));
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

    @Override
    @Transactional
    public ResponseEntity<Result> verifyCode(String email, String code) {
        // 去除前后空格
        email = email.trim();
        code = code.trim();
        String storedCode = (String) redisTemplate.opsForValue().get(email);
        if (storedCode != null && storedCode.equals(code)) {
            // 验证成功，完成注册流程
            User tempUser = (User) redisTemplate.opsForValue().get("temp_user:" + email);
            if (tempUser != null) {
                tempUser.setPassword(bCryptPasswordEncoder.encode(tempUser.getPassword()));
                userMapper.insertUser(tempUser);

                // assign the user with default role(‘ROLE_USER’) and permission(‘READ_PRIVILEGE’) after registration
                Long userId = tempUser.getId();
                /*
                * 这里设置了默认的角色为‘ROLE_USER’，同时会根据‘ROLE_USER’这个角色给用户分配一个默认的权限，这个权限是在数据库中的，不是在代码中写死的
                */
                Long roleId = roleMapper.getRoleIdByRoleName("ROLE_USER");
                roleMapper.insertUserRole(userId, roleId);

                // 清除临时数据
                redisTemplate.delete(email);
                redisTemplate.delete("temp_user:" + email);

                return ResponseEntity.ok(Result.success("Registration successful"));
            } else {
                return ResponseEntity.status(400).body(Result.error(400, "Temporary user data not found"));
            }
        } else {
            return ResponseEntity.status(400).body(Result.error(400, "Invalid verification code"));
        }
    }


}
