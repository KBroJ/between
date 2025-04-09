package com.wb.between.reservation.seat.service;

import com.wb.between.reservation.reserve.domain.Reservation;
import com.wb.between.reservation.reserve.repository.ReservationRepository;
import com.wb.between.reservation.seat.dto.TimeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TimeService {

    @Autowired
    private ReservationRepository reservationRepository;

    private static final LocalTime OPEN_TIME = LocalTime.of(9, 0);
    private static final LocalTime CLOSE_TIME = LocalTime.of(22, 0);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 특정 좌석/날짜의 모든 시간 슬롯 상태(AVAILABLE, BOOKED, PAST) 목록을 조회합니다.
     * @param date 조회 날짜 (LocalDate)
     * @param seatId 좌석 ID (String)
     * @return 시간 슬롯 상태 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<TimeDto> getAvailableTimesWithStatus(LocalDate date, String seatId) { // 메소드 이름 및 반환 타입 변경
        Long seatNo;
        try { seatNo = Long.parseLong(seatId); }
        catch (NumberFormatException e) { return List.of(); }
        System.out.printf("DB 연동 시간 슬롯 상태 조회 요청 - 날짜: %s, 좌석번호: %d%n", date, seatNo);

        // 1. 해당 좌석/날짜의 확정된 예약 정보 조회
        Boolean confirmedStatus = true; // !!! 실제 '확정' 상태값 확인 !!!
        List<Reservation> reservations = reservationRepository.findBySeatNoAndDateAndStatus(seatNo, date, confirmedStatus);

        // 2. 예약된 시간 슬롯("HH:mm") Set 생성
        Set<String> reservedStartTimes = new HashSet<>();
        for (Reservation res : reservations) {
            LocalDateTime start = res.getResStart(); LocalDateTime end = res.getResEnd();
            LocalDateTime current = start;
            while (current.isBefore(end)) {
                if (current.toLocalDate().equals(date)) { reservedStartTimes.add(current.format(TIME_FORMATTER)); }
                current = current.plusHours(1);
            }
        }
        System.out.println("DB 기반 예약된 시간(Set): " + reservedStartTimes);

        // 3. 모든 시간 슬롯 생성 및 상태 결정
        List<TimeDto> timeSlots = new ArrayList<>();
        LocalTime currentTimeSlot = OPEN_TIME;
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now(); // 현재 시각

        while (currentTimeSlot.isBefore(CLOSE_TIME)) {
            String timeSlotString = currentTimeSlot.format(TIME_FORMATTER);
            String status;

            // --- 상태 결정 로직 (우선순위: 과거 > 예약됨 > 가능) ---
            // 1) 오늘 이전 날짜거나, 오늘이면서 이미 지나간 시간인가?
            if (date.isBefore(today) || (date.equals(today) && currentTimeSlot.isBefore(now))) {
                status = "PAST";
                // 2) 예약된 시간 목록에 포함되는가?
            } else if (reservedStartTimes.contains(timeSlotString)) {
                status = "BOOKED";
                // 3) 둘 다 아니면 예약 가능
            } else {
                status = "AVAILABLE";
            }
            // -------------------------------------------------

            timeSlots.add(new TimeDto(timeSlotString, status)); // DTO 생성 및 리스트 추가
            currentTimeSlot = currentTimeSlot.plusHours(1);
        }

        System.out.println("반환될 시간 슬롯 상태 목록 수: " + timeSlots.size());
        return timeSlots; // 최종 리스트 반환
    }
}