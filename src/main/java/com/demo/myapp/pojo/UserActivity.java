package com.demo.myapp.pojo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.sql.Timestamp;

/**
 * @Author: Yupeng Li
 * @Date: 2/8/2024 23:32
 * @Description:
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String username;
    private String deviceName;
    private String details;
    private Timestamp updateTime; //mysql会自动更新不需要设置，这里只是要返回给前端这个字段
}
