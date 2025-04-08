package com.wb.between.reservation.seat.service;

import com.wb.between.reservation.reserve.domain.Reservation;
import com.wb.between.reservation.reserve.repository.ReservationRepository;
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
    private ReservationRepository reservationRepository; // ReservationRepository 주입

    // --- 실제 운영 시간 (잠시 임시로 오전 9시~오후 10시까지만 설정함)
    private static final LocalTime OPEN_TIME = LocalTime.of(9, 0);  // 예: 오전 9시
    private static final LocalTime CLOSE_TIME = LocalTime.of(22, 0); // 예: 오후 10시 (22시 전까지 예약 가능)
    // -----------------------------------------------------
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 특정 좌석/날짜의 예약 가능 시간 목록을 DB 예약 정보를 기반으로 조회합니다.
     *
     * @param date 조회 날짜 (LocalDate)
     * @param seatId 좌석 ID (프론트엔드에서 받은 String)
     * @return 예약 가능한 시작 시간 목록 ("HH:mm" 형식 문자열 리스트)
     */
    @Transactional(readOnly = true) // DB 조회만 하므로 읽기 전용 트랜잭션
    public List<String> getAvailableTimes(LocalDate date, String seatId) {
        Long seatNo;
        try {
            // 프론트엔드에서 받은 String 타입 ID를 DB 조회 위한 Long 타입으로 변환
            seatNo = Long.parseLong(seatId);
        } catch (NumberFormatException e) {
            System.err.printf("잘못된 좌석 ID 형식입니다: %s%n", seatId);
            return List.of(); // 빈 리스트 반환
        }

        System.out.printf("DB 연동 예약 가능 시간 조회 요청 - 날짜: %s, 좌석번호: %d%n", date, seatNo);
        List<String> availableTimes = new ArrayList<>();

        // 1. 해당 좌석/날짜의 '확정된' 예약 정보 조회
        // !!! 중요: true 부분은 실제 DB의 '예약 확정' 상태값과 일치해야 합니다 !!!
        // (ReservationRepository의 findBySeatNoAndDateAndStatus 메소드 확인 필요)
        Boolean confirmedStatus = true;
        List<Reservation> reservations = reservationRepository.findBySeatNoAndDateAndStatus(seatNo, date, confirmedStatus);
        System.out.println("DB에서 조회된 확정 예약 건수: " + reservations.size());

        // 2. 예약된 모든 '시작 시간' 슬롯("HH:mm")을 Set에 저장
        Set<String> reservedStartTimes = new HashSet<>();
        for (Reservation res : reservations) {
            LocalDateTime start = res.getResStart(); // 예약 시작 시각 (LocalDateTime)
            LocalDateTime end = res.getResEnd();     // 예약 종료 시각 (LocalDateTime)

            // 예약 시작 시간부터 종료 시간 '직전'까지 1시간 단위로 반복
            LocalDateTime currentSlotStart = start;
            while (currentSlotStart.isBefore(end)) {
                // 조회하려는 날짜(date)에 해당하는 시간 슬롯만 추가
                // (혹시 모를 자정 넘어가는 예약 등을 정확히 처리하기 위함)
                if (currentSlotStart.toLocalDate().equals(date)) {
                    String formattedTime = currentSlotStart.format(TIME_FORMATTER); // "HH:mm" 형식
                    reservedStartTimes.add(formattedTime);
                }
                currentSlotStart = currentSlotStart.plusHours(1); // 다음 시간으로 이동
            }
        }
        System.out.println("DB 기반 예약된 시간(시작시간 기준): " + reservedStartTimes);

        // 3. 운영 시간 내 모든 1시간 단위 슬롯 생성
        LocalTime currentTimeSlot = OPEN_TIME;
        while (currentTimeSlot.isBefore(CLOSE_TIME)) { // CLOSE_TIME 정각은 포함하지 않음
            String timeSlotString = currentTimeSlot.format(TIME_FORMATTER);

            // 4. 예약된 시간 목록(Set)에 없는 시간이면 availableTimes 리스트에 추가
            if (!reservedStartTimes.contains(timeSlotString)) {
                availableTimes.add(timeSlotString);
            }
            currentTimeSlot = currentTimeSlot.plusHours(1); // 다음 시간 슬롯
        }

        System.out.println("최종 반환될 예약 가능 시간 목록: " + availableTimes);
        return availableTimes;
    }
}