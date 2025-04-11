package com.wb.between.mypage.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class MyReservationDetailDto {

    // Reservation Info
    private Long resNo;
    private LocalDateTime resDt; // 예약 신청일
    private String displayStatus; // 표시용 상태 문자열 (예: "예약완료", "이용완료", "취소됨")
    private String statusCode;    // 상태 코드 (예: "1", "2", "3" - 버튼 조건 등에 사용)

    // Seat Info
    private String seatNm;
    private String seatSort; // 예: "개인석", "회의실"

    // Time Info
    private LocalDateTime resStart;
    private LocalDateTime resEnd;
    // 필요시 기간(Duration) 필드 추가 가능

    // Payment Info
    private String resPrice;  // 원래 가격
    private String dcPrice;   // 할인 금액
    private String totalPrice; // 최종 결제 금액
    private String paymentMethod; // 결제 수단 (예: "카드", "가상계좌") - Payment 테이블 연동 필요
    private LocalDateTime paymentApproveDt; // 결제 승인 시각 - Payment 테이블 연동 필요
    // 필요시 쿠폰 정보 필드 추가 가능 (userCpNo)

    // Action Flags (Service에서 비즈니스 로직에 따라 계산)
    private boolean canModify; // 예약 변경 가능 여부
    private boolean canCancel; // 예약 취소 가능 여부

}
