package com.wb.between.reservation.seat.service; // 패키지 경로는 실제 프로젝트에 맞게 변경하세요

import com.wb.between.reservation.seat.domain.Seat;   // Seat Entity 경로 확인
import com.wb.between.reservation.seat.dto.SeatDto;     // SeatDto 경로 확인
import com.wb.between.reservation.seat.repository.SeatRepository; // SeatRepository 경로 확인
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SeatService {

    @Autowired
    private SeatRepository seatRepository;

    // ReservationRepository 의존성 제거됨

    /**
     * 모든 활성 좌석 목록을 조회합니다. (예약 가능 여부는 반영하지 않음)
     * @param date 조회 날짜 (현재 로직에서는 사용되지 않지만, 향후 예약 로직 추가 시 필요)
     * @return 좌석 DTO 목록
     */
    @Transactional(readOnly = true)
    public List<SeatDto> getSeatStatus(LocalDate date) {
        System.out.printf("DB 연동 좌석 정보 조회 요청 - 날짜: %s%n", date);
        List<Seat> activeSeats = seatRepository.findByUseAtTrue();
        if (activeSeats.isEmpty()) return List.of();
        // ... (예약 정보 조회 및 reservedSeatNos 생성 - 필요시 다시 활성화) ...
        // Set<Long> reservedSeatNos = findReservationsAndGetIds(date); // 예약 로직 분리 가정

        List<SeatDto> seatDtos = activeSeats.stream().map(seat -> {
            String status; String seatType = mapSeatSortToType(seat.getSeatSort());
            if (!seat.isUseAt() || "AREA".equals(seatType)) { status = "STATIC"; }
            // else if (reservedSeatNos.contains(seat.getSeatNo())) { status = "UNAVAILABLE"; } // 예약 로직 필요시 활성화
            else { status = "AVAILABLE"; } // 예약 로직 없으면 일단 AVAILABLE

            // !!! DTO 생성 시 gridRow, gridColumn 추가 !!!
            return new SeatDto(
                    String.valueOf(seat.getSeatNo()),
                    seat.getSeatNm(),
                    status,
                    seatType,
                    seat.getGridRow(),    // gridRow 값 전달
                    seat.getGridColumn() // gridColumn 값 전달
            );
        }).collect(Collectors.toList());
        System.out.println("반환될 좌석 DTO 목록 수: " + seatDtos.size());
        return seatDtos;
    }

    /** seatSort 값을 프론트엔드에서 사용할 타입으로 변환하는 헬퍼 메소드 */
    private String mapSeatSortToType(String seatSort) {
        if (seatSort == null) return "SEAT"; // 기본값 SEAT
        switch (seatSort) {
            case "개인": return "SEAT";
            case "회의실": return "ROOM";
            default: return "AREA"; // 그 외는 AREA 취급 (출입문 등)
        }
    }
}