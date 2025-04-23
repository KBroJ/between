package com.wb.between.admin.menu.controller;

import com.wb.between.admin.menu.dto.AdminMenuResponseDto;
import com.wb.between.admin.menu.dto.JsTreeNodeDto;
import com.wb.between.admin.menu.service.AdminMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/menus")
@RequiredArgsConstructor
public class AdminMenuRestController {

    private final AdminMenuService adminMenuService;

    /**
     * 관리자 > 메뉴관리
     * 트리구조 최상단노드
     */
    @GetMapping("/root")
    public List<JsTreeNodeDto> getMenuRootNode(
            @RequestParam(value = "id", defaultValue = "#") String id){
        log.debug("getMenuRootNode|id: {}", id);
//        List<Map<String, Object>> virtualNodes = adminMenuService.getMenuRootNode();
//        log.debug("Menu Root Node: {}", virtualNodes);
        if("#".equals(id)) {
            return adminMenuService.getMenuRootNode();
        } else {
            return adminMenuService.getMenuRootNode(id);
        }
    }

    /**
     * 관리자 > 메뉴관리
     * 트리구조 자식노드
     */
    @GetMapping("/details/{menuId}")
    public AdminMenuResponseDto getMenuDetail(@PathVariable Long menuId){
        log.debug("getMenuDetail|menuId: {}", menuId);
        AdminMenuResponseDto adminMenuResponseDto =  adminMenuService.getMenuDetail(menuId);
        return adminMenuResponseDto;
    }
}
