package com.wb.between.menu.controller;

import com.wb.between.menu.dto.MenuListResponseDto;
import com.wb.between.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Deprecated
@Slf4j
@Controller
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/common/menu")
    public String menuList(Model model) {
        log.debug("controller {}", model);
        List<MenuListResponseDto> menuList = menuService.findMenuList();
        model.addAttribute("menuList", menuList);
        return "common/menu";
    }
}
