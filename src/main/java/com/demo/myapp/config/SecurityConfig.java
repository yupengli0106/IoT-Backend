package com.demo.myapp.config;

import com.demo.myapp.filter.JwtRequestFilter;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * @Author: Yupeng Li
 * @Date: 1/7/2024 16:38
 * @Description:
 */

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // controller 访问权限控制
public class SecurityConfig {
    @Resource
    private JwtRequestFilter jwtRequestFilter;

    /**
     * Security filter chain
     * @param http HttpSecurity instance
     * @return  SecurityFilterChain instance
     * @throws Exception exception
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // disable csrf
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login","/register","/verify-code","/forgot-password","/change-password").permitAll()// permit request without authentication
                        .requestMatchers("/ws/**").permitAll()// permit websocket request without authentication
                        .requestMatchers("/swagger-ui/**","/v3/**").permitAll() // OpenAPI documents
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .logout(AbstractHttpConfigurer::disable);// disable logout otherwise it will conflict with our custom logout

        return http.build();
    }


    /**
     * Password encoder
     * @return BCryptPasswordEncoder instance.
     * @Description: Password encoder for password encryption and decryption.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    /**
     * Authentication manager
     * @param authenticationConfiguration AuthenticationConfiguration instance
     * @return AuthenticationManager instance
     * @throws Exception exception
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

}
