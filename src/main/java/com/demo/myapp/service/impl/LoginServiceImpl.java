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
 * @Description: LoginService implementation including login, register, logout, verifyCode, updateProfile, resetPassword, changePassword
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
    @Resource
    UserService userService;

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
            response.put("email", loginUser.getUser().getEmail());

            return ResponseEntity.ok(Result.success(response));
        }else {
            // If the authentication fails, return an error message
            return ResponseEntity.status(403).body(Result.error(403,"Password or Username is incorrect"));
        }
    }

    @Override
    public ResponseEntity<Result> register(User user) {
        String username = removeSpacesByRegex(user.getUsername());
        String email = removeSpacesByRegex(user.getEmail());
        // Check if the username already exists
        String dbUsername = userMapper.getUsernameByUsername(username);
        // Check if the email already exists
        String dbEmail = userMapper.getEmailByEmail(email);

        if (dbUsername != null && dbUsername.equals(username)) {
            return ResponseEntity.status(409).body(Result.error(409,"Username already exists"));
        } else if (dbEmail != null && email.equals(dbEmail)) {
            return ResponseEntity.status(409).body(Result.error(409,"Email already exists"));
        } else {
            return storeCodeInRedis(user, "register");
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

    /**
     * public method to remove all spaces from a string
     * @param string the string to remove spaces from
     * @return the string without spaces
     */
    private String removeSpacesByRegex(String string) {
        return string.replaceAll("\\s", "");
    }

    /**
     * public method to store the code in Redis
     * @param user the temp user to store in Redis
     * @param action the action to store in Redis
     * @return the response entity
     */
    private ResponseEntity<Result> storeCodeInRedis(User user, String action) {
        // verify the email by sending a verification code
        String email = removeSpacesByRegex(user.getEmail());
        String code;
        try {
            code = removeSpacesByRegex(emailService.sendVerificationCode(email));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Result.error(500, "Failed to send verification code"));
        }
        // 临时存储验证码
        redisTemplate.opsForValue().set(email, code, 3, TimeUnit.MINUTES);
        // 临时存储用户信息
        redisTemplate.opsForValue().set("temp_user:" + email + ":" + action, user, 4, TimeUnit.MINUTES);

        return ResponseEntity.ok(Result.success("Verification code sent to your email"));
    }

    @Override
    @Transactional
    public ResponseEntity<Result> verifyCode(String email, String code, String action) {
        // 去除所有空格
        email = removeSpacesByRegex(email);
        code = removeSpacesByRegex(code);
        String storedCode = (String) redisTemplate.opsForValue().get(email);
        if (storedCode != null && storedCode.equals(code)) {
            // 验证成功，根据不同的action执行不同的操作
            User tempUser = (User) redisTemplate.opsForValue().get("temp_user:" + email + ":" + action);
            if (tempUser != null) {
                return switch (action) {
                    case "register" -> completeRegistration(tempUser);
                    case "updateProfile" -> completeProfileUpdate(tempUser);
                    //告诉前端邮箱验证成功，重定向到修改密码的页面
                    case "resetPassword" -> ResponseEntity.status(202).body(Result.success("Verification successful"));
                    default -> ResponseEntity.status(400).body(Result.error(400, "Invalid action"));
                };
            } else {
                return ResponseEntity.status(400).body(Result.error(400, "Temporary user data not found"));
            }
        } else {
            return ResponseEntity.status(400).body(Result.error(400, "Invalid verification code"));
        }
    }

    protected ResponseEntity<Result> completeProfileUpdate(User user) {
        userMapper.updateUser(user);
        redisTemplate.delete(user.getEmail());
        redisTemplate.delete("temp_user:" + user.getEmail() + ":updateProfile");

        return ResponseEntity.ok(Result.success("Profile updated successfully"));
    }

    protected ResponseEntity<Result> completeRegistration(User user) {
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        userMapper.insertUser(user);
        Long userId = user.getId();
        //这里设置了默认的角色为‘ROLE_USER’，同时会根据‘ROLE_USER’这个角色给用户分配一个默认的权限，这个权限是在数据库中的，不是在代码中写死的
        Long roleId = roleMapper.getRoleIdByRoleName("ROLE_USER");
        roleMapper.insertUserRole(userId, roleId);

        redisTemplate.delete(user.getEmail());
        redisTemplate.delete("temp_user:" + user.getEmail() + ":register");

        return ResponseEntity.ok(Result.success("Registration successful"));
    }

    @Override
    public ResponseEntity<Result> updateProfile(User user) {
        // TODO: 如果修改了密码，需要删除redis中的token。仅仅修改了其他信息，不需要删除token，不需要重新登录！
        if (user == null || user.getEmail() == null) {
            return ResponseEntity.status(400).body(Result.error(400, "Email or other fields are empty"));
        }
        //!这里需要先获取当前登录用户的ID，然后再进行下一步更新用户信息。
        //!如果到下一步 completeProfileUpdate 再去获取当前登录用户的ID，可能会出现用户未认证的情况
        Long userId = userService.getCurrentUserId();
        user.setId(userId);

        //如果修改了邮箱则需要验证邮箱验证码
        if (!userService.getCurrentUserEmail().equals(user.getEmail())) {
            //检查新邮箱是否已存在
            if (userMapper.getEmailByEmail(removeSpacesByRegex(user.getEmail())) != null) {
                return ResponseEntity.status(400).body(Result.error(400, "Email already exists"));
            }
            //验证邮箱
            return storeCodeInRedis(user, "updateProfile");
        } else {//如果没有修改邮箱，则直接更新用户信息
            return completeProfileUpdate(user);
        }

    }

    @Override
    public ResponseEntity<Result> resetPassword(User user) {
        if (user == null || user.getEmail() == null) {
            return ResponseEntity.status(400).body(Result.error(400, "Email or other fields are empty"));
        }else if (userMapper.getEmailByEmail(removeSpacesByRegex(user.getEmail())) == null) {
            return ResponseEntity.status(400).body(Result.error(400, "Email does not exist"));
        }
        return storeCodeInRedis(user, "resetPassword");
    }

    @Override
    @Transactional
    public ResponseEntity<Result> changePassword(User user) {
        if (user == null || user.getEmail() == null || user.getPassword() == null) {
            return ResponseEntity.status(400).body(Result.error(400, "Email or password is empty"));
        }
        //TODO: 是否可以删除redis中的token？
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        //!这里是根据email去修改密码的，在上一步的resetPassword方法中已经将email存入了user对象中
        userMapper.changePassword(user);
        redisTemplate.delete(user.getEmail());
        redisTemplate.delete("temp_user:" + user.getEmail() + ":resetPassword");

        return ResponseEntity.ok(Result.success("Password reset successfully"));
    }


}
