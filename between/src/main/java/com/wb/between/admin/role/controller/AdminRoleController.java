package com.wb.between.admin.role.controller;

import com.wb.between.admin.permission.dto.AdminPermissionResDto;
import com.wb.between.admin.role.dto.AdminRoleEditReqDto;
import com.wb.between.admin.role.dto.AdminRoleRegistReqDto;
import com.wb.between.admin.role.dto.AdminRoleResDto;
import com.wb.between.admin.role.service.AdminRoleService;
import com.wb.between.common.exception.CustomException;
import com.wb.between.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    /**
     * 관리자 > 역할 관리
     */
    @GetMapping
    public String getRoleManagementView(@AuthenticationPrincipal User user,
                                                      @RequestParam(required = false, defaultValue = "") String searchRoleName,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      Model model){
        Pageable pageable = PageRequest.of(page, 10); // 예: 페이지당 10개

        //관리자 역할 목록 조회
        Page<AdminRoleResDto> adminRoleList = adminRoleService.findAdminRoleList(pageable, searchRoleName);

        model.addAttribute("adminRoleList", adminRoleList);
        model.addAttribute("searchRoleName", searchRoleName);

        return "admin/role/role-manage";
    }

    /**
     * 관리자 > 역할 관리 > 역할 등록 화면
     */
    @GetMapping("/regist")
    public String getRoleRegistView(Model model) {

        //권한 목록
        List<AdminPermissionResDto> adminPermissionList = adminRoleService.findAdminPermissionList();

        //초기 객체
        model.addAttribute("roleInfo", new AdminRoleRegistReqDto());
        //권한 목록
        model.addAttribute("adminPermissionList", adminPermissionList);

        return "admin/role/role-regist";

    }

    /**
     * 관리자 > 역할 관리 > 역할 등록
     */
    @PostMapping("/regist")
    public String roleRegist(@Valid @ModelAttribute("roleInfo") AdminRoleRegistReqDto adminRoleRegistReqDto,
                             BindingResult bindingResult,
                             Model model) {
        log.debug("AdminRoleController|roleRegist|adminRoleRegistReqDto {}", adminRoleRegistReqDto);

        if (bindingResult.hasErrors()) {
            // 유효성 검사 실패 시, 다시 등록 폼으로 이동 (오류 메시지 표시됨)
            List<AdminPermissionResDto> adminPermissionList = adminRoleService.findAdminPermissionList();
            model.addAttribute("adminPermissionList", adminPermissionList);
            return "admin/role/role-regist";
        }

        try {
            //등록
            adminRoleService.roleRegist(adminRoleRegistReqDto);
            //등록 후 관리 리다이렉트
            return "redirect:/admin/roles";
        } catch (CustomException ex) {
            log.error("AdminRoleController|Post|roleRegist|error => {}", ex.getMessage());
            return "admin/role/role-regist";
        } catch (RuntimeException e) {
            // 예상치 못한 다른 종류의 예외 처리
            log.error("예상치 못한 오류 발생 => {}", e.getMessage());
            return "admin/role/role-regist";
        }

    }

    /**
     * 관리자 > 역할 관리 > 역할 수정 화면
     */
    @GetMapping("/edit/{roleId}")
    public String getRoleEditView(@PathVariable("roleId") Long roleId, Model model) {

        //역할 단일 조회
        AdminRoleResDto roleInfo = adminRoleService.findRoleById(roleId);
        log.debug("getRoleEditView|roleInfo => {}", roleInfo.getPermissionIds());
        //권한 목록
        List<AdminPermissionResDto> adminPermissionList = adminRoleService.findAdminPermissionList();

           //권한 목록
        model.addAttribute("adminPermissionList", adminPermissionList);

        model.addAttribute("roleInfo", roleInfo);

        return "admin/role/role-edit";
    }

    /**
     * 관리자 > 역할 관리 > 역할 수정
     */
    @PutMapping("/edit/{roleId}")
    public String editRole(@PathVariable("roleId") Long roleId,
                           @Valid @ModelAttribute("roleInfo") AdminRoleEditReqDto adminRoleEditReqDto,
                           BindingResult bindingResult,
                           Model model) {

        if (bindingResult.hasErrors()) {
            // 유효성 검사 실패 시, 다시 등록 폼으로 이동 (오류 메시지 표시됨)
            List<FieldError> fieldErrors = bindingResult.getFieldErrors();
            for (FieldError error : fieldErrors) {
                log.debug("field => {}, message => {}", error.getField(), error.getDefaultMessage());
            }
            return "admin/role/role-edit";
        }

        try {
            //역할 수정
            adminRoleService.editRole(roleId, adminRoleEditReqDto);

            //수정 후 리다이렉트
            return "redirect:/admin/roles/edit/" + roleId;
        } catch (CustomException ex) {
            log.error("AdminRoleController|editRole|error = {}", ex.getMessage());
            return "admin/role/role-edit";
        } catch (RuntimeException e) {
            // 예상치 못한 다른 종류의 예외 처리
            log.error("예상치 못한 오류 발생 => {}", e.getMessage());
            return "admin/role/role-edit";
        }

    }

    @ResponseBody
    @DeleteMapping("/delete/{roleId}")
    public void deleteRole(@PathVariable("roleId") Long roleId) {
        log.debug("deleteRole|roleId => {}", roleId);

        try {
            adminRoleService.deleteRole(roleId);

        } catch (CustomException ex) {
            log.error("editRole|error = {}", ex.getMessage());

        } catch (RuntimeException e) {
            // 예상치 못한 다른 종류의 예외 처리
            log.error("예상치 못한 오류 발생", e);

        }

    }
}
