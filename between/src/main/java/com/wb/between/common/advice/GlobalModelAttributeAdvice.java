package com.wb.between.common.advice;

import com.wb.between.menu.dto.MenuListResponseDto;
import com.wb.between.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributeAdvice {

    private final MenuService menuService;

    @ModelAttribute("menuList")
    public List<MenuListResponseDto> setCommonMenu() {
        return menuService.findMenuList();
    }
}
