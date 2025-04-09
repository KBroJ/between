package com.wb.between.reservation.seat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TimeDto {
    private String startTime; // 시작 시간 "HH:mm"
    // private String endTime; // 필요시 종료 시간도 추가 가능
    private String status;    // 상태: "AVAILABLE", "BOOKED", "PAST"
}
