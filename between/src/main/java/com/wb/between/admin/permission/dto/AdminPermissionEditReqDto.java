package com.wb.between.admin.permission.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class AdminPermissionEditReqDto {

    private Long permissionId;

    private String permissionCode;

    private String permissionName;

    private String description;


}
