package com.wb.between.menu.service;

import com.wb.between.admin.role.domain.Role;
import com.wb.between.menu.domain.Menu;
import com.wb.between.menu.dto.MenuListResponseDto;
import com.wb.between.menu.repository.MenuRepository;
import com.wb.between.menurole.domain.MenuRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;

    private final MenuCacheService menuCacheService;

    /******************************
     * 캐시 메뉴 조회 처리
     ******************************/

    /**
     * 메뉴 목록 역할별 필터링
     */
    public List<MenuListResponseDto> getHeaderMenuByRole(Authentication authentication) {
       
        //역할 셋
        Set<String> roles;
        
        //인증여부에 따른 역할 처리
        
        //비회원
        if(authentication == null || !authentication.isAuthenticated()) {
            roles = Set.of("ROLE_ANONYMOUS");
        } else {
            //회원
            roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());
        }

        log.debug("getHeaderMenuByRole|roles => {}", roles);

        //전체목록 조회, 캐시설정한 메소드
        List<Menu> allHeaderMenu = menuCacheService.getAllHeaderMenu();
        
        //메뉴 역할 필터 후 리턴
        return allHeaderMenu.stream()
                .filter(menu -> {

                    //1. 전체공개일 경우 모든 사용자 표출
                    if(menu.isPublic()) {
                        return true;
                    }

                    Set<String> menuAllowedRoles = getMenuAllowedRoleNames(menu);
                    // menuAllowedRoles 가 비어있으면 공개
                    if (menuAllowedRoles.isEmpty()) {
                        return roles.contains("ROLE_ANONYMOUS");
                    }
                    return roles.stream().anyMatch(menuAllowedRoles::contains);
                })
                .map(MenuListResponseDto::from).toList();
    }

    /**
     * Menu 객체에서 허용된 역할 이름(String) Set을 안전하게 추출합니다.
     * (Menu 엔티티가 MenuRole Set을 가지고 있고, MenuRole이 Role을 참조한다고 가정)
     */
    private Set<String> getMenuAllowedRoleNames(Menu menu) {
        if (menu == null || menu.getMenuRoles() == null || menu.getMenuRoles().isEmpty()) {
            return Collections.emptySet();
        }
        return menu.getMenuRoles().stream()
                .map(MenuRole::getRole)      // MenuRole -> Role 추출
                .filter(Objects::nonNull)    // null Role 객체 필터링
                .map(Role::getRoleCode)      // Role -> 역할 이름(String) 추출
                .filter(Objects::nonNull)    // null 역할 이름 필터링
                .collect(Collectors.toSet());
    }

    /**
     * 관리자 사이드바
     * @param authentication
     * @return
     */
    public List<MenuListResponseDto> getAdminSideMenu(Authentication authentication) {
        if(isAdmin(authentication)) {
            log.debug("===> {}", menuCacheService.getAdminSideMenu());
            return menuCacheService.getAdminSideMenu().stream().map(MenuListResponseDto::from).toList();
        }
        return Collections.emptyList();
    }

    /**
     * 사용자가 관리자 역할을 가지고 있는지 확인하는 헬퍼 메서드
     * @param authentication 현재 인증 정보
     * @return 관리자이면 true, 아니면 false
     */
    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_ADMIN".equals(role)); 
    }
}
