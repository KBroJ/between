package com.wb.between.admin.popup.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
public class AdminPopupEditReqDto {

    private Long popupId;

    private String title;

    private String contentType;

    private String contentBody;

    private LocalDateTime startDt;

    private LocalDateTime endDt;

    private String useAt;

    private String linkUrl;

    private int displayOrder;

    private String showOnceCookieName;

    //타임리프 바인딩 필드
    private boolean active;

    //active가 true면 y, 아니면 n
    public String getUseAt() {
        return this.active ? "Y" : "N";
    }

    //useAt yn으로 tf 설정
    public void setUseAt(String useAt) {
        this.active = "Y".equalsIgnoreCase(useAt);
    }
}
