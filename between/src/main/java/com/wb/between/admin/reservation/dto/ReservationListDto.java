package com.wb.between.admin.reservation.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

// 관리자 > 예약 관리 페이지 : 예약 목록 조회시 데이터를 담는 DTO
@Data
@Builder
public class ReservationListDto {

    private Long resNo;             // 예약 번호 (PK, 상세 보기 링크 등에 사용)
    private LocalDateTime resDt;    // 예약 신청 일시 (테이블 '예약일' 컬럼)
    private String userEmail;       // 예약자 이메일
    private String userName;        // 예약자 이름
    private String seatNm;          // 좌석 정보 (좌석 이름)
    private LocalDateTime resStart; // 실제 이용 시작 시간
    private LocalDateTime resEnd;   // 실제 이용 종료 시간
    private String totalPrice;  // 결제 금액 (nullable 가능)
    private String resStatus;        // 예약 상태 문자열 ("완료", "취소", "확인 불가" 등)

}
