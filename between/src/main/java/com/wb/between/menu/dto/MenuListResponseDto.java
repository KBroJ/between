package com.wb.between.menu.dto;


import com.wb.between.menu.domain.Menu;
import lombok.Builder;
import lombok.Getter;

import java.math.BigInteger;
import java.util.List;

@Getter
@Builder
public class MenuListResponseDto {

    private Long menuNo;

    private Long upperMenuNo;

    private String menuNm;

    private String menuDsc;

    private String menuUrl;

    private String useAt;

    private int sortOrder;

    private List<String> roleName;

    public static MenuListResponseDto from(Menu menu) {
        return MenuListResponseDto.builder()
                .menuNo(menu.getMenuNo())
                .upperMenuNo(menu.getUpperMenuNo())
                .menuNm(menu.getMenuNm())
                .menuDsc(menu.getMenuDsc())
                .menuUrl(menu.getMenuUrl())
                .useAt(menu.getUseAt())
                .sortOrder(menu.getSortOrder())
                .build();
    }


}
