package com.wb.between.admin.popup.dto;

import com.wb.between.popup.domain.Popups;
import lombok.Builder;
import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminPopupResDto {

    private Long popupId;

    private String title;

    private String contentType;

    private String contentBody;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startDt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endDt;

    private String useAt;

    private String linkUrl;

    private int displayOrder;

    private boolean active;

    public static AdminPopupResDto from(Popups popups) {

        boolean active = "Y".equalsIgnoreCase(popups.getUseAt());

        return AdminPopupResDto.builder()
                .popupId(popups.getPopupId())
                .title(popups.getTitle())
                .contentType(popups.getContentType())
                .contentBody(popups.getContentBody())
                .startDt(popups.getStartDt())
                .endDt(popups.getEndDt())
                .useAt(popups.getUseAt())
                .active(active)
                .build();
    }
}
