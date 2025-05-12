package com.wb.between.admin.banner.dto;

import com.wb.between.banner.domain.Banner;
import lombok.Builder;
import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;


import java.time.LocalDateTime;

@Getter
@Builder
public class AdminBannerResDto {

    private Long bNo;

    private String bTitle;

    private String bImageUrl;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startDt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endDt;

    private String register;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime createDt;

    private String useAt;

    private Integer sortOrder;

    public static AdminBannerResDto from(Banner banner) {
            return AdminBannerResDto.builder()
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
