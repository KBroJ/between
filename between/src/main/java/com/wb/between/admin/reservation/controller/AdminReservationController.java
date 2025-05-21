package com.wb.between.admin.reservation.controller;

import com.wb.between.admin.reservation.dto.*;
import com.wb.between.admin.reservation.service.AdminReservationService;
import com.wb.between.admin.user.service.AdminUserService;
import com.wb.between.reservation.reserve.domain.Reservation;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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

    @GetMapping("/reservationList/{resNo}")
    public String reservationDetailPage(@PathVariable Long resNo, Model model) {
        log.info("관리자 - 예약 상세 정보 조회 요청. resNo: {}", resNo);

        try {
            // 예약 상세 정보 조회
            ReservationDetailDto reservationDetail = adminReservationService.getReservationDetail(resNo);
            // 모든 좌석 정보 가져오기
            List<SeatDto> allSeats = adminReservationService.getAllSeatsForFilter();

            model.addAttribute("reservationDetail", reservationDetail);
            model.addAttribute("allSeats", allSeats);

            return "admin/reservation/reservation-detail";

        } catch (EntityNotFoundException e) {
            log.warn("요청한 예약 정보를 찾을 수 없습니다. resNo: {}", resNo, e);
            model.addAttribute("errorMessage", e.getMessage());
            // TODO: 적절한 404 에러 페이지 또는 목록 페이지로 리다이렉션
            return "error/404"; // 예시 404 페이지
        } catch (Exception e) {
            log.error("예약 상세 정보 조회 중 오류 발생. resNo: {}", resNo, e);
            model.addAttribute("errorMessage", "예약 정보를 불러오는 중 오류가 발생했습니다.");
            // TODO: 적절한 500 에러 페이지
            return "error/500"; // 예시 500 페이지
        }
    }


    /**
     * 관리자에 의한 예약 정보 수정 (HTML Form 제출 방식)
     */
    @PostMapping("/reserve/{resNo}/update")
    public ResponseEntity<?> updateReservationByAdmin(
            @PathVariable Long resNo,
            @RequestParam("seatNo") Long seatNo,
            @RequestParam("resStart") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime resStart,
            @RequestParam("resEnd") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime resEnd,
            @RequestParam("planType") String planType,
            @RequestParam("moReason") String moReason,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.error("AdminReservationController|updateReservationByAdmin|관리자 예약 수정 시작 =========================>");
        log.error("AdminReservationController|updateReservationByAdmin|userDetails : {}", userDetails);
        log.error("AdminReservationController|updateReservationByAdmin|수정권한 체크 : {}", isAdmin(userDetails));

    /*
        // 권한 확인
        if (!isAdmin(userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "수정 권한이 없습니다."));
        }
    */
        try {
            ReservationReqDto adminUpdateDto = new ReservationReqDto();
            adminUpdateDto.setSeatNo(seatNo);
            adminUpdateDto.setResStart(resStart);
            adminUpdateDto.setResEnd(resEnd);
            adminUpdateDto.setPlanType(planType);
            adminUpdateDto.setMoReason(moReason);

            Reservation updatedReservation = adminReservationService.updateReservationByAdmin(resNo, adminUpdateDto, userDetails.getUsername());

            // Ajax로 응답을 받아 처리하고 싶다면 JSON 응답, 그렇지 않고 일반 form submit 후 리다이렉션이라면 다른 방식
            // 여기서는 JSON 응답 후 프론트에서 처리한다고 가정
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "예약이 성공적으로 변경되었습니다.");
            response.put("reservationId", updatedReservation.getResNo());
            // response.put("redirectUrl", "/admin/reservationDetail/" + updatedReservation.getResNo()); // 프론트 리다이렉션용 URL
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        } catch (RuntimeException e) { // 유효성, 중복 등
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("관리자 예약 수정 중 예기치 않은 시스템 오류 발생 - resNo: {}", resNo, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "예기치 않은 오류로 예약 변경에 실패했습니다."));
        }
    }

    /**
     * 관리자에 의한 예약 취소 (JSON 요청 방식)
     */
    @PostMapping("/reserve/{resNo}/cancel")
    public ResponseEntity<?> cancelReservationByAdmin(
            @PathVariable Long resNo,
            @RequestBody ReservationReqDto cancelDto, // JSON 요청 본문
            @AuthenticationPrincipal UserDetails userDetails) {

        log.error("AdminReservationController|cancelReservationByAdmin|관리자 예약 취소 시작 =========================>");
        log.error("AdminReservationController|updateReservationByAdmin|userDetails : {}", userDetails);
        log.error("AdminReservationController|updateReservationByAdmin|수정권한 체크 : {}", isAdmin(userDetails));

    /*
        // 권한 확인
        if (!isAdmin(userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "취소 권한이 없습니다."));
        }
    */
        try {
            adminReservationService.cancelReservationByAdmin(resNo, cancelDto, userDetails.getUsername());
            return ResponseEntity.ok(Map.of("success", true, "message", "예약이 성공적으로 취소되었습니다."));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalStateException e) { // 이미 취소되었거나 취소 불가 상태
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (RuntimeException e) { // 카카오페이 취소 실패 등 서비스 내부 오류
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "예약 취소 처리 중 오류: " + e.getMessage()));
        } catch (Exception e) {
            log.error("관리자 예약 취소 중 예기치 않은 시스템 오류 발생 - resNo: {}", resNo, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "예기치 않은 오류로 예약 취소에 실패했습니다."));
        }
    }

    // 관리자 권한 확인을 위한 헬퍼 메소드 (실제 구현은 Spring Security 설정에 따름)
    private boolean isAdmin(UserDetails userDetails) {
        if (userDetails == null) return false;

        return userDetails.getAuthorities().stream()
//                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("관리자"));
    }

}
