package com.wb.between.admin.popup.controller;

import com.wb.between.admin.popup.dto.AdminPopupEditReqDto;
import com.wb.between.admin.popup.dto.AdminPopupRegReqDto;
import com.wb.between.admin.popup.dto.AdminPopupResDto;
import com.wb.between.admin.popup.service.AdminPopupService;
import com.wb.between.common.exception.CustomException;
import com.wb.between.common.util.pagination.PaginationInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/admin/popup")
@RequiredArgsConstructor
public class AdminPopupController {

    private final AdminPopupService adminPopupService;

    /**
     * 관리자 > 팝업 관리
     */
    @GetMapping
    public String getAdminPopupPageView(
            @RequestParam(required = false, defaultValue = "") String searchPopupName,
            @PageableDefault(size = 10) Pageable pageable,
            Model model) {

        Page<AdminPopupResDto> popupList = adminPopupService.findPopupList(pageable, searchPopupName);
        model.addAttribute("popupList", popupList);

        // --- PaginationInfo를 위한 추가 파라미터 구성 ---
        Map<String, Object> additionalParams = new HashMap<>();
        if (searchPopupName != null && !searchPopupName.isEmpty()) {
            additionalParams.put("searchPopupName", searchPopupName);
        }

        int pageDisplayWindow = 5; // 예: 한 번에 5개의 페이지 번호를 보여줌
        PaginationInfo paginationInfo =
                new PaginationInfo(popupList, "/admin/popup", additionalParams, pageDisplayWindow);

        model.addAttribute("paginationInfo", paginationInfo);

        return "admin/popup/popup-manage";
    }

    /**
     * 관리자 > 팝업 관리 > 팝업 등록
     */
    @GetMapping("/regist")
    public String getAdminPopupRegistPageView(Model model) {
        model.addAttribute("popupInfo", new AdminPopupRegReqDto());
        return "admin/popup/popup-regist";
    }

    /**
     * 관리자 > 팝업관리 > 팝업등록
     */
    @PostMapping("/regist")
    public String registPopup(@Valid @ModelAttribute("popupInfo") AdminPopupRegReqDto adminPopupRegReqDto,
                              BindingResult bindingResult) {
        
        if (bindingResult.hasErrors()) {
            List<FieldError> fieldErrors = bindingResult.getFieldErrors();
            for (FieldError error : fieldErrors) {
                log.debug("field => {}, message => {}", error.getField(), error.getDefaultMessage());
            }
            return "admin/popup/popup-regist";
        }

        try {
            log.debug("regist => {}", adminPopupRegReqDto.getContentBody());

            adminPopupService.registPopup(adminPopupRegReqDto);
            return "redirect:/admin/popup";
        } catch (CustomException ex) {
            log.error("registPopup error => {}", ex.getMessage());
            return "admin/popup/popup-regist";
        }


    }

    /**
     * 관리자 > 팝업 관리 > 팝업수정
     */
    @GetMapping("/edit/{popupId}")
    public String getAdminPopupEditPageView(@PathVariable("popupId") Long popupId, Model model) {

        AdminPopupResDto popupInfo = adminPopupService.findPopup(popupId);

        log.debug("popupcontroller => {}", popupInfo.getStartDt());
        model.addAttribute("popupInfo", popupInfo);

        return "admin/popup/popup-edit";
    }

    /**
     * 관리자 > 팝업 관리 > 팝업수정
     */
    @PutMapping("/edit/{popupId}")
    public String editPopup(@PathVariable("popupId") Long popupId,
                            @Valid @ModelAttribute("popupInfo") AdminPopupEditReqDto adminPopupEditReqDto,
                            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "admin/popup/popup-edit";
        }

        try {
            adminPopupService.editPopup(popupId, adminPopupEditReqDto);
        } catch (CustomException ex) {
            log.error("editPopup error => {}", ex.getMessage());
        }

        return "admin/popup/popup-edit";
    }

}
