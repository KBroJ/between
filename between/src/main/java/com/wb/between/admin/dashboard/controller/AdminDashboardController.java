package com.wb.between.admin.dashboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @GetMapping
    public String redirectToAdminDashboard() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String getDashboard(Model model) {
        Map<String, Object> dashboardData = getSampleDashboardData();
        model.addAttribute("dashboardData", dashboardData);
        return "admin/dashboard/dashboard";
    }

    // 샘플 데이터 생성 메서드 (실제로는 DB 등에서 조회)
    private Map<String, Object> getSampleDashboardData() {
        Map<String, Object> data = new HashMap<>();

        // KPI 데이터
        Map<String, Object> kpis = new HashMap<>();
        Map<String, Object> occupancy = new HashMap<>();
        occupancy.put("current", 75); // %
        occupancy.put("totalSeats", 100);
        occupancy.put("usedSeats", 75);
        kpis.put("occupancy", occupancy);
        kpis.put("newBookingsToday", 5);
        kpis.put("revenueToday", 125000);
        kpis.put("pendingInquiries", 3);
        data.put("kpis", kpis);

        // 최근 예약 목록
        List<Map<String, Object>> recentBookings = Arrays.asList(
                Map.of("id", 1L, "userName", "홍길동", "spaceName", "회의실 A", "time", "14:00-16:00", "status", "확정"),
                Map.of("id", 2L, "userName", "김철수", "spaceName", "1인실 B", "time", "09:00-18:00", "status", "확정"),
                Map.of("id", 3L, "userName", "이영희", "spaceName", "오픈데스크", "time", "10:00-12:00", "status", "대기"),
                Map.of("id", 4L, "userName", "박보검", "spaceName", "세미나실", "time", "13:00-17:00", "status", "확정")
        );
        data.put("recentBookings", recentBookings);

        // 빠른 실행 링크
        List<Map<String, String>> quickLinks = Arrays.asList(
                Map.of("title", "신규 예약 등록", "href", "/admin/bookings/new"),
                Map.of("title", "회원 관리", "href", "/admin/users"),
                Map.of("title", "공간 관리", "href", "/admin/spaces"),
                Map.of("title", "공지사항 등록", "href", "/admin/notices/new")
        );
        data.put("quickLinks", quickLinks);

        // 알림 (만료 예정 계약 등)
        List<Map<String, Object>> alerts = Arrays.asList(
                Map.of("id", 101L, "message", "'박보검'님 계약 만료 D-5", "userContractId", 50, "type", "warning"),
                Map.of("id", 102L, "message", "'송혜교'님 계약 만료 D-7", "userContractId", 52, "type", "warning"),
                Map.of("id", 103L, "message", "새로운 회원 가입 승인 대기", "userId", 201, "type", "info")
        );
        data.put("alerts", alerts);

        // 간단한 차트 데이터 (실제 차트 라이브러리 연동 필요)
        Map<String, Object> chartData = new HashMap<>();
        chartData.put("title", "주간 예약 추이");
        chartData.put("labels", Arrays.asList("월", "화", "수", "목", "금", "토", "일"));
        chartData.put("data", Arrays.asList(12, 19, 3, 5, 2, 3, 7));
        data.put("sampleChart", chartData);

        data.put("todayDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")));

        return data;
    }
}
