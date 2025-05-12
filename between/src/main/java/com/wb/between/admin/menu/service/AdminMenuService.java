package com.wb.between.admin.menu.service;

import com.wb.between.admin.menu.dto.*;
import com.wb.between.admin.menu.repository.AdminMenuRepository;
import com.wb.between.admin.role.domain.Role;
import com.wb.between.admin.role.dto.AdminRoleResDto;
import com.wb.between.admin.role.repository.AdminRoleRepository;
import com.wb.between.common.exception.CustomException;
import com.wb.between.common.exception.ErrorCode;
import com.wb.between.menu.domain.Menu;
import com.wb.between.menu.service.MenuCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMenuService {

    //메뉴
    private final AdminMenuRepository adminMenuRepository;

    //역할
    private final AdminRoleRepository adminRoleRepository;

    //메뉴 캐시 서비스
    private final MenuCacheService menuCacheService;

    /**
     * 관리자 > 메뉴관리 - 최상단 메뉴조회
     * 최상단 메뉴 타입 구분
     * @return
     */
    @Transactional(readOnly = true)
    public List<JsTreeNodeDto> getMenuRootNode(){

        //메뉴 타입 조회
        List<String> menuTypeList = adminMenuRepository.findDistinctByMenuType();

        log.debug("menuTypeList: {}", menuTypeList);

        //결과 없을 경우
        if(menuTypeList.isEmpty()) {
            throw new CustomException(ErrorCode.MENU_NOT_FOUND);
        }

        List<JsTreeNodeDto> virtualNodes = new ArrayList<>();

        String nodeId = null;

        for (String type : menuTypeList) {

            JsTreeNodeDto node = JsTreeNodeDto.builder()
                    // 1. 고유 ID 생성 (가상 노드 식별용)
                    .id(type)
                    // 2. 부모는 최상위 루트 '#'
                    .parent("#")
                    // 3. 화면에 표시될 텍스트 (타입 코드 -> 사용자 친화적 이름 변환)
                    .text(getMenuTypeText(type))
                    // 4. 하위 노드를 로드할 수 있음을 JSTree에 알림 (Lazy loading 트리거)
                    .children(true)
                    // 5. 추가 데이터 저장 (예: 원본 타입 코드 - 하위 노드 로드 시 사용)
                    .data(Map.of("type", type))
                    .build();

            virtualNodes.add(node);
        }

        return virtualNodes;
    }

    @Transactional(readOnly = true)
    public List<JsTreeNodeDto> getMenuRootNode(String nodeId){

        List<Menu> menuTypeList = adminMenuRepository.findDistinctByMenuType(nodeId);

        log.debug("menuTypeList|String nodeId {}", menuTypeList);

        //결과 없을 경우
        if(menuTypeList.isEmpty()) {
            throw new CustomException(ErrorCode.MENU_NOT_FOUND);
        }

        List<JsTreeNodeDto> virtualNodes = new ArrayList<>();

        for (Menu menu : menuTypeList) {
            JsTreeNodeDto node = JsTreeNodeDto.builder()
                    // 1. 고유 ID 생성 (가상 노드 식별용)
                    .id(menu.getMenuNo().toString())
                    // 2. 부모는 최상위 루트 '#'
                    .parent(nodeId)
                    // 3. 화면에 표시될 텍스트 (타입 코드 -> 사용자 친화적 이름 변환)
                    .text(menu.getMenuNm())
                    // 4. 하위 노드를 로드할 수 있음을 JSTree에 알림 (Lazy loading 트리거)
                    .children(false)
                    // 5. 추가 데이터 저장 (예: 원본 타입 코드 - 하위 노드 로드 시 사용)
//                    .data(Map.of("type", type))
                    .build();

            virtualNodes.add(node);
        }
        log.debug("menuTypeList|String id| virtualNodes {}", virtualNodes);
        return virtualNodes;
    }


    /**
     * 메뉴 타입 코드를 사용자 친화적인 표시 이름으로 변환하는 헬퍼 메소드.
     * @param type 메뉴 타입 코드 (예: "ADMIN")
     * @return 표시 이름 (예: "관리자 사이드바 메뉴")
     */
    private String getMenuTypeText(String type) {
        switch (type) {
            case "USER": return "사용자 헤더 메뉴";
            case "ADMIN": return "관리자 사이드바 메뉴";
            case "MYPAGE": return "마이페이지 사이드바 메뉴";
            // 새로운 타입 추가 시 여기에 case 추가
            default: return type; // 매핑 정보 없으면 코드 그대로 반환
        }
    }

    /**
     * 메뉴 상세 정보 조회
     */
    @Transactional(readOnly = true)
    public MenuDetailResDto getMenuDetail(Long menuNo) {

        //1. 선택 메뉴 조회
        Menu menu = adminMenuRepository.findByMenuNo(menuNo).orElseThrow(()-> new CustomException(ErrorCode.INVALID_INPUT));

        //2. 전체 역할 조회
        List<Role> allRoles = adminRoleRepository.findAll();

        //3. 현재 메뉴에 할당된 역할 Id조회
        Set<Long> assignRoleIds = menu.getMenuRoles().stream()
                .map(menuRole -> menuRole.getRole().getRoleId())
                .collect(Collectors.toSet());

        //4. dto처리
        AdminMenuResDto adminMenuResDto = AdminMenuResDto.from(menu);
        List<AdminRoleResDto> allRoleDto = allRoles.stream()
                .map(AdminRoleResDto::from)
                .toList();

        MenuDetailResDto menuDetailResDto = MenuDetailResDto.builder()
                .adminMenuResDto(adminMenuResDto)
                .allRoleDto(allRoleDto)
                .assignedRoleIds(assignRoleIds)
                .build();

        return menuDetailResDto;
    }

    /**
     * 관리자 > 메뉴 등록
     */
    @Transactional
    public void registMenu(AdminMenuRegistReqDto adminMenuRegistReqDto) {
        Menu menu = Menu.builder()
                .menuUrl(adminMenuRegistReqDto.getMenuUrl())
                .menuType(adminMenuRegistReqDto.getMenuType())
                .upperMenuNo(adminMenuRegistReqDto.getUpperMenuNo())
                .useAt(adminMenuRegistReqDto.getUseAt())
                .menuNm(adminMenuRegistReqDto.getMenuNm())
                .menuDsc(adminMenuRegistReqDto.getMenuDsc())
                .createDt(LocalDateTime.now())
                .sortOrder(adminMenuRegistReqDto.getSortOrder())
                .build();

        //새 메뉴 등록
        adminMenuRepository.save(menu);

        //메뉴 캐시 갱신
        menuCacheService.refreshHeaderMenus();
    }

    /**
     * 관리자 > 메뉴 수정
     * @param menuNo
     * @param adminMenuEditReqDto
     */
    @Transactional
    public void editMenu(Long menuNo, AdminMenuEditReqDto adminMenuEditReqDto) {
        Menu menu = adminMenuRepository.findById(menuNo).orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));
     
        menu.setMenuNm(adminMenuEditReqDto.getMenuNm());
        menu.setMenuDsc(adminMenuEditReqDto.getMenuDsc());
        menu.setSortOrder(adminMenuEditReqDto.getSortOrder());
        menu.setUseAt(adminMenuEditReqDto.getUseAt());
        menu.setUpperMenuNo(adminMenuEditReqDto.getUpperMenuNo());
        menu.setMenuType(adminMenuEditReqDto.getMenuType());
        menu.setMenuUrl(adminMenuEditReqDto.getMenuUrl());

        //메뉴 캐시 갱신
        menuCacheService.refreshHeaderMenus();
        
    }

    /**
     * 메뉴 삭제
     * @param menuNo
     */
    @Transactional
    public void deleteMenu(Long menuNo) {
        adminMenuRepository.deleteById(menuNo);

        //메뉴 캐시 갱신
        menuCacheService.refreshHeaderMenus();
    }
}
