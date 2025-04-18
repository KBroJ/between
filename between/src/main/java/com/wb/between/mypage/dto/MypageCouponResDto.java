package com.wb.between.mypage.dto;

import com.wb.between.user.domain.User;
import com.wb.between.usercoupon.domain.UserCoupon;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MypageCouponResDto {

    // --- Coupon 정보 ---
    private Long cpNo;          // 쿠폰 번호 (Coupon 엔티티의 ID)

    private String cpnNm;         // 쿠폰 이름 (Coupon 엔티티 필드)

    private String discountInfo;  // 할인 정보 (가공된 문자열, 예: "10%", "5000원")

    private LocalDateTime cpnEndDt;   // 쿠폰 만료일 (Coupon 엔티티 필드) - 타입 주의

    private String cpnDsc;        // 쿠폰 설명 (Coupon 엔티티 필드)

    private String discount;

    private String discountAt;

    private LocalDateTime cpnStartDt;

    private String activeYn;

    private LocalDateTime createDt;

    //---- UserCoupon ------
    private Long userCpNo;

    private LocalDateTime issueDt;

    private LocalDateTime useDt;


    public static MypageCouponResDto from(UserCoupon usercoupon) {
        return MypageCouponResDto.builder()
                .userCpNo(usercoupon.getUserCpNo())
                .cpNo(usercoupon.getCoupon().getCpNo())
                .cpnNm(usercoupon.getCoupon().getCpnNm())
                .discount(usercoupon.getCoupon().getDiscount())
                .discountAt(usercoupon.getCoupon().getDiscountAt())
                .cpnStartDt(usercoupon.getCoupon().getCpnStartDt())
                .cpnEndDt(usercoupon.getCoupon().getCpnEndDt())
                .cpnDsc(usercoupon.getCoupon().getCpnDsc())
                .issueDt(usercoupon.getIssueDt())
                .useDt(usercoupon.getUseDt())
                .build();
    }

}
