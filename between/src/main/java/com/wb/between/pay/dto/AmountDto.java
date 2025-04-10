package com.wb.between.pay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmountDto {
    private Integer total;      // 전체 결제 금액
    private Integer tax_free;   // 비과세 금액
    private Integer vat;        // 부가세 금액
    private Integer point;      // 사용한 포인트 금액
    private Integer discount;   // 할인 금액
    private Integer green_deposit; // 컵 보증금
}