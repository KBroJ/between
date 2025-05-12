package com.wb.between.banner.dto;

import com.wb.between.banner.domain.Banner;
import lombok.Builder;
import lombok.Getter;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Builder
public class BannerListResponseDto {

    private Long bNo;

    private String bTitle;

    private String bImageUrl;

    private LocalDateTime startDt;

    private LocalDateTime endDt;

    private String register;

    private LocalDateTime createDt;

    private String useAt;

    private Integer sortOrder;

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
                .sortOrder(banner.getSortOrder())
                .build();

    }
}
