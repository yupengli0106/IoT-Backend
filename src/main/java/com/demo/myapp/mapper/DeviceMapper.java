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

    @Insert("INSERT INTO devices (name, type, status) VALUES (#{name}, #{type}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertDevice(Device device);

    @Update("UPDATE devices SET name = #{name}, type = #{type}, status = #{status} WHERE id = #{id}")
    void updateDevice(Device device);

    @Delete("DELETE FROM devices WHERE id = #{id}")
    void deleteDeviceById(Long id);
}
