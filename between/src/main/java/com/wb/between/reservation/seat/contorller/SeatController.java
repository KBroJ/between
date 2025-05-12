package com.wb.between.reservation.seat.contorller;

import com.wb.between.reservation.seat.dto.FloorDto;
import com.wb.between.reservation.seat.dto.SeatDto;     // SeatDto 경로 확인
import com.wb.between.reservation.seat.service.SeatService; // SeatService 경로 확인
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:63342") // !!! 중요: 실제 프론트엔드 주소로 변경 !!!
public class SeatController {

    @Autowired
    private SeatService seatService;

    /**
     * 특정 날짜의 좌석 목록 조회 API (예약 상태 미반영)
     * @param date 조회 날짜 (YYYY-MM-DD)
     * @return 좌석 DTO 목록
     */
    @GetMapping("/seats")
    public ResponseEntity<?> getSeats(
            // branchId 파라미터 제거됨
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestParam Integer floor) {

        try {
            // 서비스 호출 시 branchId 제거됨
            List<SeatDto> seatStatus = seatService.getSeatStatus(date, floor);
            return ResponseEntity.ok(seatStatus);
        } catch (Exception e) {
            System.err.println("Error fetching seat status: " + e.getMessage());
            e.printStackTrace(); // 개발 중 에러 확인을 위해 추가
            return ResponseEntity.internalServerError().body("좌석 정보 조회 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/floors")
    public ResponseEntity<?> getFloors(){
        try{
            List<FloorDto> floors = seatService.getActiveFloors();
            return ResponseEntity.ok(floors);
        } catch (Exception e){
            e.printStackTrace();;
            return ResponseEntity.internalServerError().body(Map.of("message", "층 정보를 불러오는데 실패했습니다."));
        }
    }
}