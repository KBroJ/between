package com.wb.between.reservation.calendar.controller;


import com.wb.between.reservation.calendar.dto.CalendarDto;
import com.wb.between.reservation.calendar.service.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;

@RestController
@RequestMapping("/api") // 모든 API 경로 앞에 /api 추가
public class CalendarController {

    @Autowired
    private CalendarService calendarService;

    /**
     * 달력 데이터를 조회하는 API 엔드포인트.
     * 요청 예시: GET /api/calendar?year=2025&month=4&branchId=gangnam
     *
     * @param year     조회할 연도 (필수)
     * @param month    조회할 월 (필수, 1-12)
     * @param branchId 조회할 지점 ID (선택)
     * @return 성공 시 달력 데이터(JSON), 실패 시 에러 응답
     */
    @GetMapping("/calendar")
    public ResponseEntity<?> getCalendarData(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) String branchId) { // branchId는 필수가 아님

        // 입력 값 유효성 검사
        if (month < 1 || month > 12) {
            return ResponseEntity.badRequest().body("Month must be between 1 and 12.");
        }
        if (year < 1900 || year > Year.now().getValue() + 10) { // 너무 과거이거나 먼 미래 방지
            return ResponseEntity.badRequest().body("Invalid year provided.");
        }

        try {
            CalendarDto.CalendarResponse calendarData = calendarService.generateCalendar(year, month , branchId);

            // --- !!! 로그 추가: 반환 직전 데이터 확인 !!! ---
            System.out.println("--- CalendarController: 최종 응답 데이터 확인 ---");
            try {
                // Jackson ObjectMapper를 사용하여 JSON 문자열로 예쁘게 출력 (의존성 필요할 수 있음)
                // import com.fasterxml.jackson.databind.ObjectMapper;
                // import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
                // ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
                // System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(calendarData));

                // 간단하게 첫 주 첫날 데이터 확인
                if (calendarData != null && calendarData.getWeeks() != null && !calendarData.getWeeks().isEmpty() &&
                        calendarData.getWeeks().get(0) != null && calendarData.getWeeks().get(0).getDays() != null && !calendarData.getWeeks().get(0).getDays().isEmpty() &&
                        calendarData.getWeeks().get(0).getDays().get(0) != null) {
                    System.out.println("첫 주의 첫 날 데이터 샘플: " + calendarData.getWeeks().get(0).getDays().get(0)); // 객체 toString()
                    System.out.println("첫 주의 첫 날 selectable 값: " + calendarData.getWeeks().get(0).getDays().get(0).isSelectable()); // Lombok Getter 사용 가정
                } else {
                    System.out.println("데이터 구조 문제 또는 빈 데이터");
                }
            } catch (Exception e) {
                System.out.println("데이터 로깅 중 오류 발생: " + e.getMessage());
            }
            System.out.println("--- 데이터 확인 끝 ---");
            // --- 로그 추가 끝 ---

            return ResponseEntity.ok(calendarData);
        } catch (Exception e) {
            // 로그 기록 등 에러 처리
            System.err.println("Error generating calendar: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Error generating calendar data.");
        }
    }
}