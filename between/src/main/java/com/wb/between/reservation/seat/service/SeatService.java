package com.wb.between.reservation.seat.service; // 패키지명 확인



import com.wb.between.reservation.reserve.domain.Reservation;
import com.wb.between.reservation.reserve.repository.ReservationRepository;
import com.wb.between.reservation.seat.domain.Seat;
import com.wb.between.reservation.seat.dto.FloorDto;
import com.wb.between.reservation.seat.dto.SeatDto;
import com.wb.between.reservation.seat.dto.SeatResponseDto;
import com.wb.between.reservation.seat.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;


import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.collect;

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
    @Transactional(readOnly = true) // 지연 로딩된 prices 컬렉션 접근 위해 필요 (또는 JOIN FETCH 사용)
    public List<SeatResponseDto> getSeatStatus(LocalDate date, Integer floor) {
        System.out.printf("[SeatService] 사용자 예약 페이지 좌석 조회 - 날짜: %s, 층: %d%n", date, floor);

        List<Seat> activeSeatsOnFloor;
        if (floor != null && floor > 0) { // 특정 층 조회
            activeSeatsOnFloor = seatRepository.findByFloorAndUseAtTrueWithPrices(floor);
        } else { // 전체 층 조회 (또는 기본 층만 보여줄 경우 이 분기 제거)
            // activeSeatsOnFloor = seatRepository.findByUseAtTrueWithPrices(); // 예시
            // 여기서는 특정 층 조회가 필수라고 가정하고, floor가 null이면 빈 리스트 반환 또는 에러 처리
            if (floor == null) {
                System.out.println("[SeatService] 층 정보가 없습니다. 빈 목록을 반환합니다.");
                return List.of();
            }
            // 위에서 이미 floor > 0 체크 했으므로, 실제로는 floor가 null이 아닌 경우만 이 서비스에 도달해야 함
            // Controller 단에서 floor 파라미터를 필수로 받거나 기본값을 설정하는 것이 좋음
            activeSeatsOnFloor = seatRepository.findByFloorAndUseAtTrueWithPrices(floor);
        }
        // ---------------------------------------------------------

        if (activeSeatsOnFloor.isEmpty()) {
            System.out.println("[SeatService] 해당 층에 활성 좌석 없음 (floor: " + floor +")");
            return List.of();
        }

        // DTO 변환 (이 과정에서 SeatResponseDto 생성자가 seat.getPrices()를 호출)
        List<SeatResponseDto> seatDtos = activeSeatsOnFloor.stream()
                .map(seat -> {
                    String status;
                    String seatType = mapSeatSortToType(seat.getSeatSort());

                    if (!seat.isUseAt() || "AREA".equals(seatType)) {
                        status = "STATIC";
                    } else {
                        status = "AVAILABLE"; // 실제 시간별 예약 가능 여부는 TimeService가 담당
                    }

                    // SeatResponseDto 생성자는 이제 seat.getPrices()를 통해 가격 정보를 채울 수 있음
                    return new SeatResponseDto(seat, status);
                })
                .collect(Collectors.toList());

        System.out.println("[SeatService] 반환 DTO 목록 수: " + seatDtos.size());
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

    @Transactional(readOnly = true)
    public List<FloorDto> getActiveFloors(){
        List<Integer> floorsNo = seatRepository.findDistinctActiveFloors();
        List<FloorDto> floors = floorsNo.stream().map(num -> new FloorDto(num, num + "층"))
                .collect(Collectors.toList());

        System.out.println("[Service] 조회된 층 목록: " + floors);
        return floors;

    }

}