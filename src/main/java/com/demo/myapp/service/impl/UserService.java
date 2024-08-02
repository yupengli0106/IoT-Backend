package com.demo.myapp.service.impl;

import com.demo.myapp.pojo.LoginUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * @Author: Yupeng Li
 * @Date: 20/7/2024 15:37
 * @Description: use this service to get the current user information from the security context
 */

@Service
public class UserService {

    public LoginUser getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof LoginUser) {
            return (LoginUser) principal;
        } else {
            throw new IllegalStateException("User not authenticated");
        }
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getUser().getId();
    }

    public String getCurrentUsername() {
        return getCurrentUser().getUser().getUsername();
    }
}

