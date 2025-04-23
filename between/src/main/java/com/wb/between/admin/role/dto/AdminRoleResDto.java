package com.wb.between.admin.role.dto;

import com.wb.between.admin.role.domain.Role;
import com.wb.between.coupon.domain.Coupon;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminRoleResDto {

    private Long roleId;

    private String roleName;

    private String roleCode;

    private String description;

    private LocalDateTime createDt;

    public static AdminRoleResDto from(Role role) {
        return AdminRoleResDto.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .roleCode(role.getRoleCode())
                .description(role.getDescription())
                .createDt(role.getCreateDt())
                .build();
    }
}
