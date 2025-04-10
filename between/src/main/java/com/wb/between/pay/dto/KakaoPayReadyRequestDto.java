package com.wb.between.pay.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString // 로그 출력 시 필드 값 보기 좋게
public class KakaoPayReadyRequestDto {
    // 프론트엔드에서 받아야 할 정보들
    private Long itemId;            // 예약 항목 ID (좌석 번호 등)
    private String itemName;        // 상품명 (카카오페이 화면에 표시됨, 예: "좌석 A01 시간제")
    private Integer quantity;       // 상품 수량 (보통 좌석 예약은 1)
    private Integer totalAmount;    // 최종 결제 금액 (할인 적용 후)
    private String userId;          // 사용자 고유 ID (카카오페이 전달용)

    // 백엔드에서 추가하거나 프론트에서 같이 받을 수 있는 정보들
    private String planType;        // 요금제 (HOURLY, DAILY, MONTHLY) - 가격 검증 등에 사용
    private String reservationDate; // 예약 날짜 (YYYY-MM-DD)
    private List<String> selectedTimes; // 시간제 선택 시간 목록
    private String couponId;        // 사용한 쿠폰 ID

    // 백엔드에서 생성/설정할 정보 (DTO에 꼭 포함될 필요는 없음)
     private String partnerOrderId;  // 백엔드에서 생성할 주문번호
    // private String partnerUserId; // 백엔드에서 생성/관리할 사용자 ID
}
