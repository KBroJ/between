package com.wb.between.reservation.seat.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class TimeService {

    private static final LocalTime OPEN_TIME = LocalTime.of(9, 0);
    private static final LocalTime CLOSE_TIME = LocalTime.of(22, 0);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 특정 좌석/날짜의 예약 가능 시간 목록 조회 (임시 로직)
     */
    public List<String> getAvailableTimes(LocalDate date, String seatId) { // branchId 제거됨
        System.out.printf("예약 가능 시간 조회 요청 - 날짜: %s, 좌석: %s%n", date, seatId);
        List<String> availableTimes = new ArrayList<>();

        // --- !!! 실제 예약 정보 조회 로직 필요 !!! ---
        Set<String> reservedStartTimes = Set.of(); // 임시 빈 Set
        if (date.getDayOfMonth() % 2 != 0) { // 홀수 날짜 임시 예약
            if (seatId != null && seatId.contains("S")) reservedStartTimes = Set.of("10:00", "14:00");
            else reservedStartTimes = Set.of("13:00");
        } else { // 짝수 날짜 임시 예약
            if (seatId != null && seatId.contains("S")) reservedStartTimes = Set.of("11:00", "15:00");
            else reservedStartTimes = Set.of("12:00");
        }
        System.out.println("임시 예약된 시간: " + reservedStartTimes);
        // -----------------------------------------

        LocalTime currentTimeSlot = OPEN_TIME;
        while (currentTimeSlot.isBefore(CLOSE_TIME)) {
            String timeSlotString = currentTimeSlot.format(TIME_FORMATTER);
            if (!reservedStartTimes.contains(timeSlotString)) {
                availableTimes.add(timeSlotString);
            }
            currentTimeSlot = currentTimeSlot.plusHours(1);
        }
        System.out.println("반환될 예약 가능 시간: " + availableTimes);
        return availableTimes;
    }
}