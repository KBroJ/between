package com.wb.between.banner.dto;

import com.wb.between.banner.domain.Banner;
import lombok.Builder;
import lombok.Getter;

import java.math.BigInteger;
import java.util.Date;

@Getter
@Builder
public class BannerListResponseDto {

    private Long bNo;

    private String bTitle;

    private String bImageUrl;

    private Date startDt;

    private Date endDt;

    private String register;

    private Date createDt;

    private String useAt;

    public static BannerListResponseDto from(Banner banner) {
        return BannerListResponseDto.builder()
                .bNo(banner.getBNo())
                .bTitle(banner.getBTitle())
                .bImageUrl(banner.getBImageUrl())
                .startDt(banner.getStartDt())
                .endDt(banner.getEndDt())
                .register(banner.getRegister())
                .createDt(banner.getCreateDt())
                .useAt(banner.getUseAt())
                .build();

    }
}
