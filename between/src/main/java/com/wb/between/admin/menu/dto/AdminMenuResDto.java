package com.wb.between.admin.menu.dto;


import com.wb.between.menu.domain.Menu;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminMenuResDto {

    private Long menuNo;

    private Long upperMenuNo;

    private String menuNm;

    private String menuDsc;

    private String menuUrl;

    private String useAt;

    private int sortOrder;

    private String menuType;

    private List<String> roleName;

    public static AdminMenuResDto from(Menu menu) {
        return AdminMenuResDto.builder()
                .menuNo(menu.getMenuNo())
                .upperMenuNo(menu.getUpperMenuNo())
                .menuNm(menu.getMenuNm())
                .menuDsc(menu.getMenuDsc())
                .menuUrl(menu.getMenuUrl())
                .useAt(menu.getUseAt())
                .sortOrder(menu.getSortOrder())
                .menuType(menu.getMenuType())
                .build();
    }


}
