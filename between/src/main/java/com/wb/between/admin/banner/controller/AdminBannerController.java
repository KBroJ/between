package com.wb.between.admin.banner.controller;

import com.wb.between.admin.banner.dto.AdminBannerEditReqDto;
import com.wb.between.admin.banner.dto.AdminBannerRegistReqDto;
import com.wb.between.admin.banner.dto.AdminBannerResDto;
import com.wb.between.admin.banner.service.AdminBannerService;
import com.wb.between.common.exception.CustomException;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/admin/banner")
@RequiredArgsConstructor
@Slf4j
public class AdminBannerController {

    private final AdminBannerService adminBannerService;

    /**
     * 관리자 > 메인관리
     * @return
     */
    @GetMapping
    public String getBannerManagementView(@RequestParam(required = false, defaultValue = "") String searchBannerName,
                                          @PageableDefault(size = 10) Pageable pageable,
                                          Model model) {

        Page<AdminBannerResDto> bannerList = adminBannerService.findBannerList(pageable, searchBannerName);
        log.debug("getBannerManagementView: bannerList = {}", bannerList);

        model.addAttribute("bannerList", bannerList);

        return "admin/banner/banner-manage";
    }

    @GetMapping("/regist")
    public String getRegistBannerView(Model model) {

        model.addAttribute("bannerInfo", new AdminBannerRegistReqDto());
        return "admin/banner/banner-regist";
    }

    /**
     * 관리자 > 배너 관리 > 배너 등록
     */
    @PostMapping("/regist")
    public String registBanner(@Valid @ModelAttribute("bannerInfo") AdminBannerRegistReqDto adminBannerRegistReqDto,
                                   BindingResult bindingResult,
                                   Model model,
                                    RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            List<FieldError> fieldErrors = bindingResult.getFieldErrors();
            for (FieldError error : fieldErrors) {
                log.debug("field => {}, message => {}", error.getField(), error.getDefaultMessage());
            }
            return "admin/banner/banner-regist";
        }

        try {

            //등록
            adminBannerService.registMainBanner(adminBannerRegistReqDto);
            redirectAttributes.addFlashAttribute("alertMessage", "배너정보가 성공적으로 등록되었습니다."); // 성공
            return "redirect:/admin/banner";
        } catch (CustomException ex) {
            log.error("registMainBanner|error = {}", ex.getMessage());
            model.addAttribute("result", "fail");
            return "redirect:/admin/banner";
        } catch (RuntimeException e) {
            // 예상치 못한 다른 종류의 예외 처리
            log.error("예상치 못한 오류 발생 = {}", e.getMessage());
            return "admin/banner/banner-regist";
        } catch (IOException e) {
            log.error("예상치 못한 오류 발생 = {}", e.getMessage());
            throw new RuntimeException(e);
        }

    }

    /**
     * 관리자 > 배너관리 > 배너수정화면
     * @return
     */
    @GetMapping("/edit/{bNo}")
    public String getEditMainBannerView(@PathVariable("bNo") Long bNo, Model model) {

        AdminBannerResDto bannerInfo = adminBannerService.findBannerById(bNo);

        model.addAttribute("bannerInfo", bannerInfo);

        return "admin/banner/banner-edit";
    }

    /**
     * 관리자 > 배너관리 > 배너수정
     */
    @PutMapping("/edit/{bNo}")
    public String editBanner(@PathVariable("bNo") Long bNo,
                             @Valid @ModelAttribute("bannerInfo") AdminBannerEditReqDto adminBannerEditReqDto,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if(bindingResult.hasErrors()) {
            return "admin/banner/banner-edit";
        }

        try {
            log.debug("bno = {}, adminBannerEditReqDto = {} ", bNo, adminBannerEditReqDto);
            adminBannerService.editBanner(bNo, adminBannerEditReqDto);
            redirectAttributes.addFlashAttribute("alertMessage", "배너정보가 성공적으로 수정되었습니다."); // 성공
            return "redirect:/admin/banner/edit/" + bNo;
        } catch (CustomException ex) {
            return "admin/banner/banner-edit";
        }
    }

    @ResponseBody
    @DeleteMapping("/delete/{bNo}")
    public void deleteBanner(@PathVariable("bNo") Long bNo) {
        try {
            adminBannerService.deleteBanner(bNo);
        } catch (CustomException ex) {
            log.error("deleteBanner|error = {}", ex.getMessage());

        } catch (RuntimeException e) {
            // 예상치 못한 다른 종류의 예외 처리
            log.error("예상치 못한 오류 발생", e);
        }
    }

}
