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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalModelAttributeAdvice {

    private final MenuCache menuCache;

    @ModelAttribute("menuList")
    public List<MenuListResponseDto> setCommonMenu(Authentication authentication, HttpSession session) {

        List<String> roles;

        if (authentication == null || !authentication.isAuthenticated()) {
            roles = List.of("ROLE_ANONYMOUS");
        } else {
            roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
        }

        // 권한명으로 세션 키를 구분
        String sessionKey = "menuList_" + String.join("_", roles);

        // 세션 캐싱을 추가하면 성능을 더 향상시킬 수 있음
        List<MenuListResponseDto> cachedMenu = (List<MenuListResponseDto>) session.getAttribute("menuList");

        if (cachedMenu != null) {
            return cachedMenu;
        }

        try {
            List<MenuListResponseDto> menus = roles.stream()
                    .flatMap(role -> menuCache.getMenusByRole(role).stream())
                    .distinct()
                    .sorted(Comparator.comparingInt(MenuListResponseDto::getSortOrder))
                    .collect(Collectors.toList());

            session.setAttribute(sessionKey, menus);
            return menus;

        } catch (Exception e) {
            log.error("메뉴 조회 실패", e);
            return Collections.emptyList();
        }
    }
}
