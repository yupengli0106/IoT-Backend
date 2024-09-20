package com.demo.myapp.utils;


import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: Yupeng Li
 * @Date: 2/7/2024 00:38
 * @Description:
 */
@SpringBootTest
public class JwtUtilTest {
    @Resource
    private JwtUtil jwtUtil;

    @Test
    public void testGenerateToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user", "User");
        claims.put("test", 1);
        String token =  jwtUtil.generateToken(claims);
        System.out.println(token);
    }

    @Test
    public void testParseToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user", "User");
        claims.put("test", 1);
        String token = jwtUtil.generateToken(claims);

        Map<String, Object> stringObjectMap = jwtUtil.parseToken(token);
        System.out.println(stringObjectMap);
    }

    @Test
    public void testIsValidToken(){
        Map<String, Object> claims = new HashMap<>();
        claims.put("user", "User");
        claims.put("test", 1);
        String token = jwtUtil.generateToken(claims);

       boolean isValid = jwtUtil.isValidToken(token);
       assert isValid;
    }

    @Test
    public void testGetTokenExpirationTime(){
        Map<String, Object> claims = new HashMap<>();
        claims.put("user", "User");
        claims.put("test", 1);
        String token =  jwtUtil.generateToken(claims);

        Date tokenExpirationTime = jwtUtil.getTokenExpirationTime(token);
        System.out.println("Token expiration at date: " + tokenExpirationTime);

        long remainingTime = tokenExpirationTime.getTime() - System.currentTimeMillis();
        System.out.println("Remaining time to be expired in Redis: " + remainingTime + " milliseconds");
    }
}
