package com.demo.myapp.mapper;

import com.demo.myapp.pojo.Device;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * @Author: Yupeng Li
 * @Date: 11/7/2024 23:09
 * @Description:
 */
@Mapper
public interface DeviceMapper {
    @Select("SELECT * FROM devices")
    List<Device> findAllDevices();

    @Select("SELECT * FROM devices WHERE id = #{id}")
    Device findDeviceById(Long id);

    @Insert("INSERT INTO devices (name, type, status, user_id) VALUES (#{name}, #{type}, #{status}, #{userId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertDevice(Device device);

    @Update("UPDATE devices SET name = #{name}, type = #{type}, status=#{status} WHERE id = #{id} AND user_id = #{userId}")
    void updateDevice(Device device);

    @Delete("DELETE FROM devices WHERE id = #{id} AND user_id = #{userId}")
    void deleteDeviceById(Long id, Long userId);

    @Select("SELECT * FROM devices WHERE user_id = #{userId} LIMIT #{pageSize} OFFSET #{offset}")
    List<Device> findDevicesByPage(@Param("userId") Long userId, @Param("pageSize") int pageSize, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM devices WHERE user_id = #{userId}")
    long countDevices(@Param("userId") Long userId);

    @Select("SELECT * FROM devices WHERE name = #{name} AND user_id = #{userId}")
    Device findDeviceByName(String name, Long userId);
}
