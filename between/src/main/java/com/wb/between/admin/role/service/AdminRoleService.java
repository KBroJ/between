package com.wb.between.admin.role.service;

import com.wb.between.admin.role.domain.Role;
import com.wb.between.admin.role.dto.AdminRoleResDto;
import com.wb.between.admin.role.repository.AdminRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminRoleService {

    private final AdminRoleRepository adminRoleRepository;

    public Page<AdminRoleResDto> findAdminRoleList(Pageable pageable, String searchRoleName) {
        Page<Role> adminRoleList = adminRoleRepository.findRoleWithFilter(pageable, searchRoleName);

        return adminRoleList.map(AdminRoleResDto::from);
    }
}
