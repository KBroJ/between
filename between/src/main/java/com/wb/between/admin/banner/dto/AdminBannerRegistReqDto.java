package com.wb.between.admin.banner.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AdminBannerRegistReqDto {

    private Long bNo;

    private String bTitle;

    private String bImageUrl;

    private LocalDateTime startDt;

    private LocalDateTime endDt;

    private String register;

    private LocalDateTime createDt;

    private String useAt;

    private Integer sortOrder;

    private MultipartFile bannerImgFile;

    private String originalFileName;

    private Long fileSize;

    private String mimeType;

}
