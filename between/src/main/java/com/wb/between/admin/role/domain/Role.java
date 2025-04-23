package com.wb.between.admin.role.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roleId;

    private String roleCode;

    private String roleName;

    private String description;

    private LocalDateTime createDt;

    private LocalDateTime updateDt;
}
