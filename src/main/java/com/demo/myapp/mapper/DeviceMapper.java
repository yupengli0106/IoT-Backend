package com.demo.myapp.mapper;

import com.demo.myapp.dto.DeviceStatsDTO;
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

    @Update("UPDATE devices SET name = #{name}, type = #{type} WHERE id = #{id} AND user_id = #{userId}")
    void editDevice(Device device);

    //批量删除，但是要注意mysql的in语句有长度限制。目前我们的项目还达不到这个限制，后续可以考虑分批删除。
    void deleteDevicesByIds(@Param("ids") List<Long> ids, @Param("userId") Long userId);

    @Select("SELECT * FROM devices WHERE user_id = #{userId} LIMIT #{pageSize} OFFSET #{offset}")
    List<Device> findDevicesByPage(@Param("userId") Long userId, @Param("pageSize") int pageSize, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM devices WHERE user_id = #{userId}")
    long countDevices(@Param("userId") Long userId);

    @Select("SELECT * FROM devices WHERE name = #{name} AND user_id = #{userId}")
    Device findDeviceByName(String name, Long userId);

    @Update("UPDATE devices SET status = #{status} WHERE id = #{id} AND user_id = #{userId}")
    void updateDeviceStatus(Device device);

    // 获取设备统计信息: totalDevices, onlineDevices, offlineDevices
    DeviceStatsDTO getDeviceStats(long userId);
}
