package com.wb.between.admin.permission.controller;

import com.wb.between.admin.permission.dto.AdminPermissionEditReqDto;
import com.wb.between.admin.permission.dto.AdminPermissionRegReqDto;
import com.wb.between.admin.permission.dto.AdminPermissionResDto;
import com.wb.between.admin.permission.service.AdminPermissionService;
import com.wb.between.common.exception.CustomException;
import com.wb.between.common.util.pagination.PaginationInfo;
import com.wb.between.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
                                              @PageableDefault(size = 10) Pageable pageable,
                                              Model model){

        //권한 목록 조회
        Page<AdminPermissionResDto> adminPermissionList = adminPermissionService.findAdminPermissionList(pageable, searchPermissionName);

        model.addAttribute("adminPermissionList", adminPermissionList);
        model.addAttribute("searchPermissionName", searchPermissionName);

        // --- PaginationInfo를 위한 추가 파라미터 구성 ---
        Map<String, Object> additionalParams = new HashMap<>();
        if (searchPermissionName != null && !searchPermissionName.isEmpty()) {
            additionalParams.put("searchPermissionName", searchPermissionName);
        }

        int pageDisplayWindow = 5; // 예: 한 번에 5개의 페이지 번호를 보여줌
        PaginationInfo paginationInfo =
                new PaginationInfo(adminPermissionList, "/admin/permissions", additionalParams, pageDisplayWindow);

        model.addAttribute("paginationInfo", paginationInfo);

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

    /**
     * 권한 삭제
     * @param permissionId
     */
    @ResponseBody
    @DeleteMapping("/delete/{permissionId}")
    public void deletePermission(@PathVariable("permissionId") Long permissionId) {
        adminPermissionService.deletePermission(permissionId);
    }
}
