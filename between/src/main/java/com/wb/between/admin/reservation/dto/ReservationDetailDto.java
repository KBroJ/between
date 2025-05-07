package com.wb.between.admin.reservation.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReservationDetailDto {

    // 기본 정보
    private Long resNo;

    // 예약자 정보
    private String userEmail;
    private String userName;
    private String userPhoneNo; // User 엔티티에 phoneNo 필드가 있다고 가정
    private String userGrade;   // User 엔티티에 authCd (회원 등급) 필드가 있다고 가정

    // 예약 정보
    private Long currentSeatNo; // 현재 예약된 좌석의 ID (수정 시 selectbox 초기 선택용)
    private String seatNm;
    private LocalDateTime resStart;
    private LocalDateTime resEnd;
    private String statusNm; // 예: "예약완료", "취소됨", "이용완료"
    private String planType; // 요금제(HOURLY, DAILY, MONTHLY 등)

    // 결제 정보
    private String totalPrice;  // 최종 결제 금액 (DB가 varchar이므로 String 유지)
    private String resPrice;    // 좌석 금액 (DB가 varchar이므로 String 유지)
    private String couponName;  // 사용한 쿠폰 이름 (없으면 null 또는 "해당 없음")
    private String dcPrice;     // 할인 금액 (DB가 varchar이므로 String 유지, 없으면 null 또는 "0")
    private boolean couponUsed; // 쿠폰 사용 여부

    // 버튼 활성화 로직용 플래그 (Service에서 설정)
    private boolean canModify;  // 수정 가능 여부
    private boolean canCancel;  // 취소 가능 여부

}
