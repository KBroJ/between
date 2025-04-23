package com.wb.between.admin.permission.service;

import com.wb.between.admin.permission.domain.Permission;
import com.wb.between.admin.permission.dto.AdminPermissionResDto;
import com.wb.between.admin.permission.repository.AdminPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminPermissionService {

    private final AdminPermissionRepository adminPermissionRepository;

    public Page<AdminPermissionResDto> findAdminPermissionList(Pageable pageable, String searchPermissionName) {
        Page<Permission> permissionList = adminPermissionRepository.findPermissionWithFilter(pageable, searchPermissionName);
        return permissionList.map(AdminPermissionResDto::from);
    }

}
