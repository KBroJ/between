package com.wb.between.admin.menu.dto;


import com.wb.between.menu.domain.Menu;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class AdminMenuRegistReqDto {

    private Long menuNo;

    private Long upperMenuNo;

    private String menuNm;

    private String menuDsc;

    private String menuUrl;

    private String useAt;

    private int sortOrder;

    private String menuType;

    private List<String> roleName;

    public static AdminMenuRegistReqDto from(Menu menu) {
        return AdminMenuRegistReqDto.builder()
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
