package com.wb.between.admin.coupon.controller;

import com.wb.between.admin.coupon.dto.AdminCouponEditReqDto;
import com.wb.between.admin.coupon.dto.AdminCouponRegistReqDto;
import com.wb.between.admin.coupon.dto.AdminCouponResDto;
import com.wb.between.admin.coupon.service.AdminCouponService;
import com.wb.between.common.exception.CustomException;
import com.wb.between.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
@Slf4j
public class AdminCouponController {

    private final AdminCouponService adminCouponService;

    /**
     * 관리자 > 쿠폰관리
     * @param user
     * @param model
     * @return
     */
    @GetMapping
    public String getCouponManagementView(@AuthenticationPrincipal User user,
                                          @RequestParam(required = false, defaultValue = "") String searchCouponName,
                                          @RequestParam(defaultValue = "0") int page,
                                          Model model){

        Pageable pageable = PageRequest.of(page, 10); // 예: 페이지당 10개

        Page<AdminCouponResDto> adminCouponList = adminCouponService.findAdminCouponList(pageable, searchCouponName);

        model.addAttribute("adminCouponList", adminCouponList);

        return "admin/coupon/coupon-manage";
    }

    /**
     * 관리자 > 쿠폰등록 화면
     * @param user
     * @param model
     * @return
     */
    @GetMapping("/regist")
    public String getCouponRegistView(@AuthenticationPrincipal User user,
                                          Model model){

        model.addAttribute("couponInfo", new AdminCouponRegistReqDto());
        return "admin/coupon/coupon-regist";
    }

    /**
     * 관리자 > 쿠폰등록
     * @param adminCouponRegistReqDto
     * @param bindingResult
     * @param model
     * @return
     */
    @PostMapping("/regist")
    public String couponRegist(@Valid @ModelAttribute("couponInfo")AdminCouponRegistReqDto adminCouponRegistReqDto,
                               BindingResult bindingResult,
                               Model model) {

        log.debug("adminCouponRegistReqDto {}", adminCouponRegistReqDto.getCpnNm());

        if (bindingResult.hasErrors()) {
            // 유효성 검사 실패 시, 다시 등록 폼으로 이동 (오류 메시지 표시됨)
            return "admin/coupon/coupon-regist";
        }

        try {

            adminCouponService.registAdminCoupon(adminCouponRegistReqDto);
            model.addAttribute("result", "success");

            return "redirect:/admin/coupons";
        } catch (CustomException ex) {
            log.error("couponRegist|error = {}", ex.getMessage());
            model.addAttribute("result", "fail");
            return "admin/coupon/coupon-regist";

        } catch (RuntimeException e) {
            // 예상치 못한 다른 종류의 예외 처리
            log.error("예상치 못한 오류 발생", e);
            return "admin/coupon/coupon-regist";
        }

    }

    /**
     * 쿠폰 단일 조회
     * @param cpNo
     * @param model
     * @return
     */
    @GetMapping("/edit/{cpNo}")
    public String getEditCouponView(@PathVariable Long cpNo, Model model) {

        log.debug("getEditCouponView|cpNo = {}", cpNo);

        //쿠폰번호로 단일 쿠폰정보 조회
        AdminCouponResDto adminCouponResDto = adminCouponService.findAdminCoupon(cpNo);

        model.addAttribute("couponInfo", adminCouponResDto);

        return "admin/coupon/coupon-edit";
    }

    /**
     * 관리자 > 쿠폰등록
     * @param bindingResult
     * @param model
     * @return
     */
    @PutMapping("/edit/{cpNo}")
    public String editCoupon(@PathVariable("cpNo") Long cpNo,
                             @Valid @ModelAttribute("couponInfo") AdminCouponEditReqDto adminCouponEditReqDto,
                             BindingResult bindingResult,
                             Model model) {
        log.debug("editCoupon|cpNo = {}", cpNo);
        log.debug("adminCouponRegistReqDto {}", adminCouponEditReqDto.getCpnNm());

        if (bindingResult.hasErrors()) {
            // 유효성 검사 실패 시, 다시 등록 폼으로 이동 (오류 메시지 표시됨)
            return "admin/coupon/coupon-edit";
        }

        try {

            AdminCouponResDto adminCouponResDto = adminCouponService.updateAdminCoupon(cpNo, adminCouponEditReqDto);
//            model.addAttribute("couponInfo", adminCouponResDto);

            return "redirect:/admin/coupons/edit/" + cpNo;
        } catch (CustomException ex) {
            log.error("couponRegist|error = {}", ex.getMessage());
            model.addAttribute("result", "fail");
            return "admin/coupon/coupon-edit";

        } catch (RuntimeException e) {
            // 예상치 못한 다른 종류의 예외 처리
            log.error("예상치 못한 오류 발생", e);
            return "admin/coupon/coupon-edit";
        }


    }

}
