package com.demo.myapp.controller;

import com.demo.myapp.controller.response.Result;
import com.demo.myapp.pojo.User;
import com.demo.myapp.service.LoginService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/logout")
    public ResponseEntity<Result> logout() {
        return null;
    }

    @PostMapping("/register")
    public ResponseEntity<Result> register(@RequestBody User user) {
        return loginService.register(user);
    }

    @GetMapping("/home")
    public String home() {
        return "Welcome to the home page!";
    }

}
