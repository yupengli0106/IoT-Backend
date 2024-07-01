package com.demo.myapp.mapper;

import com.demo.myapp.pojo.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @Author: Yupeng Li
 * @Date: 1/7/2024 15:32
 * @Description:
 */
@Mapper
public interface UserMapper {
    @Select("select password from users where username = #{username}")
    String getPasswordByUsername(String username);

    @Select("select * from users where username = #{username}")
    User findByUsername(String username);

    @Insert("insert into users(username,password,email) values(#{username},#{password},#{email})")
    void insertUser(User newUser);

    @Select("select email from users where email = #{email}")
    String getEmailByEmail(String email);
}
