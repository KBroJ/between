package com.wb.between.common.advice;

import com.wb.between.common.cache.MenuCache;
import com.wb.between.menu.dto.MenuListResponseDto;
import com.wb.between.menu.service.MenuService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalModelAttributeAdvice {

    //메뉴서비스
    private final MenuService menuService;

    /**
     * 역할별 메뉴 조회
     */
    @ModelAttribute("headerMenus")
    public List<MenuListResponseDto> getHeaderMenu(Authentication authentication) {
        return menuService.getHeaderMenuByRole(authentication);
    }

}
