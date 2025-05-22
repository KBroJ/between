package com.wb.between.admin.permission.service;

import com.wb.between.admin.permission.domain.Permission;
import com.wb.between.admin.permission.dto.AdminPermissionEditReqDto;
import com.wb.between.admin.permission.dto.AdminPermissionRegReqDto;
import com.wb.between.admin.permission.dto.AdminPermissionResDto;
import com.wb.between.admin.permission.repository.AdminPermissionRepository;
import com.wb.between.common.exception.CustomException;
import com.wb.between.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminPermissionService {

    private final AdminPermissionRepository adminPermissionRepository;

    /**
     * 권한 목록 조회
     * @param pageable
     * @param searchPermissionName
     * @return
     */
    @Transactional(readOnly = true)
    public Page<AdminPermissionResDto> findAdminPermissionList(Pageable pageable, String searchPermissionName) {
        Page<Permission> permissionList = adminPermissionRepository.findPermissionWithFilter(pageable, searchPermissionName);
        return permissionList.map(AdminPermissionResDto::from);
    }

    /**
     * 권한 단일 조회
     */
    @Transactional(readOnly = true)
    public AdminPermissionResDto findAdminPermissionById(Long permissionId) {
       Permission permission = adminPermissionRepository.findById(permissionId).orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));
       return AdminPermissionResDto.from(permission);
    }

    /**
     * 권한 등록
     * @param adminPermissionRegReqDto
     */
    @Transactional
    public void permissionRegist(AdminPermissionRegReqDto adminPermissionRegReqDto) {
        Permission permission = Permission.builder()
                .permissionCode(adminPermissionRegReqDto.getPermissionCode())
                .permissionName(adminPermissionRegReqDto.getPermissionName())
                .description(adminPermissionRegReqDto.getDescription())
                .createDt(LocalDateTime.now())
                .build();

        adminPermissionRepository.save(permission);
    }

    public void permissionEdit(Long permissionId,
                               AdminPermissionEditReqDto adminPermissionEditReqDto) {

        //1. 권한 조회
        Permission permission = adminPermissionRepository.findById(permissionId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));

        //2. 권한 정보 수정
        permission.setPermissionCode(adminPermissionEditReqDto.getPermissionCode());
        permission.setPermissionName(adminPermissionEditReqDto.getPermissionName());
        permission.setDescription(adminPermissionEditReqDto.getDescription());

        adminPermissionRepository.save(permission);
    }

    // 삭제
    public void deletePermission(Long permissionId) {
        adminPermissionRepository.deleteById(permissionId);
    }
}
