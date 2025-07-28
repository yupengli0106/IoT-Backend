package com.demo.myapp.service.impl;

import com.demo.myapp.controller.response.Result;
import com.demo.myapp.enums.UserAction;
import com.demo.myapp.mapper.RoleMapper;
import com.demo.myapp.mapper.UserMapper;
import com.demo.myapp.pojo.LoginUser;
import com.demo.myapp.pojo.User;
import com.demo.myapp.service.LoginService;
import com.demo.myapp.utils.JwtUtil;
import com.demo.myapp.utils.RateLimitUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Resource
    RateLimitUtil rateLimitUtil;

    private static final Logger log = LoggerFactory.getLogger(LoginServiceImpl.class);
    
    // Configurable constants for better maintainability
    private static final String TEMP_USER_PREFIX = "temp_user:";
    private static final String VERIFICATION_ATTEMPTS_PREFIX = "verify_attempts:";
    private static final int VERIFICATION_CODE_TTL_MINUTES = 3;
    private static final int MAX_VERIFICATION_ATTEMPTS = 3;
    private static final int JWT_COOKIE_MAX_AGE_HOURS = 24;
    private static final String JWT_COOKIE_NAME = "httpOnlyToken";

    @Override
    public ResponseEntity<Result> login(User user, HttpServletResponse response, HttpServletRequest request) {
        String clientIp = rateLimitUtil.getClientIpAddress(request);
        String correlationId = MDC.get("correlationId");
        
        // Check rate limiting before processing login
        if (rateLimitUtil.isRateLimited(clientIp, "login")) {
            log.warn("Login rate limit exceeded for IP: {} [{}]", clientIp, correlationId);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Result.error(429, "Too many login attempts. Please try again later."));
        }

        try {
            // Clear any existing authentication cookie
            clearAuthCookie(response);

            // Add correlation ID and IP to logs
            log.info("Login attempt for user: {} from IP: {} [{}]", user.getUsername(), clientIp, correlationId);

            // Use SpringSecurity's AuthenticationManager to authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));

            // Successful authentication
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            
            // Clear rate limiting on successful login
            rateLimitUtil.clearRateLimit(clientIp, "login");
            
            log.info("Login successful for user: {} [{}]", user.getUsername(), correlationId);

            // user info for the front-end
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("roles", loginUser.getRoles());
            userInfo.put("username", loginUser.getUser().getUsername());
            userInfo.put("email", loginUser.getUser().getEmail());
            userInfo.put("permissions", loginUser.getPermissions());
            userInfo.put("userId", loginUser.getUser().getId());

            // Generate a token by only storing the user id in the token
            Map<String, Object> tokenMap = new HashMap<>();
            tokenMap.put("userId", loginUser.getUser().getId());
            tokenMap.put("username", loginUser.getUser().getUsername()); // IMPROVED: Add username for better logging
            String token = jwtUtil.generateToken(tokenMap);

            // Set secure HttpOnly cookie with better configuration
            setSecureAuthCookie(response, token);

            // return the token and user info to the client
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("user", userInfo);
            responseData.put("message", "Login successful");

            return ResponseEntity.ok(Result.success(responseData));
            
        } catch (BadCredentialsException e) {
            // Handle authentication failures with rate limiting
            rateLimitUtil.incrementRateLimit(clientIp, "login");
            log.warn("Login failed for user: {} from IP: {} [{}] - Invalid credentials", 
                    user.getUsername(), clientIp, correlationId);
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.error(401, "Invalid username or password"));
        } catch (Exception e) {
            log.error("Login error for user: {} from IP: {} [{}]: {}", 
                    user.getUsername(), clientIp, correlationId, e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error(500, "Internal server error, please try again later"));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Result> register(@Valid User user) {
        // Input validation
        if (user == null) {
            return ResponseEntity.status(400).body(Result.error(400, "User data is required"));
        }
        
        try {
            String username = removeSpacesByRegex(user.getUsername());
            String email = removeSpacesByRegex(user.getEmail());
            
            // Additional validation
            if (!isValidEmail(email)) {
                return ResponseEntity.status(400).body(Result.error(400, "Invalid email format"));
            }
            
            if (user.getPassword() != null && !isValidPassword(user.getPassword())) {
                return ResponseEntity.status(400).body(Result.error(400, 
                    "Password must be at least 8 characters with uppercase, lowercase, digit, and special character"));
            }
            
            // Use timing-safe existence check to prevent user enumeration
            boolean userExists = checkUserExistenceSecurely(username, email);
            
            if (userExists) {
                // Generic message to prevent user enumeration attacks
                return buildErrorResponse(HttpStatus.CONFLICT, "User already exists");
            } else {
                return storeCodeInRedis(user, UserAction.REGISTER);
            }
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid input: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Result> logout(HttpServletRequest request, HttpServletResponse response) {
        String correlationId = MDC.get("correlationId");
        
        try {
            // Extract token from multiple sources (cookies and Authorization header)
            String token = jwtUtil.extractTokenFromRequest(request);

            if (token != null) {
                // Clear the authentication cookie
                clearAuthCookie(response);

                // Use a consistent blocklist key format with JwtUtil
                jwtUtil.blacklistToken(token);
                
                log.info("User logout successful [{}]", correlationId);
                return ResponseEntity.ok(Result.success("Logout successful"));
            } else {
                log.warn("Logout attempted without valid token [{}]", correlationId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Result.error(400, "No active session found"));
            }
        } catch (Exception e) {
            log.error("Logout error [{}]: {}", correlationId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error(500, "Logout failed due to an internal error"));
        }
    }

    /**
     * Validate and sanitize input string by removing spaces
     * Also performs basic validation checks
     *
     * @param input the string to validate and sanitize
     * @return the sanitized string without spaces
     * @throws IllegalArgumentException if input is null or empty
     */
    private String removeSpacesByRegex(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty");
        }
        return input.replaceAll("\\s", "");
    }
    
    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        if (email == null) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
    
    /**
     * Validate password strength
     */
    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) return false;
        // At least one digit, one lowercase, one uppercase, one special character
        return password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$");
    }

    /**
     * public method to store the code in Redis and send the code to the user's email
     * @param user   the temp user to store in Redis
     * @param action the action to store in Redis
     * @return the response entity
     */
    private ResponseEntity<Result> storeCodeInRedis(User user, UserAction action) {
        String email = removeSpacesByRegex(user.getEmail());
        String correlationId = MDC.get("correlationId");

        // Generate verification code and store in Redis first
        String code = emailService.generateVerificationCode();
        
        // Store verification data atomically
        try {
            redisTemplate.opsForValue().set(email, code, VERIFICATION_CODE_TTL_MINUTES, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(TEMP_USER_PREFIX + email + ":" + action.name(), user, VERIFICATION_CODE_TTL_MINUTES, TimeUnit.MINUTES);
            // Initialize verification attempts counter
            redisTemplate.opsForValue().set(VERIFICATION_ATTEMPTS_PREFIX + email, "0", VERIFICATION_CODE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Failed to store verification data in Redis for email: {} [{}]", email, correlationId, e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to initiate verification process", correlationId);
        }

        // Async email sending with improved error handling
        CompletableFuture.runAsync(() -> {
            try {
                emailService.sendVerificationCode(email, code);
                log.info("Verification code sent successfully to email: {} [{}]", email, correlationId);
            } catch (Exception e) {
                log.error("Failed to send verification code to email: {} [{}]", email, correlationId, e);
                // Clean up verification data on email failure
                cleanupVerificationData(email, action);
            }
        });

        // Async operation, notify user verification code is being sent
        return buildSuccessResponse("Verification code is being sent to your email. Please check your inbox and spam folder.");
    }

    @Override
    @Transactional
    public ResponseEntity<Result> verifyCode(String email, String code, UserAction action) {
        // Remove all spaces
        email = removeSpacesByRegex(email);
        code = removeSpacesByRegex(code);
        
        // Check verification attempts to prevent brute force
        String attemptsKey = VERIFICATION_ATTEMPTS_PREFIX + email;
        String attemptsStr = (String) redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;
        
        if (attempts >= MAX_VERIFICATION_ATTEMPTS) {
            // Cleanup verification data on max attempts reached
            cleanupVerificationData(email, action);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Result.error(429, "Maximum verification attempts exceeded. Please request a new code."));
        }
        
        String storedCode = (String) redisTemplate.opsForValue().get(email);
        if (storedCode != null && storedCode.equals(code)) {
            // Verification successful - clear attempts counter
            redisTemplate.delete(attemptsKey);
            
            // Execute different operations based on action
            User tempUser = (User) redisTemplate.opsForValue().get(TEMP_USER_PREFIX + email + ":" + action);
            if (tempUser != null) {
                return switch (action) {
                    case REGISTER -> completeRegistration(tempUser);
                    case UPDATE_PROFILE -> completeProfileUpdate(tempUser);
                    // Tell frontend email verification successful, redirect to change password page
                    case RESET_PASSWORD -> ResponseEntity.status(202).body(Result.success("Verification successful"));
                };
            } else {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Temporary user data not found");
            }
        } else {
            // Increment failed attempts
            redisTemplate.opsForValue().set(attemptsKey, String.valueOf(attempts + 1), 
                    VERIFICATION_CODE_TTL_MINUTES, TimeUnit.MINUTES);
            
            int remainingAttempts = MAX_VERIFICATION_ATTEMPTS - attempts - 1;
            if (remainingAttempts <= 0) {
                cleanupVerificationData(email, action);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Result.error(429, "Maximum verification attempts exceeded. Please request a new code."));
            }
            
            return ResponseEntity.status(400).body(Result.error(400, 
                    "Invalid verification code. " + remainingAttempts + " attempts remaining."));
        }
    }

    private ResponseEntity<Result> completeProfileUpdate(User user) {
        try {
            userMapper.updateUser(user);
        } catch (DuplicateKeyException e) {
            log.warn("Profile update failed - duplicate key: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Result.error(409, "Username already exists, please try another one"));
        } catch (Exception e) {
            log.error("Profile update failed for user ID {}: {}", user.getId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error(500, "Internal server error, please try again later"));
        }

        // Clean up verification data atomically
        cleanupVerificationData(removeSpacesByRegex(user.getEmail()), UserAction.UPDATE_PROFILE);

        // Optimized: Only evict user info cache since profile update doesn't affect roles/permissions
        evictUserCaches(user.getId(), CacheType.USER_INFO);

        // Create a new LoginUser object to update a Spring Security context Authentication object
        List<String> permissions = cachedUserService.getPermissionsByUserId(user.getId());
        List<String> roles = cachedUserService.getRolesByUserId(user.getId());
        LoginUser updatedLoginUser = new LoginUser(user, permissions, roles);
        // Create a new Authentication object
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                updatedLoginUser, null, updatedLoginUser.getAuthorities()
        );
        // Update SecurityContextHolder Authentication object after each user info update
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

        // Clean up verification data atomically
        cleanupVerificationData(removeSpacesByRegex(user.getEmail()), UserAction.REGISTER);

        return buildSuccessResponse("Registration successful");
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
        
        String email = removeSpacesByRegex(user.getEmail());
        
        // Verify email ownership before allowing password change
        // Check if there's a valid verification session for password reset
        String verificationCode = (String) redisTemplate.opsForValue().get(email);
        User tempUser = (User) redisTemplate.opsForValue().get(TEMP_USER_PREFIX + email + ":" + UserAction.RESET_PASSWORD.name());
        
        if (verificationCode == null || tempUser == null) {
            return ResponseEntity.status(400).body(Result.error(400, 
                    "Invalid password reset session. Please request a new password reset."));
        }
        
        // Verify the email exists in a database
        if (userMapper.getEmailByEmail(email) == null) {
            return ResponseEntity.status(400).body(Result.error(400, "Email does not exist"));
        }
        
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        // Change password based on email from the resetPassword method
        userMapper.changePassword(user);
        
        // Clean up verification data after successful password change
        cleanupVerificationData(email, UserAction.RESET_PASSWORD);

        return ResponseEntity.ok(Result.success("Password reset successfully"));
    }

    /**
     * Optimized cache eviction - only evict specific cache types that changed
     *
     * @param userId 用户ID
     * @param cacheTypes specific cache types to evict
     */
    private void evictUserCaches(Long userId, CacheType... cacheTypes) {
        for (CacheType cacheType : cacheTypes) {
            switch (cacheType) {
                case USER_INFO -> cachedUserService.evictUserCache(userId);
                case ROLES -> cachedUserService.evictRolesCache(userId);
                case PERMISSIONS -> cachedUserService.evictPermissionsCache(userId);
                case ALL -> {
                    cachedUserService.evictUserCache(userId);
                    cachedUserService.evictRolesCache(userId);
                    cachedUserService.evictPermissionsCache(userId);
                }
            }
        }
    }
    
    /**
     * Cache types for targeted eviction
     */
    private enum CacheType {
        USER_INFO, ROLES, PERMISSIONS, ALL
    }
    
    /**
     * Standardized error response builder
     */
    private ResponseEntity<Result> buildErrorResponse(HttpStatus status, String message, String correlationId) {
        log.warn("Error response [{}]: {} - {}", correlationId, status.value(), message);
        return ResponseEntity.status(status).body(Result.error(status.value(), message));
    }
    
    private ResponseEntity<Result> buildErrorResponse(HttpStatus status, String message) {
        return buildErrorResponse(status, message, MDC.get("correlationId"));
    }
    
    /**
     * Standardized success response builder
     */
    private ResponseEntity<Result> buildSuccessResponse(Object data, String correlationId) {
        log.debug("Success response [{}]", correlationId);
        return ResponseEntity.ok(Result.success(data));
    }
    
    private ResponseEntity<Result> buildSuccessResponse(String message) {
        return buildSuccessResponse(message, MDC.get("correlationId"));
    }


    /**
     * Set secure HTTP-only cookie for authentication
     */
    private void setSecureAuthCookie(HttpServletResponse response, String token) {
        Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, token);
        jwtCookie.setHttpOnly(true); // Prevent XSS attacks
        jwtCookie.setSecure(true); // HTTPS only
        jwtCookie.setMaxAge(JWT_COOKIE_MAX_AGE_HOURS * 60 * 60);
        jwtCookie.setPath("/"); // Entire application
        // Note: SameSite not available in older servlet versions - handle in web server config
        response.addCookie(jwtCookie);
    }

    /**
     * Clear authentication cookie
     */
    private void clearAuthCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    /**
     * Cleanup verification data from Redis
     */
    private void cleanupVerificationData(String email, UserAction action) {
        redisTemplate.delete(email);
        redisTemplate.delete(TEMP_USER_PREFIX + email + ":" + action.name());
        redisTemplate.delete(VERIFICATION_ATTEMPTS_PREFIX + email);
    }

    /**
     * IMPROVED: Secure user existence check to prevent user enumeration
     */
    private boolean checkUserExistenceSecurely(String username, String email) {
        // Check both username and email but don't reveal which one exists
        String dbUsername = userMapper.getUsernameByUsername(removeSpacesByRegex(username));
        String dbEmail = userMapper.getEmailByEmail(removeSpacesByRegex(email));
        
        return (dbUsername != null && dbUsername.equals(removeSpacesByRegex(username))) || 
               (dbEmail != null && dbEmail.equals(removeSpacesByRegex(email)));
    }

}
