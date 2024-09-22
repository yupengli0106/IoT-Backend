package com.demo.myapp.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @Author: Yupeng Li
 * @Date: 3/7/2024 17:32
 * @Description:
 */
@Mapper
public interface PermissionMapper {
    List<String> findPermissionsByUsername(String username);

    List<String> findPermissionsByUserId(Long userId);
}
