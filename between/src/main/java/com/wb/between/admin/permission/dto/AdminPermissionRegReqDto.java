package com.wb.between.admin.permission.dto;

import com.wb.between.admin.permission.domain.Permission;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminPermissionRegReqDto {

    private Long permissionId;

    private String permissionCode;

    private String permissionName;

    private String description;


}
