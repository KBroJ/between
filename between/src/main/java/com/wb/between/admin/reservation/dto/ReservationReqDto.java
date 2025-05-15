package com.wb.between.admin.reservation.dto;

import lombok.Data;

import java.time.LocalDateTime;

/*
    ReservationReqDto.java
    관리자가 예약 관리 페이지에서
     - 수정/취소 시 필요한 데이터 전송 객체 (DTO)
     - 예약 번호, 좌석 번호, 예약 시작/종료 시간, 요금제, 수정 사유 등
 */
@Data
public class ReservationReqDto {

    private Long seatNo;            // 좌석 번호
    private LocalDateTime resStart; // 예약 시작 / 컨트롤러에서 @DateTimeFormat으로 변환된 값을 받음
    private LocalDateTime resEnd;   // 예약 종료
    private String planType;        // 요금제
    private String moReason;    // 수정 사유

}
