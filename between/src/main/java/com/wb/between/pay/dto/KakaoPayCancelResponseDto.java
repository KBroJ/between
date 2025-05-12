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
public class KakaoPayCancelResponseDto {

    private String aid; // 요청 고유 번호
    private String tid; // 결제 고유 번호
    private String cid; // 가맹점 코드
    private String status; // 결제 상태 (예: "CANCEL_PAYMENT")

    @JsonProperty("partner_order_id")
    private String partnerOrderId;

    @JsonProperty("partner_user_id")
    private String partnerUserId;

    @JsonProperty("payment_method_type")
    private String paymentMethodType; // 결제 수단

    @JsonProperty("item_name")
    private String itemName;

    @JsonProperty("item_code")
    private String itemCode;

    private Integer quantity; // 상품 수량

    // 취소 관련 금액 정보
    @JsonProperty("approved_cancel_amount")
    private AmountDto approvedCancelAmount; // 이번 요청으로 취소된 금액

    @JsonProperty("canceled_amount")
    private AmountDto canceledAmount; // 누계 취소 금액

    @JsonProperty("cancel_available_amount")
    private AmountDto cancelAvailableAmount; // 남은 취소 가능 금액

    @JsonProperty("created_at")
    private String createdAt; // 결제 준비 요청 시각

    @JsonProperty("approved_at")
    private String approvedAt; // 결제 승인 시각

    @JsonProperty("canceled_at")
    private String canceledAt; // 결제 취소 시각

    private String payload; // 취소 요청 시 전달한 값
}
