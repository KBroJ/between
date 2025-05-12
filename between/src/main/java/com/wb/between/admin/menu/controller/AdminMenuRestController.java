package com.wb.between.admin.menu.controller;

import com.wb.between.admin.menu.dto.*;
import com.wb.between.admin.menu.service.AdminMenuService;
import com.wb.between.common.exception.CustomException;
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
            @RequestParam(value = "nodeId", defaultValue = "#") String nodeId){
        log.debug("getMenuRootNode|id: {}", nodeId);
//        List<Map<String, Object>> virtualNodes = adminMenuService.getMenuRootNode();
//        log.debug("Menu Root Node: {}", virtualNodes);
        if("#".equals(nodeId)) {
            return adminMenuService.getMenuRootNode();
        } else {
            return adminMenuService.getMenuRootNode(nodeId);
        }
    }

    /**
     * 관리자 > 메뉴관리
     * 트리구조 자식노드
     */
    @GetMapping("/details/{menuNo}")
    public MenuDetailResDto getMenuDetail(@PathVariable Long menuNo){
        log.debug("getMenuDetail|menuNo: {}", menuNo);
        MenuDetailResDto menuDetailResDto =  adminMenuService.getMenuDetail(menuNo);
        log.debug("getMenuDetail|menuDetailResDto: {}", menuDetailResDto);
        return menuDetailResDto;
    }

    /**
     * 관리자 > 메뉴 등록
     * @param adminMenuRegistReqDto
     */
    @PostMapping("/regist")
    public void registMenu(@RequestBody AdminMenuRegistReqDto adminMenuRegistReqDto){
        try {
            adminMenuService.registMenu(adminMenuRegistReqDto);
        } catch (CustomException ex) {
            log.error("예상치 못한 오류 발생", ex);
        } catch (RuntimeException e) {
            // 예상치 못한 다른 종류의 예외 처리
            log.error("예상치 못한 오류 발생", e);
        }
    }

    /**
     * 메뉴 수정
     * @param menuNo
     * @param adminMenuEditReqDto
     */
    @PutMapping("/edit/{menuNo}")
    public void editMenu(@PathVariable Long menuNo, @RequestBody AdminMenuEditReqDto adminMenuEditReqDto) {
        try {

            adminMenuService.editMenu(menuNo, adminMenuEditReqDto);
        } catch (CustomException ex) {
            log.error("예상치 못한 오류 발생", ex);
        } catch (RuntimeException e) {
            // 예상치 못한 다른 종류의 예외 처리
            log.error("예상치 못한 오류 발생", e);
        }
    }

    @DeleteMapping("/delete/{menuNo}")
    public void deleteMenu(@PathVariable Long menuNo) {
        try {
            adminMenuService.deleteMenu(menuNo);
        } catch (CustomException ex) {
            log.error(ex.getMessage(), ex);
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
        }
    }
}
