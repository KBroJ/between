package com.wb.between.reservation.reserve.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReservationModificationDetailDto {
    private Long resNo;
    private String planType; // HOURLY, DAILY, MONTHLY
    private String reservationDate; // "YYYY-MM-DD"
    private SeatInfo seatInfo;
    private List<String> selectedTimes;
    private String couponId;
    private String status; // 예약 상태 (예: CONFIRMED)
    private String originalTotalPrice;

    @Data
    @Builder
    public static class SeatInfo {
        private Long id;
        private String name;
        private String type; // SEAT, ROOM 등
    }
}
