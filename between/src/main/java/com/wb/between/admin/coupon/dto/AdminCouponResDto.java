package com.wb.between.admin.coupon.dto;

import com.wb.between.coupon.domain.Coupon;
import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminCouponResDto {

    private Long cpNo;          // 쿠폰 번호 (Coupon 엔티티의 ID)

    private String cpnNm;         // 쿠폰 이름 (Coupon 엔티티 필드)

    private String discountInfo;  // 할인 정보 (가공된 문자열, 예: "10%", "5000원")

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime cpnStartDt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime cpnEndDt;   // 쿠폰 만료일 (Coupon 엔티티 필드) - 타입 주의

    private String cpnDsc;        // 쿠폰 설명 (Coupon 엔티티 필드)

    private String discount;

    private String discountAt;

    private String activeYn;

    private LocalDateTime createDt;

    public static AdminCouponResDto from(Coupon coupon) {
        return AdminCouponResDto.builder()
                .cpNo(coupon.getCpNo())
                .cpnNm(coupon.getCpnNm())
                .discount(coupon.getDiscount())
                .discountAt(coupon.getDiscountAt())
                .cpnStartDt(coupon.getCpnStartDt())
                .cpnEndDt(coupon.getCpnEndDt())
                .cpnDsc(coupon.getCpnDsc())
                .activeYn(coupon.getActiveYn())
                .build();
    }
}
