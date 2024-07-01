package com.demo.myapp.pojo;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Set;

/**
 * @Author: Yupeng Li
 * @Date: 1/7/2024 15:08
 * @Description:
 */

@Entity
@Data
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions;
}
