package com.wb.between.admin.menu.dto;


import com.wb.between.admin.role.dto.AdminRoleResDto;
import com.wb.between.menu.domain.Menu;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Set;

@Getter
@Builder
public class MenuDetailResDto {

    private AdminMenuResDto adminMenuResDto;

    private List<AdminRoleResDto> allRoleDto;

    private Set<Long> assignedRoleIds;


}
