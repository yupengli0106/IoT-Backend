package com.demo.myapp.service;

import com.demo.myapp.controller.response.Result;
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
     * after the Register API is called, the user will receive an email with a verification code,
     * then the user will call this API to verify the code
     * @param email the email address
     * @param code the code from the user
     * @return the result of the verification, if the code is correct, insert the user into the database, otherwise return the error message
     */
    ResponseEntity<Result> verifyCode(String email, String code, String action);

    ResponseEntity<Result> updateProfile(User user);

    ResponseEntity<Result> resetPassword(User user);

    ResponseEntity<Result> changePassword(User user);

}
