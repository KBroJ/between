package com.wb.between.admin.coupon.controller;

import com.wb.between.admin.coupon.dto.AdminCouponResDto;
import com.wb.between.admin.coupon.service.AdminCouponService;
import com.wb.between.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
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


}
