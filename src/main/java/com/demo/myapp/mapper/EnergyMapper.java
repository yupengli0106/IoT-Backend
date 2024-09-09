package com.demo.myapp.mapper;

import com.demo.myapp.pojo.Energy;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @Author: Yupeng Li
 * @Date: 9/9/2024 19:26
 * @Description:
 */
@Mapper
public interface EnergyMapper {
    @Insert("INSERT INTO energy_data_daily (device_id, user_id, record_date, energy) VALUES (#{deviceId}, #{userId}, #{recordDate}, #{energy})")
    void insertEnergy(Energy energy);

    @Select("SELECT * FROM energy_data_daily where user_id = #{user_id} ORDER BY record_date")
    List<Energy> getAllEnergy(Long user_id);
}
