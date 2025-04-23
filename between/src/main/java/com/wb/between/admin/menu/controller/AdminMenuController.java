package com.wb.between.admin.menu.controller;

import com.wb.between.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/menus")
@RequiredArgsConstructor
public class AdminMenuController {

    /**
     * 관리자 > 메뉴관리
     * @param user
     * @param model
     * @return
     */
    @GetMapping
    public String getMenuManagementView(@AuthenticationPrincipal User user, Model model){
        return "admin/menu/menu-manage";
    }

}
