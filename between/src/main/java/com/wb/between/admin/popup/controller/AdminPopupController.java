package com.wb.between.admin.popup.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/popup")
public class AdminPopupController {

    /**
     * 관리자 > 팝업 관리
     * @return
     */
    @GetMapping
    public String getAdminPopupPageView() {
        return "admin/popup/popup-manage";
    }

    /**
     * 관리자 > 팝업 관리 > 팝업 등록
     * @return
     */
    @GetMapping("/regist")
    public String getAdminPopupRegistPageView() {
        return "admin/popup/popup-regist";
    }

    /**
     * 관리자 > 팝업 관리 > 팝업 수정
     * @param popupId
     * @return
     */
    @GetMapping("/edit/{popupId}")
    public String getAdminPopupEditPageView(Long popupId) {
        return "admin/popup/popup-edit";
    }

}
