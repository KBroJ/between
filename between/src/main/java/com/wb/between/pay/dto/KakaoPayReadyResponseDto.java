package com.wb.between.pay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor // 기본 생성자
@ToString
@JsonIgnoreProperties(ignoreUnknown = true) // 카카오 응답 중 모르는 필드는 무시
public class KakaoPayReadyResponseDto {

    private String tid; // 결제 고유 번호 (결제 요청 시 필수)

    @JsonProperty("next_redirect_pc_url") // JSON의 snake_case -> Java camelCase
    private String nextRedirectPcUrl; // PC 웹용 결제 페이지 URL

    @JsonProperty("next_redirect_mobile_url")
    private String nextRedirectMobileUrl; // 모바일 웹용 결제 페이지 URL

    @JsonProperty("next_redirect_app_url")
    private String nextRedirectAppUrl; // 카카오페이 앱용 URL 스킴

    @JsonProperty("android_app_scheme")
    private String androidAppScheme; // 안드로이드 앱 스킴

    @JsonProperty("ios_app_scheme")
    private String iosAppScheme; // iOS 앱 스킴

    @JsonProperty("created_at")
    private String createdAt; // 결제 준비 요청 시간

    // --- 백엔드에서 추가로 내려줄 수 있는 정보 (프론트 리다이렉트용) ---
    private boolean success = true; // 성공 여부 플래그 (프론트 처리용)
    private String message = "카카오페이 결제 준비 완료"; // 메시지
    // -------------------------------------------------------
}