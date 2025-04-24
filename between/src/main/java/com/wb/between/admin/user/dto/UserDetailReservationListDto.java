package com.wb.between.admin.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/*
    DTO : 화면에 필요한 데이터만 전달하고 데이터를 가공하기 위한 객체
*/
@Data
@Builder
public class UserDetailReservationListDto {

    private Long resNo;
    private LocalDateTime resDt;    // 예약시간
    private Long seatNo;            // 좌석번호
    private String seatNm;          // 좌석명
    private Boolean resStatus;      // 예약 상태 (null: 보류, 1: 완료 (true), 0 : 취소 (false))
    private String resStatusNm;      // 예약 상태 (null: 보류, 1: 완료 (true), 0 : 취소 (false))
    private LocalDateTime resStart; // 예약 시작 시각 (날짜 + 시간)
    private LocalDateTime resEnd;   // 예약 종료 시각 (날짜 + 시간)

}
