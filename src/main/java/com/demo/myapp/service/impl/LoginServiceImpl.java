package com.demo.myapp.service.impl;

import com.demo.myapp.controller.response.Result;
import com.demo.myapp.enums.UserAction;
import com.demo.myapp.mapper.RoleMapper;
import com.demo.myapp.mapper.UserMapper;
import com.demo.myapp.pojo.LoginUser;
import com.demo.myapp.pojo.User;
import com.demo.myapp.service.LoginService;
import com.demo.myapp.utils.JwtUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
    @Resource
    JwtUtil jwtUtil;
    @Resource
    CachedUserService cachedUserService;

    private static final Logger log = LoggerFactory.getLogger(LoginServiceImpl.class);

    @Override
    public ResponseEntity<Result> login(User user, HttpServletResponse response) {
        // use SpringSecurity's AuthenticationManager to authenticate the user
        // 如果认证失败，会抛出异常，由全局异常处理器处理（AuthenticationException）
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));

        // Successful authentication
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        // user info for front-end
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("roles", loginUser.getRoles());
        userInfo.put("username", loginUser.getUser().getUsername());
        userInfo.put("email", loginUser.getUser().getEmail());
        userInfo.put("permissions", loginUser.getPermissions());
        userInfo.put("userId", loginUser.getUser().getId());

        // Generate a token by only storing the user id in the token
        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("userId", loginUser.getUser().getId());
        String token = jwtUtil.generateToken(tokenMap);

        // 将 token 设置为 HttpOnly 和 Secure Cookie
        Cookie jwtCookie = new Cookie("httpOnlyToken", token);
        jwtCookie.setHttpOnly(true); // 设置 HttpOnly 防止 XSS 攻击
        jwtCookie.setSecure(true); // 设置 Secure 确保只在 HTTPS 中传输
        jwtCookie.setMaxAge(24 * 60 * 60); // 设置有效期为 1 天（单位：秒）注意Token的有效期需不需要和Cookie的有效期一致
        jwtCookie.setPath("/"); // 适用于整个应用
        //            jwtCookie.setDomain("localhost"); // 这个应该是你的域名，只有在这个域名下才能访问到这个 cookie
        response.addCookie(jwtCookie); // 将 cookie 添加到响应中

        // return the token and user info to the client
        // for stateless authentication, the server does not need to store the token
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("user", userInfo);
        responseData.put("message", "Login successful");

        return ResponseEntity.ok(Result.success(responseData));
    }

    @Override
    @Transactional
    public ResponseEntity<Result> register(@Valid User user) {
        String username = removeSpacesByRegex(user.getUsername());
        String email = removeSpacesByRegex(user.getEmail());
        // Check if the username already exists
        String dbUsername = userMapper.getUsernameByUsername(username);
        // Check if the email already exists
        String dbEmail = userMapper.getEmailByEmail(email);

        if (dbUsername != null && dbUsername.equals(username)) {
            return ResponseEntity.status(409).body(Result.error(409, "Username already exists"));
        } else if (email.equals(dbEmail)) {
            return ResponseEntity.status(409).body(Result.error(409, "Email already exists"));
        } else {
            return storeCodeInRedis(user, UserAction.REGISTER);
        }
    }

    @Override
    public ResponseEntity<Result> logout(HttpServletRequest request, HttpServletResponse response) {
        // Extract token using utility method
        String token = jwtUtil.extractTokenFromCookies(request);

        if (token != null) {
            // Invalidate the cookie
            Cookie cookie = new Cookie("httpOnlyToken", null);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);

            //设置黑名单blacklist
            Date tokenExpirationTime = jwtUtil.getTokenExpirationTime(token);
            long remainingTime = tokenExpirationTime.getTime() - System.currentTimeMillis();
            String redisTokenKey = "blacklist_tokens:" + token;
            redisTemplate.opsForValue().set(redisTokenKey, "blacklisted", remainingTime, TimeUnit.MILLISECONDS);

            return ResponseEntity.status(HttpStatus.OK).body(Result.success("Logout successful"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(400, "Token not found in cookies"));
        }
    }

    /**
     * public method to remove all spaces from a string
     *
     * @param string the string to remove spaces from
     * @return the string without spaces
     */
    private String removeSpacesByRegex(String string) {
        return string.replaceAll("\\s", "");
    }

    /**
     * public method to store the code in Redis and send the code to the user's email
     * @param user   the temp user to store in Redis
     * @param action the action to store in Redis
     * @return the response entity
     */
    private ResponseEntity<Result> storeCodeInRedis(User user, UserAction action) {
        // verify the email by sending a verification code
        String email = removeSpacesByRegex(user.getEmail());

        // 生成验证码并"先"存入Redis
        String code = emailService.generateVerificationCode();
        redisTemplate.opsForValue().set(email, code, 3, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set("temp_user:" + email + ":" + action.name(), user, 3, TimeUnit.MINUTES);

        // 异步发送验证码邮件
        CompletableFuture.runAsync(() -> {
            try {
                emailService.sendVerificationCode(email, code);
            } catch (Exception e) {
                // 邮件发送失败时，从Redis中删除验证码
                redisTemplate.delete(email);
                redisTemplate.delete("temp_user:" + email + ":" + action.name());
                // 记录发送邮件的异常
                log.error("Failed to send verification code to email: {}", email, e);
            }
        });

        // 异步操作，通知用户验证码已发送
        return ResponseEntity.ok(Result.success("Verification code is being sent to your email"));
    }

    @Override
    @Transactional
    public ResponseEntity<Result> verifyCode(String email, String code, UserAction action) {
        // 去除所有空格
        email = removeSpacesByRegex(email);
        code = removeSpacesByRegex(code);
        String storedCode = (String) redisTemplate.opsForValue().get(email);
        if (storedCode != null && storedCode.equals(code)) {
            // 验证成功，根据不同的action执行不同的操作
            User tempUser = (User) redisTemplate.opsForValue().get("temp_user:" + email + ":" + action);
            if (tempUser != null) {
                return switch (action) {
                    case REGISTER -> completeRegistration(tempUser);
                    case UPDATE_PROFILE -> completeProfileUpdate(tempUser);
                    //告诉前端邮箱验证成功，重定向到修改密码的页面
                    case RESET_PASSWORD -> ResponseEntity.status(202).body(Result.success("Verification successful"));
                };
            } else {
                return ResponseEntity.status(400).body(Result.error(400, "Temporary user data not found"));
            }
        } else {
            return ResponseEntity.status(400).body(Result.error(400, "Invalid verification code"));
        }
    }

    private ResponseEntity<Result> completeProfileUpdate(User user) {
        userMapper.updateUser(user);
        redisTemplate.delete(user.getEmail());
        redisTemplate.delete("temp_user:" + user.getEmail() + ":updateProfile");

        // 每当用户更新profile，移除所有的用户缓存，以便下次查询时重新加载数据
        evictAllUserCaches(user.getId());

        // 创建新的 LoginUser 对象，使用更新Spring Security上下文中的 Authentication 对象
        List<String> permissions = cachedUserService.getPermissionsByUserId(user.getId());
        List<String> roles = cachedUserService.getRolesByUserId(user.getId());
        LoginUser updatedLoginUser = new LoginUser(user, permissions, roles);
        // 创建新的 Authentication 对象
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                updatedLoginUser, null, updatedLoginUser.getAuthorities()
        );
        // 在每次更新用户信息后，都需要更新一下 SecurityContextHolder 中的 Authentication 对象
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        return ResponseEntity.ok(Result.success("Profile updated successfully"));
    }

    private ResponseEntity<Result> completeRegistration(User user) {
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
        if (user == null || user.getEmail() == null) {
            return ResponseEntity.status(400).body(Result.error(400, "Email or other fields are empty"));
        }
        //!这里需要先获取当前登录用户的ID，然后再进行下一步更新用户信息。
        //!如果到下一步 completeProfileUpdate 再去获取当前登录用户的ID，可能会出现用户未认证的情况
        Long userId = userService.getCurrentUserId();
        user.setId(userId);

        // 获取当前用户的 email
        String currentEmail = userService.getCurrentUserEmail();
        String newEmail = removeSpacesByRegex(user.getEmail());

        //如果修改了邮箱则需要验证邮箱验证码
        if (!currentEmail.equals(newEmail)) {
            //检查新邮箱是否已存在
            if (userMapper.getEmailByEmail(newEmail) != null) {
                return ResponseEntity.status(400).body(Result.error(400, "Email already exists"));
            }

            user.setEmail(newEmail);// 重新设置邮箱
            user.setUsername(user.getUsername());// 重新设置用户名

            //验证邮箱
            return storeCodeInRedis(user, UserAction.UPDATE_PROFILE);
        } else {//如果没有修改邮箱，则直接更新用户信息
            return completeProfileUpdate(user);
        }

    }

    @Override
    public ResponseEntity<Result> resetPassword(User user) {
        if (user == null || user.getEmail() == null) {
            return ResponseEntity.status(400).body(Result.error(400, "Email or other fields are empty"));
        } else if (userMapper.getEmailByEmail(removeSpacesByRegex(user.getEmail())) == null) {
            return ResponseEntity.status(400).body(Result.error(400, "Email does not exist"));
        }
        return storeCodeInRedis(user, UserAction.RESET_PASSWORD);
    }

    @Override
    @Transactional
    public ResponseEntity<Result> changePassword(User user) {
        if (user == null || user.getEmail() == null || user.getPassword() == null) {
            return ResponseEntity.status(400).body(Result.error(400, "Email or password is empty"));
        }
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        //!这里是根据email去修改密码的，在上一步的resetPassword方法中已经将email存入了user对象中
        userMapper.changePassword(user);

        return ResponseEntity.ok(Result.success("Password reset successfully"));
    }

    /**
     * 移除所有用户缓存
     *
     * @param userId 用户ID
     */
    private void evictAllUserCaches(Long userId) {
        cachedUserService.evictUserCache(userId);
        cachedUserService.evictRolesCache(userId);
        cachedUserService.evictPermissionsCache(userId);
    }

}
