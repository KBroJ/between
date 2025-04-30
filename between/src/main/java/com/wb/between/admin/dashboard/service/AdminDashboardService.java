package com.wb.between.admin.dashboard.service;

import com.wb.between.coupon.service.CouponService;
import com.wb.between.reservation.reserve.service.ReservationService;
import com.wb.between.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    //예약서비스
    private final ReservationService reservationService;

    //회원서비스
    private final UserService userService;

    //쿠폰서비스
    private final CouponService couponService;


    public void getDashboardData() {
//        reservationService.
    }

}
