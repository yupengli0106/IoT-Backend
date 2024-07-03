package com.demo.myapp.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @Author: Yupeng Li
 * @Date: 3/7/2024 17:28
 * @Description:
 */
@Mapper
public interface RoleMapper {
    @Select("select id from roles where name = #{roleUser}")
    Long getRoleIdByRoleName(String roleUser);

    @Insert("insert into user_roles(user_id, role_id) values(#{userId}, #{roleId})")
    void insertUserRole(Long userId, Long roleId);
}
