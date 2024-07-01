package com.demo.myapp.service;

import com.demo.myapp.controller.response.Result;
import com.demo.myapp.pojo.User;
import org.springframework.stereotype.Service;

/**
 * @Author: Yupeng Li
 * @Date: 1/7/2024 15:35
 * @Description:
 */
public interface LoginService {
    Result login(User user);

    Result register(User user);
}
