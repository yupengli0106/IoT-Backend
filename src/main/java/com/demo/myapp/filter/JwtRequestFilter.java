package com.demo.myapp.filter;

import com.demo.myapp.pojo.LoginUser;
import com.demo.myapp.pojo.User;
import com.demo.myapp.utils.JwtUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @Author: Yupeng Li
 * @Date: 2/7/2024 14:36
 * @Description: Filter to intercept the request and validate the JWT token before it reaches the controller
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    @Resource
    JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 从请求中提取出token
        String token = jwtUtil.extractTokenFromCookies(request);

        // if token is null clear context and pass the request to the next filter(usernamePasswordAuthenticationFilter)
        if (token == null) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        // check if the token is valid
        if (!jwtUtil.isValidToken(token)){
            SecurityContextHolder.clearContext();
            // 401 Unauthorized
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"message\": \"Invalid token\"}");
            return;
        }

        //get the LoginUser after passed all the checks
        LoginUser loginUser = getLoginUser(token);

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

    /**
     * parse the token and get the user information to create a LoginUser object
     * @param token token to parse
     * @return LoginUser object
     */
    private LoginUser getLoginUser(String token) {
        // Parse the token to get the user information
        Map<String, Object> claims = jwtUtil.parseToken(token);

        // Extract user information from the token
        String username = claims.get("username").toString();
        Long userId = Long.parseLong(claims.get("userId").toString());
        String email = claims.get("email").toString();
        List<String> roles = (List<String>) claims.get("roles");
        List<String> permissions = (List<String>) claims.get("permissions");

        // Create a user object
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setEmail(email);
        // Password is not included in the token for security reasons
        user.setPassword(null);
        user.setEnabled(true);

        return new LoginUser(user, permissions, roles);
    }


}