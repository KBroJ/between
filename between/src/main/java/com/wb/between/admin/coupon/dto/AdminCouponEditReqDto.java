package com.wb.between.admin.coupon.dto;

import com.wb.between.coupon.domain.Coupon;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AdminCouponEditReqDto {

    private Long cpNo;

    @NotBlank(message = "쿠폰명은 필수 입력 항목입니다.")
    @Size(max = 100, message = "쿠폰명은 최대 100자까지 입력 가능합니다.")
    private String cpnNm;

    @NotNull(message = "할인 값은 필수 입력 항목입니다.")
    @Positive(message = "할인 값은 0보다 커야 합니다.") // 0 초과 (또는 @Min(0)으로 0 포함)
    // @Max(value = 100, message = "할인율은 100%를 초과할 수 없습니다.") // 할인율(R)일 때 추가 검증 필요
    private String discount; // 숫자 타입으로 변경 (혹은 Integer)

    @NotBlank(message = "할인 유형은 필수 선택 항목입니다.")
    @Pattern(regexp = "[WR]", message = "할인 유형은 'W'(정액) 또는 'R'(정률) 중 하나여야 합니다.")
    private String discountAt;

    // 할인 정보 (가공된 문자열, 예: "10%", "5000원")
    private String discountInfo;

    @NotNull(message = "쿠폰 시작일시는 필수 입력 항목입니다.")
    @FutureOrPresent(message = "쿠폰 시작일시는 현재 이후여야 합니다.") // 필요에 따라 조정
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") // HTML datetime-local 형식과 맞춤
    private LocalDateTime cpnStartDt;

    @NotNull(message = "쿠폰 만료일시는 필수 입력 항목입니다.")
    @Future(message = "쿠폰 만료일시는 미래여야 합니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime cpnEndDt;

    @Size(max = 500, message = "쿠폰 설명은 최대 500자까지 입력 가능합니다.")
    private String cpnDsc;

    @Pattern(regexp = "[YN]", message = "활성 여부는 'Y' 또는 'N'이어야 합니다.")
    private String activeYn;


    public static AdminCouponResDto from(Coupon coupon) {
        return AdminCouponResDto.builder()
                .cpNo(coupon.getCpNo())
                .cpnNm(coupon.getCpnNm())
                .discount(coupon.getDiscount())
                .discountAt(coupon.getDiscountAt())
                .cpnStartDt(coupon.getCpnStartDt())
                .cpnEndDt(coupon.getCpnEndDt())
                .cpnDsc(coupon.getCpnDsc())
                .build();
    }
}
