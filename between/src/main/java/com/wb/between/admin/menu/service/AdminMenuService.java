package com.wb.between.admin.menu.service;

import com.wb.between.admin.menu.dto.AdminMenuResponseDto;
import com.wb.between.admin.menu.dto.JsTreeNodeDto;
import com.wb.between.admin.menu.repository.AdminMenuRepository;
import com.wb.between.common.exception.CustomException;
import com.wb.between.common.exception.ErrorCode;
import com.wb.between.menu.domain.Menu;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMenuService {

    private final AdminMenuRepository adminMenuRepository;

    @Transactional(readOnly = true)
    public List<JsTreeNodeDto> getMenuRootNode(){

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
    public List<JsTreeNodeDto> getMenuRootNode(String id){

        List<Menu> menuTypeList = adminMenuRepository.findDistinctByMenuType(id);

        log.debug("menuTypeList|String id {}", menuTypeList);

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
                    .parent(id)
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
     *
     * @param menuId
     */
    public AdminMenuResponseDto getMenuDetail(Long menuId) {
        Menu menu = adminMenuRepository.findById(menuId).orElseThrow(()-> new CustomException(ErrorCode.INVALID_INPUT));

        return AdminMenuResponseDto.from(menu);
    }
}
