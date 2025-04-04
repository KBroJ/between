package com.wb.between.reservation.seat.service; // 패키지명 확인

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

    /**
     * 모든 활성 좌석 목록과 위치 정보를 조회합니다.
     * (주의: 현재 코드에서는 실제 예약 상태를 반영하지 않습니다.)
     *
     * @param date 조회 날짜 (현재 로직에서는 사용되지 않음)
     * @return 좌석 정보(SeatDto) 리스트
     */
    @Transactional(readOnly = true)
    public List<SeatDto> getSeatStatus(LocalDate date) {
        System.out.printf("DB 연동 좌석 정보 조회 요청 - 날짜: %s (위치 정보 포함, 예약 상태 미반영)%n", date);

        // 1. DB에서 사용 중(useAt=true)인 모든 좌석 정보 조회
        // (SeatRepository의 findByUseAtTrue 메소드는 boolean<->Y/N 컨버터가 정상 동작해야 함)
        List<Seat> activeSeats = seatRepository.findByUseAtTrue();

        if (activeSeats.isEmpty()) {
            System.out.println("사용 가능한 좌석 정보 없음");
            return List.of(); // 빈 리스트 반환
        }

        // 2. 좌석 목록을 DTO 리스트로 변환
        List<SeatDto> seatDtos = activeSeats.stream()
                .map(seat -> {
                    String status;
                    String seatType = mapSeatSortToType(seat.getSeatSort()); // DB 값 -> 프론트엔드 타입 변환

                    // DB의 useAt 필드가 false ('N') 이거나 좌석 타입이 AREA면 STATIC 처리
                    if (!seat.isUseAt() || "AREA".equals(seatType)) {
                        status = "STATIC";
                    } else {
                        // !!! 현재 예약 상태 확인 로직 없음 !!!
                        // DB useAt='Y' 이고 AREA 타입 아니면 일단 AVAILABLE 처리
                        status = "AVAILABLE";
                    }

                    // Seat Entity 객체 -> SeatDto 객체 변환
                    return new SeatDto(
                            String.valueOf(seat.getSeatNo()), // Long ID를 String으로 변환
                            seat.getSeatNm(),                 // 좌석 이름
                            status,                           // 계산된 상태 (AVAILABLE 또는 STATIC)
                            seatType,                         // 변환된 좌석 타입
                            seat.getGridRow(),                // DB의 gridRow 값 (Java null일 수 있음)
                            seat.getGridColumn()              // DB의 gridColumn 값 (Java null일 수 있음)
                    );
                })
                .collect(Collectors.toList());

        System.out.println("반환될 좌석 DTO 목록 수: " + seatDtos.size());
        return seatDtos;
    }

    /**
     * DB의 seatSort 값 (예: "개인", "회의실")을
     * 프론트엔드에서 사용할 타입 문자열 (예: "SEAT", "ROOM")로 변환합니다.
     * @param seatSort DB에서 읽어온 seatSort 값
     * @return 변환된 타입 문자열 (기본값 "SEAT")
     */
    private String mapSeatSortToType(String seatSort) {
        if (seatSort == null) {
            return "SEAT"; // null이면 기본 SEAT 타입
        }
        switch (seatSort) {
            case "개인":
                return "SEAT";
            case "회의실":
                return "ROOM";
            // 다른 타입이 있다면 case 추가
            default:
                // DB에 정의되지 않은 값이거나 "출입문" 등 고정 영역일 경우 AREA로 처리 (예시)
                return "AREA";
        }
    }
}