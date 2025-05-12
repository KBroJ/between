package com.wb.between.admin.role.dto;

import com.wb.between.admin.role.domain.Role;
import com.wb.between.admin.rolepermission.domain.RolePermission;
import com.wb.between.coupon.domain.Coupon;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class AdminRoleResDto {

    private Long roleId;

    private String roleName;

    private String roleCode;

    private String description;

    private LocalDateTime createDt;
    
    //권한 목록
    private List<Long> permissionIds;

    public static AdminRoleResDto from(Role role) {
        
        List<Long> permissionIds = new ArrayList<>();
        
        //권한 목록 입력
        for(RolePermission rp : role.getRolePermissions()) {
            permissionIds.add(rp.getPermission().getPermissionId());
        }
                
        return AdminRoleResDto.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .roleCode(role.getRoleCode())
                .description(role.getDescription())
                .createDt(role.getCreateDt())
                .permissionIds(permissionIds)
                .build();
    }
}
