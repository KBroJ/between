package com.wb.between.menu.service;

import com.wb.between.common.exception.CustomException;
import com.wb.between.common.exception.ErrorCode;
import com.wb.between.menu.domain.Menu;
import com.wb.between.menu.dto.MenuListResponseDto;
import com.wb.between.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;

    /**
     * 메뉴목록 조회
     * @return
     */
    public List<MenuListResponseDto> findMenuList() {

        //메뉴목록 조회
        List<Menu> menuList = menuRepository.findByUseAt("Y", Sort.by(Sort.Direction.ASC, "menuNo"));
        log.debug("menuList: {}", menuList);

        //결과 없을 경우
        if(menuList.isEmpty()) {
            throw new CustomException(ErrorCode.MENU_NOT_FOUND);
        }


        return menuList.stream().map(MenuListResponseDto::from).toList();
    }


}
