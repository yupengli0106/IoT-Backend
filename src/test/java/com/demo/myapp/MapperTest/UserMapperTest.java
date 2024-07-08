package com.demo.myapp.MapperTest;

import com.demo.myapp.mapper.PermissionMapper;
import com.demo.myapp.mapper.UserMapper;
import com.demo.myapp.pojo.User;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.List;

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
    @Resource
    PermissionMapper permissionMapper;

    @Test
    public void testBCrypt() {
        String password = "admin";
        String encode = bCryptPasswordEncoder.encode(password);
        System.out.println(encode);
    }

    @Test
    public void testDecode() {
        String hashPwd ="$2a$10$lE4ZYLi64hLsopv5bCgxBuWOf8OufbXXxMt2P7osM83rNKtVOez.e";
        boolean matches = bCryptPasswordEncoder.matches("123", hashPwd);
        assert matches;
    }

    @Test
    public void testFindByUsername() {
        User user = userMapper.findByUsername("admin");
        assert user.getUsername().equals("admin");
    }

    @Test
    public void testGetEmailByEmail() {
        String email = userMapper.getEmailByEmail("lypa520@gmail.com");
        assert email.equals("lypa520@gmail.com");
    }


    @Test
    public void testAddUser() {
        User user = new User("test1", "123123a", "test1@test1.com");
        userMapper.insertUser(user);
        User newUser = userMapper.findByUsername("test1");
        assert newUser.getUsername().equals("test1");
    }

    @Test
    public void testGetPermissionsByUsername() {
        List<String> permissions = new ArrayList<>();
        permissions.add("READ_PRIVILEGE");
        permissions.add("WRITE_PRIVILEGE");
        permissions.add("DELETE_PRIVILEGE");

        List<String> permissionsByEmail = permissionMapper.findPermissionsByUsername("admin");
        for (String permission : permissionsByEmail) {
            System.out.println(permission);
        }
        assert permissions.equals(permissionsByEmail);
    }
}
