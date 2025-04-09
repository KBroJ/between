package com.wb.between.reservation.seat.contorller;


import com.wb.between.reservation.seat.dto.TimeDto;
import com.wb.between.reservation.seat.service.TimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:8080") // !!! 중요: 실제 프론트엔드 주소로 변경 !!!
public class TimeController {

    @Autowired
    private TimeService timeService;

    /**
     * 특정 좌석/날짜의 예약 가능 시간 목록 조회 API
     */
    @GetMapping("/times")
    public ResponseEntity<?> getAvailableTimes(
            // branchId 파라미터 제거됨
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String seatId) { // 좌석(또는 룸) ID

        try {
            // 서비스 호출 시 branchId 제거됨
            List<TimeDto> timeSlots = timeService.getAvailableTimesWithStatus(date, seatId);
            return ResponseEntity.ok(timeSlots);
        } catch (Exception e) {
            System.err.println("Error fetching available times: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("예약 가능 시간 조회 중 오류가 발생했습니다.");
        }
    }
}