package com.wb.between.common.cache;

import com.wb.between.menu.domain.Menu;
import com.wb.between.menu.dto.MenuListResponseDto;
import com.wb.between.menu.service.MenuService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class MenuCache {

    private final MenuService menuService;

    private final Map<String, List<MenuListResponseDto>> roleMenuCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadMenus(); // 서버 시작 시 최초 캐싱
    }

    public void loadMenus() {
        roleMenuCache.put("ROLE_ADMIN", menuService.findByRole("ROLE_ADMIN"));
        roleMenuCache.put("ROLE_USER", menuService.findByRole("ROLE_USER"));
        roleMenuCache.put("ROLE_MANAGER", menuService.findByRole("ROLE_MANAGER"));
        roleMenuCache.put("ROLE_ANONYMOUS", menuService.findByRole("ROLE_ANONYMOUS")); // 익명 메뉴 추가
    }

    // 특정 역할 메뉴 갱신
    public void refreshRoleMenu(String role) {
        roleMenuCache.put(role, menuService.findByRole(role));
    }

    public List<MenuListResponseDto> getMenusByRole(String role) {
        return roleMenuCache.getOrDefault(role, Collections.emptyList());
    }
}
