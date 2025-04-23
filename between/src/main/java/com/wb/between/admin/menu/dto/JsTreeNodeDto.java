package com.wb.between.admin.menu.dto;


import com.wb.between.menu.domain.Menu;
import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
@Builder
public class JsTreeNodeDto {

    private String id;

    private String parent;

    private String text;

    private String icon;

    private boolean children;

    private Map<String, Object> data;


    public static JsTreeNodeDto from(Menu menu, String nodeId) {

        // 추가 데이터를 담을 Map 생성
        Map<String, Object> nodeData = new HashMap<>();
        nodeData.put("url", Optional.ofNullable(menu.getMenuUrl()).orElse(""));
        nodeData.put("description", Optional.ofNullable(menu.getMenuDsc()).orElse(""));
        nodeData.put("type", menu.getMenuType());

        return JsTreeNodeDto.builder()
                .id(String.valueOf(menu.getMenuNo())) // menuNo를 String으로 변환
                .parent(nodeId) // JSTree 요청 context 상의 부모 ID
                .text(menu.getMenuNm())
                .data(nodeData) // 위에서 만든 추가 데이터 Map 설정
                .children(false) // 자식 존재 여부 확인 로직 (선택적, 성능 고려)
                .build();
    }


}
