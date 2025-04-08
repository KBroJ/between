package com.wb.between.reservation.seat.service; // 패키지명 확인


import com.wb.between.reservation.reserve.domain.Reservation;
import com.wb.between.reservation.reserve.repository.ReservationRepository;
import com.wb.between.reservation.seat.domain.Seat;
import com.wb.between.reservation.seat.dto.SeatDto;
import com.wb.between.reservation.seat.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set; // Set 사용 위해 import 추가
import java.util.stream.Collectors;

@Service
public class SeatService {

    @Autowired
    private SeatRepository seatRepository;


    @Autowired
    private ReservationRepository reservationRepository;


    /**
     * 모든 활성 좌석 목록과 기본 상태(STATIC/AVAILABLE) 정보를 조회합니다.
     *
     * @param date 조회 날짜 (참고용)
     * @return 좌석 정보(SeatDto) 리스트
     */
    @Transactional(readOnly = true)
    public List<SeatDto> getSeatStatus(LocalDate date) {
        System.out.printf("DB 연동 좌석 정보 조회 요청 - 날짜: %s (기본 상태만 확인)%n", date);

        // 1. DB에서 사용 중(useAt=true)인 모든 좌석 정보 조회
        List<Seat> activeSeats = seatRepository.findByUseAtTrue();

        if (activeSeats.isEmpty()) {
            System.out.println("사용 가능한 좌석 정보 없음");
            return List.of();
        }

        // 2. 좌석 목록을 DTO로 변환 (예약 상태 고려 없이 AVAILABLE/STATIC만 설정)
        List<SeatDto> seatDtos = activeSeats.stream()
                .map(seat -> {
                    String status;
                    String seatType = mapSeatSortToType(seat.getSeatSort());

                    // DB useAt=false ('N') 이거나 타입이 AREA면 STATIC
                    if (!seat.isUseAt() || "AREA".equals(seatType)) {
                        status = "STATIC";
                    } else {
                        // 예약 여부 확인 없이 무조건 AVAILABLE
                        status = "AVAILABLE";
                    }

                    // Entity -> DTO 변환
                    return new SeatDto(
                            String.valueOf(seat.getSeatNo()),
                            seat.getSeatNm(),
                            status, // AVAILABLE 또는 STATIC
                            seatType,
                            seat.getGridRow(),
                            seat.getGridColumn()
                    );
                })
                .collect(Collectors.toList());

        System.out.println("반환될 좌석 DTO 목록 수 (기본 상태): " + seatDtos.size());
        return seatDtos;
    }

    // seatSort 값을 프론트엔드용 타입으로 변환하는 헬퍼 메소드
    private String mapSeatSortToType(String seatSort) {
        if (seatSort == null) return "SEAT";
        switch (seatSort) {
            case "개인": return "SEAT";
            case "회의실": return "ROOM";
            default: return "AREA";
        }
    }
}