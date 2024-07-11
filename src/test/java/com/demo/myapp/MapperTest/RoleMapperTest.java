package com.demo.myapp.MapperTest;

import com.demo.myapp.mapper.RoleMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @Author: Yupeng Li
 * @Date: 11/7/2024 15:32
 * @Description:
 */
@SpringBootTest
public class RoleMapperTest {
    @Resource
    private RoleMapper roleMapper;

    @Test
    public void testFindRolesByUserId() {
        System.out.println(roleMapper.findRolesByUserId(2L));
    }
}
