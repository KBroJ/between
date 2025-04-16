package com.wb.between.reservation.reserve.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReservationUpdateRequestDto {
    private Long itemId; // 변경된 좌석/룸 ID
    private String planType; // 요금제 (변경 불가 검증용)
    private String reservationDate; // 변경된 날짜 "YYYY-MM-DD"
    private List<String> selectedTimes; // 변경된 시간 목록 ["HH:mm", ...] (시간제 경우)
    private String couponId; // 새로 선택한 쿠폰 ID (null 가능)
    private Integer totalPrice; // 프론트엔드에서 계산한 최종 가격 (백엔드 검증용)
}
