package com.wb.between.admin.coupon.service;

import com.wb.between.admin.coupon.dto.AdminCouponResDto;
import com.wb.between.admin.coupon.repository.AdminCouponRepository;
import com.wb.between.coupon.domain.Coupon;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminCouponService {

    private final AdminCouponRepository adminCouponRepository;

    public Page<AdminCouponResDto> findAdminCouponList(Pageable pageable, String searchCouponName) {
        Page<Coupon> couponPageList = adminCouponRepository.findCouponsWithFilter(pageable, searchCouponName);
        return couponPageList.map(AdminCouponResDto::from);
    }
}
