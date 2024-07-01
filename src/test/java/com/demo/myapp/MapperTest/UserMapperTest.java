package com.demo.myapp.MapperTest;

import com.demo.myapp.mapper.UserMapper;
import com.demo.myapp.pojo.User;
import jakarta.annotation.Resource;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * @Author: Yupeng Li
 * @Date: 1/7/2024 16:01
 * @Description:
 */

@SpringBootTest
public class UserMapperTest {
    @Resource
    UserMapper userMapper;
    @Resource
    BCryptPasswordEncoder bCryptPasswordEncoder;

    @Test
    public void testGetPasswordByUsername() {
        String password = userMapper.getPasswordByUsername("admin");
        String pwd = bCryptPasswordEncoder.encode("123123a");
        System.out.println(pwd);
//        System.out.println(password);
        assert password.equals("$2a$10$JtQ3./Wvh3KKL.QB3FYOve6yZUbT.lLZZaH2ak5p/8N7FH/yZP43W");
    }

    @Test
    public void testAddUser() {
        User user = new User("test", "123123a", "lypa520@e.com");
        userMapper.insertUser(user);
        User newUser = userMapper.findByUsername("test");
        assert newUser.getUsername().equals("test");
    }
}
