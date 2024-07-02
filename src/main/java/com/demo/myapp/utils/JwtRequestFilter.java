package com.demo.myapp.utils;

import com.demo.myapp.pojo.LoginUser;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @Author: Yupeng Li
 * @Date: 2/7/2024 14:36
 * @Description: Filter to intercept the request and validate the JWT token before it reaches the controller
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    @Resource
    RedisTemplate<String, Object> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // get the token from the request header
        String token = request.getHeader("Authorization");
        // if the token is not null and starts with "Bearer ", remove "Bearer " from the token
        if (token !=null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // if token is null clear context and pass the request to the next filter(usernamePasswordAuthenticationFilter)
        if (token == null) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        // check if the token is valid
        if (!JwtUtil.isValidToken(token)){
            SecurityContextHolder.clearContext();
            throw new RuntimeException("invalid token");
        }

        // check if the user is logged in
        LoginUser loginUser = (LoginUser) redisTemplate.opsForValue().get(token);
        if (loginUser == null) {
            SecurityContextHolder.clearContext();
            throw new RuntimeException("user not logged in");
//            filterChain.doFilter(request, response);
//            return;
        }

        // pass all the authentication checks, current user is logged in, then store the user in the security context
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        // 从当前的HTTP请求中提取出相关的信息（如请求的IP地址和会话ID）并将其填充到WebAuthenticationDetails中
        usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        // 将UsernamePasswordAuthenticationToken对象存储到SecurityContextHolder中
        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

        // allow the request to pass to the controller after all the checks
        filterChain.doFilter(request, response);

    }
}
