package com.wb.between.coupon.service;

import com.wb.between.common.exception.CustomException;
import com.wb.between.common.exception.ErrorCode;
import com.wb.between.coupon.domain.Coupon;
import com.wb.between.coupon.repository.CouponRepository;
import com.wb.between.user.domain.User;
import com.wb.between.usercoupon.domain.UserCoupon;
import com.wb.between.usercoupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class CouponService {

    private final CouponRepository couponRepository;
    
    private final UserCouponRepository userCouponRepository;

    /**
     * 회원가입 쿠폰 발급
     */
    @Transactional
    public void singupCoupon(User user) {
        //1. 회원가입 쿠폰 조회
        Coupon coupon = couponRepository.findById(1L).orElseThrow(()-> new CustomException(ErrorCode.INVALID_INPUT));
        log.debug("singupCoupon|coupon = {}", coupon);
        //2. 유저쿠폰 생성
        UserCoupon userCoupon = UserCoupon.builder()
                .user(user)
                .coupon(coupon)
                .useAt("Y")
                .build();

        //3. 유저 쿠폰 등록
        userCouponRepository.save(userCoupon);

    }

}
