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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TimeService {

    @Autowired
    private ReservationRepository reservationRepository;

    private static final LocalTime OPEN_TIME = LocalTime.MIDNIGHT; // 운영 시간
    private static final LocalTime CLOSE_TIME = LocalTime.MAX; // 운영 시간
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


        if (reservationRepository.isSeatBlockedForEntireDay(seatNo, date)) {
            System.out.println("[TimeService] 해당 날짜(" + date + ")는 일일권/월정액권으로 전체 예약됨. 모든 시간 BOOKED 처리.");
            List<TimeDto> allBookedSlots = new ArrayList<>();
            for (int hour = 0; hour < 24; hour++) { // 24시간 기준
                LocalTime currentTimeSlot = LocalTime.of(hour, 0);
                allBookedSlots.add(new TimeDto(currentTimeSlot.format(TIME_FORMATTER), "BOOKED"));
            }
            return allBookedSlots; // 모든 시간 슬롯을 "BOOKED"로 반환
        }


        // 1. 해당 좌석/날짜의 확정된 예약 정보 조회
        Boolean confirmedStatus = true;
        List<Reservation> reservations = reservationRepository.findBySeatNoAndDateAndStatus(seatNo, date, confirmedStatus);

        // 2. 예약된 시간 슬롯("HH:mm") Set 생성
        Set<String> reservedStartTimes = new HashSet<>();
        for (Reservation res : reservations) {
            LocalDateTime start = res.getResStart();
            LocalDateTime end = res.getResEnd();
            LocalDateTime currentSlotInReservation = start;
            // 예약 기간 내의 모든 시간 슬롯을 추가
            while (currentSlotInReservation.isBefore(end)) {
                // 조회하려는 날짜(date)에 해당하는 시간만 포함
                if (currentSlotInReservation.toLocalDate().equals(date)) {
                    reservedStartTimes.add(currentSlotInReservation.format(TIME_FORMATTER));
                }
                currentSlotInReservation = currentSlotInReservation.plusHours(1);
            }
        }
        System.out.println("DB 기반 예약된 시간(Set): " + reservedStartTimes);

        // 3. 모든 시간 슬롯 생성 및 상태 결정
        List<TimeDto> timeSlots = new ArrayList<>();
        ZoneId kstZoneId = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(kstZoneId);
        LocalTime now = LocalTime.now(kstZoneId); // 현재 시각

        for (int hour = 0; hour < 24; hour++) {
            LocalTime currentTimeSlot = LocalTime.of(hour, 0); // 예: 00:00, 01:00, ..., 23:00
            String timeSlotString = currentTimeSlot.format(TIME_FORMATTER);
            String status;

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
        }

        System.out.println("반환될 시간 슬롯 상태 목록 수: " + timeSlots.size());
        return timeSlots; // 최종 리스트 반환
    }
}