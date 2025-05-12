package com.wb.between.reservation.seat.service; // 패키지명 확인



import com.wb.between.reservation.reserve.domain.Reservation;
import com.wb.between.reservation.reserve.repository.ReservationRepository;
import com.wb.between.reservation.seat.domain.Seat;
import com.wb.between.reservation.seat.dto.FloorDto;
import com.wb.between.reservation.seat.dto.SeatDto;
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
    @Transactional(readOnly = true)
    public List<SeatDto> getSeatStatus(LocalDate date, Integer floor) {
        System.out.printf("[SeatService] DB 좌석 조회 요청 - 날짜: %s, 층: %d (기본 상태만 확인)%n", date, floor);

        // 1. 해당 '층'의 사용 중인 좌석 조회
        List<Seat> activeSeatsOnFloor = seatRepository.findByFloorAndUseAtTrue(floor);
        if (activeSeatsOnFloor.isEmpty()) {
            System.out.println("[SeatService] 해당 층에 활성 좌석 없음 (floor: " + floor +")");
            return List.of();
        }
        System.out.printf("[SeatService] 활성 좌석 %d건 조회 완료 (층 %d)%n", activeSeatsOnFloor.size(), floor);



        // 2. DTO 변환 (예약 상태 고려 없이 AVAILABLE/STATIC만 설정)
        List<SeatDto> seatDtos = activeSeatsOnFloor.stream()
                .map(seat -> {
                    String status;
                    String seatType = mapSeatSortToType(seat.getSeatSort());

                    if (!seat.isUseAt() || "AREA".equals(seatType)) {
                        status = "STATIC";
                    } else {
                        // 예약 여부 확인 없이 무조건 AVAILABLE
                        status = "AVAILABLE";
                    }
                    // ---------------------------------

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

        System.out.println("[SeatService] 최종 반환 DTO 목록 수: " + seatDtos.size());
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