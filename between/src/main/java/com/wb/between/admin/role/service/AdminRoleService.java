package com.wb.between.admin.role.service;

import com.wb.between.admin.permission.domain.Permission;
import com.wb.between.admin.permission.dto.AdminPermissionResDto;
import com.wb.between.admin.permission.repository.AdminPermissionRepository;
import com.wb.between.admin.role.domain.Role;
import com.wb.between.admin.role.dto.AdminRoleEditReqDto;
import com.wb.between.admin.role.dto.AdminRoleRegistReqDto;
import com.wb.between.admin.role.dto.AdminRoleResDto;
import com.wb.between.admin.role.repository.AdminRoleRepository;
import com.wb.between.admin.rolepermission.domain.RolePermission;
import com.wb.between.common.exception.CustomException;
import com.wb.between.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminRoleService {

    //역할
    private final AdminRoleRepository adminRoleRepository;

    //권한
    private final AdminPermissionRepository adminPermissionRepository;

    /**
     * 관리자 > 역할 조회
     * @param pageable
     * @param searchRoleName
     * @return
     */
    @Transactional(readOnly = true)
    public Page<AdminRoleResDto> findAdminRoleList(Pageable pageable, String searchRoleName) {
        Page<Role> adminRoleList = adminRoleRepository.findRoleWithFilter(pageable, searchRoleName);

        return adminRoleList.map(AdminRoleResDto::from);
    }

    /**
     * 권한 조회
     * @return
     */
    @Transactional(readOnly = true)
    public List<AdminPermissionResDto> findAdminPermissionList(){
        List<Permission> permissionList = adminPermissionRepository.findAll();
        return permissionList.stream().map(AdminPermissionResDto::from).toList();
    }

    /**
     * 
     * 역할 신규 등록
     */
    @Transactional
    public void roleRegist(AdminRoleRegistReqDto adminRoleRegistReqDto) {
        //신규 역할 객체 생성
        Role role = Role.builder()
                .roleCode(adminRoleRegistReqDto.getRoleCode())
                .roleName(adminRoleRegistReqDto.getRoleName())
                .description(adminRoleRegistReqDto.getDescription())
                .createDt(LocalDateTime.now())
                .build();

        //권한 있는 경우 권한 등록
        if(adminRoleRegistReqDto.getPermissionIds() != null && !adminRoleRegistReqDto.getPermissionIds().isEmpty()) {
            for(Long permissionId : adminRoleRegistReqDto.getPermissionIds()) {
                log.debug("permissionId => {}", permissionId);
                Permission permission = adminPermissionRepository.findById(permissionId)
                        .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_ERROR));

                //역할-권한 객체 생성
                RolePermission rolePermission = RolePermission.builder()
                        .role(role)
                        .permission(permission)
                        .createDt(LocalDateTime.now())
                        .build();

                //역할-권한 세팅
                role.getRolePermissions().add(rolePermission);

            }
        }

        //역할 등록
        adminRoleRepository.save(role);
    }

    /**
     * 역할 단일 조회
     * @param roleId
     * @return
     */
    @Transactional(readOnly = true)
    public AdminRoleResDto findRoleById(Long roleId) {
        //역할 조회
        Role role = adminRoleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));
        return AdminRoleResDto.from(role);
    }

    /**
     * 역할 수정
     */
    @Transactional
    public void editRole(Long roleId, AdminRoleEditReqDto adminRoleEditReqDto) {

        //1. 역할 조회
        Role role = adminRoleRepository.findById(roleId).orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));

        //2. 역할 기본 정보 수정
        role.setRoleCode(adminRoleEditReqDto.getRoleCode());
        role.setRoleName(adminRoleEditReqDto.getRoleName());
        role.setDescription(adminRoleEditReqDto.getDescription());
        
        //3. 기존권한 정보 처리
        role.getRolePermissions().clear();
        
        //4. 새 권한 정보 추가
        if(adminRoleEditReqDto.getPermissionIds() != null && !adminRoleEditReqDto.getPermissionIds().isEmpty()) {
            for(Long permissionId : adminRoleEditReqDto.getPermissionIds()) {
                //권한 조회
                Permission permission = adminPermissionRepository.findById(permissionId)
                        .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_ERROR));

                //역할-권한 객체 생성
                RolePermission rolePermission = RolePermission.builder()
                        .role(role)
                        .permission(permission)
                        .createDt(LocalDateTime.now())
                        .build();

                //역할-권한 세팅
                role.getRolePermissions().add(rolePermission);
            }
        }

        //수정
        adminRoleRepository.save(role);
    }

    /**
     * 역할 삭제
     */
    @Transactional
    public void deleteRole(Long roleId) {
        adminRoleRepository.deleteById(roleId);
    }



}
