package com.wb.between.reservation.seat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SeatDto {
    private String id;     // 프론트엔드 통신 편의상 String 유지 (Long.toString() 변환)
    private String name;   // seatNm 매핑
    private String status; // 계산된 상태 (AVAILABLE, UNAVAILABLE, STATIC)
    private String type;   // seatSort 매핑 (SEAT, ROOM 등)
    // createDt, updateDt, register 는 프론트엔드 표시에 필요 없으므로 제외
    private String gridRow;
    private String gridColumn;
}