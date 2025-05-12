package com.wb.between.admin.price.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Setter
@Getter
@NoArgsConstructor
public class PriceDto {

    @Pattern(regexp = "^(HOURLY|DAILY|MONTHLY)$", message = "가격 타입은 HOURLY, DAILY, MONTHLY 중 하나여야 합니다.")
    private String type;


    @NotBlank(message = "가격은 필수입니다.")
    @Pattern(regexp = "^[0-9]+$", message = "가격은 숫자만 입력 가능합니다.")
    private String price;

    public PriceDto(String type, String price) {
        this.type = type;
        this.price = price;
    }

}
