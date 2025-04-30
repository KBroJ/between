package com.wb.between.admin.reservation.controller;

import com.wb.between.admin.reservation.dto.ReservationFilterParamsDto;
import com.wb.between.admin.reservation.dto.ReservationListDto;
import com.wb.between.admin.reservation.dto.SeatDto;
import com.wb.between.admin.reservation.service.AdminReservationService;
import com.wb.between.admin.user.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor // final 필드(AdminReservationService)에 대한 생성자 자동 생성 및 주입
@RequestMapping("/admin")
public class AdminReservationController {

    private final AdminReservationService adminReservationService;

    /**
     * 예약 관리 목록 페이지 조회 요청 처리
     * @param filterParams 검색 필터 파라미터 (ModelAttribute로 자동 바인딩)
     * @param pageable 페이징/정렬 파라미터 (기본값: 10개씩, 예약일(resDt) 내림차순)
     * @param model View에 전달할 데이터 모델
     * @return View 논리 이름 (templates 폴더 기준 경로)
     */
    @GetMapping("/reservationList")
    public String reservationListPage(@ModelAttribute ReservationFilterParamsDto filterParams,
                                      @PageableDefault(size = 10, sort = "resDt", direction = Sort.Direction.DESC) Pageable pageable,
                                      Model model) {
        log.info("AdminReservationController|reservationListPage|관리자 - 예약 목록 조회 요청 시작 ==================");

        // 1. 서비스 호출: 필터링/페이징된 예약 목록 조회
        Page<ReservationListDto> reservationPage = adminReservationService.getReservations(filterParams, pageable);

        // 2. 서비스 호출: 필터용 좌석 목록 조회
        List<SeatDto> seats = adminReservationService.getAllSeatsForFilter();

        // 3. Model에 데이터 추가 (View에서 사용)
        model.addAttribute("filterParams", filterParams);       // 검색 조건 유지용 데이터
        model.addAttribute("reservationPage", reservationPage); // 예약 목록 데이터
        model.addAttribute("seats", seats);                     // 좌석 필터 드롭다운용 데이터

        log.info("조회 완료. 총 페이지: {}, 총 예약 수: {}", reservationPage.getTotalPages(), reservationPage.getTotalElements());

        log.info("AdminReservationController|reservationListPage|관리자 - 예약 목록 조회 요청 끝   ==================");

        return "admin/reservation/reservation-list";
    }

/*
    @GetMapping("/reservationList/{resNo}")
    public String reservationDetailPage(@PathVariable Long resNo, Model model) {
        log.info("AdminReservationController|reservationDetailPage|관리자 - 예약 상세 조회 요청 시작 ==================");

        // 1. 서비스 호출: 예약 상세 정보 조회
        ReservationListDto reservationDetail = adminReservationService.getReservationDetail(resNo);

        // 2. Model에 데이터 추가 (View에서 사용)
        model.addAttribute("reservationDetail", reservationDetail); // 예약 상세 정보

        log.info("조회 완료. 예약 번호: {}", resNo);

        log.info("AdminReservationController|reservationDetailPage|관리자 - 예약 상세 조회 요청 끝   ==================");

        return "admin/reservation/reservation-detail";
    }
*/
}
