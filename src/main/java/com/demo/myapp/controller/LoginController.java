package com.demo.myapp.controller;

import com.demo.myapp.controller.response.Result;
import com.demo.myapp.pojo.User;
import com.demo.myapp.service.LoginService;
import jakarta.annotation.Resource;
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
    public ResponseEntity<Result> login(@RequestBody User user) {
        return loginService.login(user);
    }

    @DeleteMapping("/logout")
    public ResponseEntity<Result> logout(@RequestHeader("Authorization") String token) {
        return loginService.logout(token);
    }

    @PostMapping("/register")
    public ResponseEntity<Result> register(@RequestBody User user) {
        return loginService.register(user);
    }

    @PostMapping("/verify-code")
    public ResponseEntity<Result> verifyCode(@RequestParam String email, @RequestParam String code) {
        return loginService.verifyCode(email, code);
    }

    @GetMapping("/home")
    @PreAuthorize("hasAnyAuthority('DELETE_PRIVILEGE')")
    public String home() {
        return "Welcome to the home page!";
    }

}
