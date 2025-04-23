package com.wb.between.reservation.reserve.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReservationUpdateRequestDto {
    // 사용자가 변경할 수 있는 필드들
    private Long itemId; // 변경된 좌석 ID (seatNo)
    private String itemType; // 필요시
    private String planType; // (검증을 위해 받음)
    private String reservationDate; // 변경된 시작 날짜 "YYYY-MM-DD"
    private List<String> selectedTimes; // 변경된 시간 목록 (시간제 경우)
    private String couponId; // 변경된 쿠폰 ID - (일단 추가했음)


}
