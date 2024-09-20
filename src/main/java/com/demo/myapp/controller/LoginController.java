package com.demo.myapp.controller;

import com.demo.myapp.controller.response.Result;
import com.demo.myapp.pojo.User;
import com.demo.myapp.service.LoginService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: Yupeng Li
 * @Date: 1/7/2024 15:21
 * @Description:
 */
@RestController
public class LoginController {
    @Resource
    LoginService loginService;


    @PostMapping("/login")
    public ResponseEntity<Result> login(@RequestBody User user, HttpServletResponse response) {
        return loginService.login(user, response);
    }

    @DeleteMapping("/logout")
    public ResponseEntity<Result> logout(HttpServletRequest request, HttpServletResponse response) {
        return loginService.logout(request, response);
    }

    @PostMapping("/register")
    public ResponseEntity<Result> register(@RequestBody User user) {
        return loginService.register(user);
    }

    @PostMapping("/verify-code")
    public ResponseEntity<Result> verifyCode(@RequestParam String email, @RequestParam String code, @RequestParam String action) {
        return loginService.verifyCode(email, code, action);
    }

    @GetMapping("/home")
    @PreAuthorize("hasAnyAuthority('DELETE_PRIVILEGE')")
    public String home() {
        return "Welcome to the home page!";
    }

    @PostMapping("/profile")
    public ResponseEntity<Result> profile(@RequestBody User user) {
        return loginService.updateProfile(user);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Result> resetPassword(@RequestBody User user) {
        return loginService.resetPassword(user);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Result> changePassword(@RequestBody User user) {
        return loginService.changePassword(user);
    }

}
