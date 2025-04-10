package com.wb.between.pay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoPayApproveResponseDto {
    private String aid; // 요청 고유 번호 (승인/취소 구분)
    private String tid; // 결제 고유 번호
    private String cid; // 가맹점 코드 (테스트: TC0ONETIME)
    private String sid; // 정기결제용 ID (정기결제 시 사용)

    @JsonProperty("partner_order_id")
    private String partnerOrderId; // 가맹점 주문번호 (결제 준비 시 보낸 값)

    @JsonProperty("partner_user_id")
    private String partnerUserId; // 가맹점 회원 ID (결제 준비 시 보낸 값)

    @JsonProperty("payment_method_type")
    private String paymentMethodType; // 결제 수단 (CARD, MONEY)

    private AmountDto amount; // !!! 금액 상세 정보 (위에서 정의한 AmountDto 사용) !!!

    @JsonProperty("item_name")
    private String itemName; // 상품 이름 (결제 준비 시 보낸 값)

    @JsonProperty("item_code")
    private String itemCode; // 상품 코드 (결제 준비 시 보낸 값, 옵션)

    private Integer quantity; // 상품 수량

    @JsonProperty("created_at")
    // private LocalDateTime createdAt; // 타입 변환 설정 필요 시 LocalDateTime 사용 가능
    private String createdAt; // 결제 준비 요청 시각 (String)

    @JsonProperty("approved_at")
    // private LocalDateTime approvedAt; // 타입 변환 설정 필요 시 LocalDateTime 사용 가능
    private String approvedAt; // 결제 승인 시각 (String)

    private String payload; // 결제 승인 요청 시 전달한 값 (옵션)

    // 필요시 카드 정보(card_info), 카카오머니 정보 등 추가 필드 정의 가능
}