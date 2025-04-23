package com.wb.between.admin.role.controller;

import com.wb.between.admin.role.dto.AdminRoleResDto;
import com.wb.between.admin.role.service.AdminRoleService;
import com.wb.between.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    /**
     * 관리자 > 권한 그룹 관리
     * @param user
     * @param model
     * @return
     */
    @GetMapping
    public String getRoleManagementView(@AuthenticationPrincipal User user,
                                                      @RequestParam(required = false, defaultValue = "") String searchRole,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      Model model){
        Pageable pageable = PageRequest.of(page, 10); // 예: 페이지당 10개


        Page<AdminRoleResDto> adminRoleList = adminRoleService.findAdminRoleList(pageable, searchRole);

        model.addAttribute("adminRoleList", adminRoleList);

        return "admin/role/role-manage";
    }
}
