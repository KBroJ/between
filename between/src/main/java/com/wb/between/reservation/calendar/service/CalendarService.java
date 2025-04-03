package com.wb.between.reservation.calendar.service;


import com.wb.between.reservation.calendar.dto.CalendarDto;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Service
public class CalendarService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // "YYYY-MM-DD" format

    /**
     * 특정 연도, 월, 지점 ID에 대한 달력 데이터를 생성합니다.
     *
     * @param year     조회할 연도
     * @param month    조회할 월 (1-12)
     * @param branchId (선택) 지점 ID (예약 가능 여부 판별에 사용될 수 있음)
     * @return CalendarResponse DTO
     */
    public CalendarDto.CalendarResponse generateCalendar(int year, int month, String branchId) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate today = LocalDate.now(); // 서버의 현재 날짜

        // 1. 해당 월의 시작일과 마지막일 계산
        LocalDate firstDayOfMonth = yearMonth.atDay(1);
        LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();

        // 2. 달력의 시작일 계산 (첫 주의 일요일)
        // DayOfWeek.SUNDAY 는 7, DayOfWeek.MONDAY 는 1
        LocalDate startDayOfCalendar = firstDayOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

        // 3. 달력 데이터 생성 (일반적으로 6주 분량)
        List<CalendarDto.CalendarWeek> weeks = new ArrayList<>();
        LocalDate currentDayPointer = startDayOfCalendar;

        for (int i = 0; i < 6; i++) { // 최대 6주까지 표시
            List<CalendarDto.CalendarDay> daysInWeek = new ArrayList<>();
            for (int j = 0; j < 7; j++) { // 일요일부터 토요일까지 (7일)
                boolean isCurrentMonth = YearMonth.from(currentDayPointer).equals(yearMonth);
                boolean isTodayDate = currentDayPointer.equals(today);

                // 예약 가능 여부 판별 (가장 중요한 로직)
                // 조건 1: 오늘 또는 미래의 날짜여야 함
                // 조건 2: 해당 지점/날짜가 예약 가능한지 확인 (DB 조회 등 필요)
                boolean isPast = currentDayPointer.isBefore(today); // 오늘보다 이전 날짜인가?
                boolean isAvailable = checkAvailability(branchId, currentDayPointer); // 이건 항상 true
                boolean isSelectableDate = !isPast && isAvailable; // 오늘과 미래 날짜는 true가 되어야 함!

                // 로그 추가
                if (isTodayDate || !isPast) { // 오늘 또는 미래 날짜일 경우 로그 출력
                    System.out.printf("Backend Check - Date: %s, isToday: %s, isPast: %s, isAvailable: %s, isSelectableDate: %s%n",
                            currentDayPointer.format(DATE_FORMATTER), isTodayDate, isPast, isAvailable, isSelectableDate);
                }

                // isSelectableDate 계산 직후 로그 추가
                System.out.printf(">>> [Service Check] Date: %s | today: %s | isPast: %s | isAvailable: %s | isSelectableDate: %s%n",
                        currentDayPointer, // LocalDate 객체 직접 출력
                        today,             // today 값 확인
                        isPast,
                        isAvailable,
                        isSelectableDate); // 최종 계산 결과

                // CalendarService.generateCalendar 메소드 내 CalendarDay 생성 부분 확인
                daysInWeek.add(new CalendarDto.CalendarDay(
                        currentDayPointer.getDayOfMonth(),
                        currentDayPointer.format(DATE_FORMATTER), // !!! 이 부분이 dateString 값 !!!
                        isCurrentMonth,
                        isTodayDate,
                        isSelectableDate // isSelectableDate 값이 selectable 필드에 매핑됨
                ));
                currentDayPointer = currentDayPointer.plusDays(1); // 다음 날짜로 이동
            }
            weeks.add(new CalendarDto.CalendarWeek(daysInWeek));

            // 만약 현재 날짜 포인터가 해당 월의 마지막 날보다 뒤에 있고, 다음 주의 시작(일요일)이라면,
            // 더 이상 다음 주를 그릴 필요가 없을 수 있음 (최적화 가능)
            if (currentDayPointer.isAfter(lastDayOfMonth) && currentDayPointer.getDayOfWeek() == DayOfWeek.SUNDAY && i >= 3){
                // 이미 4주 이상 그렸고, 다음 시작일이 다음달이면 종료해도 될 수 있음
                // (단, isCurrentMonth 플래그로 프론트에서 비활성화 처리하면 6주 모두 보내도 무방)
                //break;
            }
        }

        return new CalendarDto.CalendarResponse(year, month, weeks);
    }

    /**
     * 특정 지점과 날짜에 대한 예약 가능 여부를 확인하는 메소드 (예시).
     * 실제 구현 시 DB 조회, 외부 API 호출 등 비즈니스 로직이 필요합니다.
     *
     * @param branchId 지점 ID
     * @param date     확인할 날짜
     * @return 예약 가능하면 true, 아니면 false
     */
    private boolean checkAvailability(String branchId, LocalDate date) {
        // --- 실제 예약 가능 로직 구현 영역 ---
        // 예시 1: 특정 요일(예: 일요일)은 항상 예약 불가
        // if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
        //     return false;
        // }

        // 예시 2: 특정 날짜(예: 2025-05-05)는 공휴일이라 예약 불가
        // if (date.equals(LocalDate.of(2025, 5, 5))) {
        //     return false;
        // }

        // 예시 3: 특정 지점(branchId)의 특정 날짜(date)가 DB 상에서 예약 마감되었는지 확인
        // boolean isClosed = reservationRepository.isClosed(branchId, date);
        // if (isClosed) {
        //     return false;
        // }

        // 위 조건들에 해당하지 않으면 기본적으로 예약 가능하다고 가정
        // System.out.println("Checking availability for Branch: " + branchId + ", Date: " + date); // 디버깅 로그
        return true;
    }
}