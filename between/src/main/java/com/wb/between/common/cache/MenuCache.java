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

    //메뉴 관련 서비스
    private final MenuService menuService;

    //권한별 메뉴 리스트 담길 맵
    private final Map<String, List<MenuListResponseDto>> roleMenuCache = new ConcurrentHashMap<>();

    //스프링이 빈을 생성하고 의존성 주입이 끝난 직후 실행
    //초기화 코드 및 캐시 세팅에 사용
    @PostConstruct
    public void init() {
        loadMenus(); // 서버 시작 시 최초 캐싱
    }


    public void reload() {
        roleMenuCache.clear();
        loadMenus(); // 서버 시작 시 최초 캐싱
    }

    //메뉴 호출 메소드
    public void loadMenus() {
        // 1.전체 목록 조회
        List<MenuListResponseDto> menuListResponseDtoList = menuService.findByUseAt();

        // 2. 메뉴별 역할 정보 조회 (추후)

        // 3. 역할 목록 정의 (DB에서 가져오거나 하드코딩)
        List<String> allRoles = List.of("일반", "관리자", "user", "ROLE_ADMIN", "ROLE_USER", "ROLE_MANAGER", "ROLE_ANONYMOUS");


        for(String role : allRoles) {
            List<MenuListResponseDto> menusForRole = menuListResponseDtoList.stream()
                    .filter(menu -> {
                        if("user".equals(role) || "일반".equals(role)) {
                            return !menu.getMenuNm().equals("로그인") &&
                                    !menu.getMenuNm().equals("회원가입");
                            //TODO: 유저권한 정리될 때까지 상관없이 메뉴 표출
//                                    !menu.getMenuNm().equals("관리");
                        } else if("admin".equals(role) || "관리자".equals(role)) {
                            return !menu.getMenuNm().equals("로그인") &&
                                    !menu.getMenuNm().equals("회원가입");
                        } else {
                            return !menu.getMenuNm().equals("마이페이지") &&
                                    !menu.getMenuNm().equals("로그아웃");
                        }
                    })
                    .toList();
            roleMenuCache.put(role, menusForRole);
        }

    }

    // 특정 역할 메뉴 갱신
    public void refreshRoleMenu(String role) {
        loadMenus();
    }

    public List<MenuListResponseDto> getMenusByRole(String role) {
        return roleMenuCache.getOrDefault(role, Collections.emptyList());
    }
}
