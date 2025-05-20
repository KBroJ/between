package com.wb.between.popup.dto;

import com.wb.between.popup.domain.Popups;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PopupResDto {

    private Long popupId;

    private String title;

    private String contentType;

    private String contentBody;

    private LocalDateTime startDt;

    private LocalDateTime endDt;

    private String useAt;

    private String linkUrl;

    private int displayOrder;

    public static PopupResDto from(Popups popups) {
        return PopupResDto.builder()
                .popupId(popups.getPopupId())
                .title(popups.getTitle())
                .contentType(popups.getContentType())
                .contentBody(popups.getContentBody())
                .startDt(popups.getStartDt())
                .endDt(popups.getEndDt())
                .useAt(popups.getUseAt())
                .build();
    }
}
