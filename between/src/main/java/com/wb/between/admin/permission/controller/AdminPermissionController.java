package com.wb.between.admin.permission.controller;

import com.wb.between.admin.permission.dto.AdminPermissionEditReqDto;
import com.wb.between.admin.permission.dto.AdminPermissionRegReqDto;
import com.wb.between.admin.permission.dto.AdminPermissionResDto;
import com.wb.between.admin.permission.service.AdminPermissionService;
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
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/permissions")
@RequiredArgsConstructor
@Slf4j
public class AdminPermissionController {

    private final AdminPermissionService adminPermissionService;

    /**
     * 관리자 > 권한그룹
     * @param user
     * @param model
     * @return
     */
    @GetMapping
    public String getPermissionManagementView(@AuthenticationPrincipal User user,
                                              @RequestParam(required = false, defaultValue = "") String searchPermissionName,
                                              @RequestParam(defaultValue = "0") int page,
                                              Model model){

        Pageable pageable = PageRequest.of(page, 10); // 예: 페이지당 10개

        Page<AdminPermissionResDto> adminPermissionList = adminPermissionService.findAdminPermissionList(pageable, searchPermissionName);

        model.addAttribute("adminPermissionList", adminPermissionList);

        return "admin/permission/permission-manage";
    }

    /**
     * 관리자 > 권한등록
     * @param model
     * @return
     */
    @GetMapping("/regist")
    public String getPermissionRegistView(Model model){

        //초기 요청 객체
        model.addAttribute("permissionInfo", new AdminPermissionRegReqDto());

        return "admin/permission/permission-regist";
    }

    @PostMapping("/regist")
    public String permissionRegist(@Valid @ModelAttribute("permissionInfo")
                                       AdminPermissionRegReqDto adminPermissionRegReqDto,
                                   BindingResult bindingResult, Model model) {
        if(bindingResult.hasErrors()){
            return "admin/permission/permission-regist";
        }

        try {
            //등록
            adminPermissionService.permissionRegist(adminPermissionRegReqDto);

            return "redirect:/admin/permissions";
        } catch (CustomException ex) {
            log.error("AdminPermissionController|Post|permissionRegist|error => {}", ex.getMessage());
            return "admin/permission/permission-regist";
        }

    }

    /**
     * 관리자 > 권한수정
     * @param permissionId
     * @param model
     * @return
     */
    @GetMapping("/edit/{permissionId}")
    public String getPermissionEditView(@PathVariable Long permissionId, Model model){

        //수정 대상 조회
        AdminPermissionResDto adminPermissionResDto = adminPermissionService.findAdminPermissionById(permissionId);

        //수정 대상 모델
        model.addAttribute("permissionInfo", adminPermissionResDto);

        return "admin/permission/permission-edit";
    }

    /**
     * 수정
     * @param permissionId
     * @param adminPermissionEditReqDto
     * @param bindingResult
     * @param model
     * @return
     */
    @PutMapping("/edit/{permissionId}")
    public String editPermission(@PathVariable("permissionId") Long permissionId,
                                 @Valid @ModelAttribute("permissionInfo")AdminPermissionEditReqDto adminPermissionEditReqDto,
                                 BindingResult bindingResult, Model model) {
       if(bindingResult.hasErrors()){
           return "admin/permission/permission-edit";
       }
       
       try {
           adminPermissionService.permissionEdit(permissionId, adminPermissionEditReqDto);
           return "redirect:/admin/permissions";
       } catch (CustomException ex) {
           log.error("AdminPermissionController|Post|permissionRegist|error => {}", ex.getMessage());
           return "admin/permission/permission-regist";
       }
      
    }
}
