package com.demo.myapp.service;

import com.demo.myapp.controller.response.Result;
import com.demo.myapp.pojo.User;
import org.springframework.http.ResponseEntity;

/**
 * @Author: Yupeng Li
 * @Date: 1/7/2024 15:35
 * @Description:
 */
public interface LoginService {
    ResponseEntity<Result> login(User user);

    ResponseEntity<Result> register(User user);

    ResponseEntity<Result> logout(String token);
}
