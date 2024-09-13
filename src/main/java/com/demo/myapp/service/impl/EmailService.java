package com.demo.myapp.service.impl;

import jakarta.annotation.Resource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.CompletableFuture;

/**
 * @Author: Yupeng Li
 * @Date: 8/7/2024 17:41
 * @Description:
 */
@Service
public class EmailService {
    @Resource
    private JavaMailSender mailSender;

    /**
     * Asynchronously send verification code to the email address
     * @param to the email address
     * @return the verification code in CompletableFuture object if the email is sent successfully, otherwise return the exception
     */
    @Async("taskExecutor")
    public CompletableFuture<String> sendVerificationCode(String to) {
        String code = generateVerificationCode();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Email Verification Code: " + code);
        message.setText("Your verification code is: " + code
                + "\n"
                + "please use it within 3 minutes, and do not tell others.");

        try {
            mailSender.send(message);
            return CompletableFuture.completedFuture(code);
        } catch (MailException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();//SecureRandom 比 Random 更安全, 但是速度慢一些

        int code = 100000 + random.nextInt(900000); // 生成6位数字验证码
        return String.valueOf(code);
    }

}
