package com.demo.myapp.utils;

import com.auth0.jwt.interfaces.Claim;
import com.demo.myapp.pojo.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: Yupeng Li
 * @Date: 2/7/2024 00:38
 * @Description:
 */
@SpringBootTest
public class JWT {
    @Test
    public void testGenerateToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user", "User");
        claims.put("test", 1);
        String token =  JwtUtil.generateToken(claims);
        System.out.println(token);
    }

    @Test
    public void testParseToken() {
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyQ2xhaW1zIjp7InRlc3QiOjEsInVzZXIiOiJVc2VyIn0sImlzcyI6Ik15QXBwIiwiaWF0IjoxNzE5ODQ1NDQyLCJleHAiOjE3MTk5MzE4NDJ9.ccwPlSXJkZncfYSnFSs59amlBeeAR4iLaW7kmQdrpeo";
        Claim claim = JwtUtil.parseToken(token);
        System.out.println(claim.asMap());
    }

    @Test
    public void testIsValidToken(){
        Map<String, Object> claims = new HashMap<>();
        claims.put("user", "User");
        claims.put("test", 1);
        String token =  JwtUtil.generateToken(claims);
        System.out.println(JwtUtil.isValidToken(token));
    }
}
