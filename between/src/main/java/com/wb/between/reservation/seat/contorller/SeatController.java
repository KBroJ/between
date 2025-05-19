package com.wb.between.reservation.seat.contorller;

import com.wb.between.reservation.seat.dto.FloorDto;
import com.wb.between.reservation.seat.dto.SeatDto;
import com.wb.between.reservation.seat.dto.SeatResponseDto;
import com.wb.between.reservation.seat.service.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
     *
     * @param date 조회 날짜 (YYYY-MM-DD)
     * @return 좌석 DTO 목록
     */
    @GetMapping("/seats")
    public ResponseEntity<?> getSeats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestParam Integer floor) {

        try {
            // 서비스 호출 시 branchId 제거됨
            List<SeatResponseDto> seatStatusList = seatService.getSeatStatus(date, floor);
            if (seatStatusList.isEmpty()) {
                System.out.println("해당 조건의 좌석 정보 없음: Date=" + date + ", Floor=" + floor);
                // 데이터가 없는 것도 정상적인 응답이므로 200 OK 와 빈 리스트 반환 가능
                return ResponseEntity.ok(List.of()); // 빈 리스트 반환
            }
            System.out.println("조회된 좌석 수: " + seatStatusList.size());
            return ResponseEntity.ok(seatStatusList); // 200 OK 와 함께 좌석 목록 반환
        } catch (Exception e) {
            System.err.println("사용자용 좌석 목록 조회 중 오류 발생: " + e.getMessage());
            e.printStackTrace(); // 개발 중 상세 에러 확인
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "좌석 정보를 불러오는 중 오류가 발생했습니다."));
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