package com.demo.myapp.service;

import com.demo.myapp.controller.response.Result;
import com.demo.myapp.enums.UserAction;
import com.demo.myapp.pojo.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

/**
 * @Author: Yupeng Li
 * @Date: 1/7/2024 15:35
 * @Description:
 */
public interface LoginService {
    ResponseEntity<Result> login(User user, HttpServletResponse response);

    /**
     * register a new user and send the verification code to the user's email
     * @param user user information
     * @return if the verification code is sent successfully, return the success message, otherwise return the error message
     */
    ResponseEntity<Result> register(User user);

    ResponseEntity<Result> logout(HttpServletRequest request, HttpServletResponse response);

    /**
     * verify the verification code
     * @param email user's email
     * @param code verification code
     * @param action the action that the user is performing
     * @return if the verification code is correct, return the success message, otherwise return the error message
     */
    ResponseEntity<Result> verifyCode(String email, String code, UserAction action);

    ResponseEntity<Result> updateProfile(User user);

    ResponseEntity<Result> resetPassword(User user);

    ResponseEntity<Result> changePassword(User user);

}
