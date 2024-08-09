package com.demo.myapp.mapper;

import com.demo.myapp.pojo.User;
import org.apache.ibatis.annotations.*;

/**
 * @Author: Yupeng Li
 * @Date: 1/7/2024 15:32
 * @Description:
 */
@Mapper
public interface UserMapper {
    @Select("select * from users where username = #{username}")
    User findByUsername(String username);

    @Insert("insert into users(username,password,email) values(#{username},#{password},#{email})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertUser(User newUser);

    @Select("select email from users where email = #{email}")
    String getEmailByEmail(String email);

    @Select("select username from users where username = #{username}")
    String getUsernameByUsername(String username);

    @Update("update users set username = #{username}, email = #{email} where id=#{id}")
    void updateUser(User user);

    @Update("update users set password = #{password} where email = #{email}")
    void changePassword(User user);
}
