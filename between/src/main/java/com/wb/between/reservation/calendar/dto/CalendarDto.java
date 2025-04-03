package com.wb.between.reservation.calendar.dto; // 패키지 경로는 실제 프로젝트에 맞게 확인해주세요

// Lombok import는 제거합니다.
import java.util.List;

public class CalendarDto {

    /**
     * 개별 날짜 정보를 담는 DTO
     */
    public static class CalendarDay {
        private int dayOfMonth;          // 날짜 (e.g., 1, 2, ..., 31)
        private String dateString;       // 전체 날짜 문자열 ("YYYY-MM-DD")
        private boolean isCurrentMonth;  // 현재 요청된 월에 속하는 날짜인지 여부
        private boolean isToday;         // 오늘 날짜인지 여부
        private boolean selectable;      // 예약 가능한 날짜인지 여부

        // --- 생성자 직접 작성 ---
        public CalendarDay(int dayOfMonth, String dateString, boolean isCurrentMonth, boolean isToday, boolean selectable) {
            this.dayOfMonth = dayOfMonth;
            this.dateString = dateString;
            this.isCurrentMonth = isCurrentMonth;
            this.isToday = isToday;
            this.selectable = selectable; // 혹시 실수로 false나 다른 값 할당?
        }

        // --- Getter 메소드 직접 작성 ---
        public int getDayOfMonth() {
            return dayOfMonth;
        }

        public String getDateString() {
            return dateString;
        }

        public boolean isCurrentMonth() { // boolean getter는 is...()
            return isCurrentMonth;
        }

        public boolean isToday() { // boolean getter는 is...()
            return isToday;
        }

        public boolean isSelectable() { // boolean getter는 is...()
            return this.selectable;
        }

        // --- Setter 메소드 (필요한 경우 직접 작성) ---
        public void setDayOfMonth(int dayOfMonth) {
            this.dayOfMonth = dayOfMonth;
        }

        public void setDateString(String dateString) {
            this.dateString = dateString;
        }

        public void setCurrentMonth(boolean currentMonth) {
            isCurrentMonth = currentMonth;
        }

        public void setToday(boolean today) {
            isToday = today;
        }

        public void setSelectable(boolean selectable) {
            this.selectable = selectable;
        }

        // 디버깅 시 객체 내용을 편하게 보려면 toString() 메소드를 추가하는 것이 좋습니다 (선택 사항)
        @Override
        public String toString() {
            return "CalendarDay{" +
                    "dayOfMonth=" + dayOfMonth +
                    ", dateString='" + dateString + '\'' +
                    ", isCurrentMonth=" + isCurrentMonth +
                    ", isToday=" + isToday +
                    ", selectable=" + selectable +
                    '}';
        }
    }

    /**
     * 한 주의 날짜 목록을 담는 DTO
     */
    public static class CalendarWeek {
        private List<CalendarDay> days;

        // --- 생성자 직접 작성 ---
        public CalendarWeek(List<CalendarDay> days) {
            this.days = days;
        }

        // --- Getter & Setter 직접 작성 ---
        public List<CalendarDay> getDays() {
            return days;
        }

        public void setDays(List<CalendarDay> days) {
            this.days = days;
        }
    }

    /**
     * 최종 달력 API 응답 DTO
     */
    public static class CalendarResponse {
        private int year;
        private int month;
        private List<CalendarWeek> weeks;

        // --- 생성자 직접 작성 ---
        public CalendarResponse(int year, int month, List<CalendarWeek> weeks) {
            this.year = year;
            this.month = month;
            this.weeks = weeks;
        }

        // --- Getter & Setter 직접 작성 ---
        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

        public int getMonth() {
            return month;
        }

        public void setMonth(int month) {
            this.month = month;
        }

        public List<CalendarWeek> getWeeks() {
            return weeks;
        }

        public void setWeeks(List<CalendarWeek> weeks) {
            this.weeks = weeks;
        }
    }
}