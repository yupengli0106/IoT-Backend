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
        Map<String, Object> claims = new HashMap<>();
        claims.put("user", "User");
        claims.put("test", 1);
        String token = JwtUtil.generateToken(claims);

        Map<String, Object> stringObjectMap = JwtUtil.parseToken(token);
        System.out.println(stringObjectMap);
    }

    @Test
    public void testIsValidToken(){
        Map<String, Object> claims = new HashMap<>();
        claims.put("user", "User");
        claims.put("test", 1);
        String token = JwtUtil.generateToken(claims);

       boolean isValid = JwtUtil.isValidToken(token);
       assert isValid;
    }
}
