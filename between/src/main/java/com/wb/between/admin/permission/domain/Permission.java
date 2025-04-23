package com.wb.between.admin.permission.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;

import java.time.LocalDateTime;

//@Entity
@Getter
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long permissionId;

    private String permissionCode;

    private String permissionName;

    private String description;

    private LocalDateTime createDt;

    private LocalDateTime updateDt;
}
