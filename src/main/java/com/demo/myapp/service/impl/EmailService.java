package com.demo.myapp.service.impl;

import jakarta.annotation.Resource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * @Author: Yupeng Li
 * @Date: 8/7/2024 17:41
 * @Description:
 */
@Service
public class EmailService {
    @Resource
    private JavaMailSender mailSender;

    public String sendVerificationCode(String to) {
        String code = generateVerificationCode();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Email Verification Code: " + code);
        message.setText("Your verification code is: " + code
                + "\n"
                + "please use it within 3 minutes, and do not tell others.");
//        message.setFrom("your-email@gmail.com");

        mailSender.send(message);
        return code;
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 生成6位数字验证码
        return String.valueOf(code);
    }

}
