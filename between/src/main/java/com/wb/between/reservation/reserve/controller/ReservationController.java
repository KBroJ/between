package com.wb.between.reservation.reserve.controller;

import com.wb.between.pay.domain.Payment;
import com.wb.between.pay.repository.PaymentRepository;
import com.wb.between.reservation.reserve.domain.Reservation;
import com.wb.between.reservation.reserve.repository.ReservationRepository;
import com.wb.between.reservation.seat.domain.Seat;
import com.wb.between.reservation.seat.repository.SeatRepository;
import com.wb.between.user.domain.User;
import com.wb.between.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class ReservationController {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeatRepository seatRepository;

    private static final LocalTime OPEN_TIME = LocalTime.of(9, 0);  // 운영 시작 시간
    private static final LocalTime CLOSE_TIME = LocalTime.of(22, 0); // 운영 종료 시간

    /**
     * 좌석 예약 페이지를 보여주는 메소드.
     * 예약 변경 모드 파라미터가 있으면 기존 예약 정보를 조회하여 모델에 추가합니다.
     *
     * @param mode    URL 파라미터 "mode" (값이 "modify"일 때 변경 모드)
     * @param resNo   URL 파라미터 "resNo" (변경할 예약 번호)
     * @param userDetails 현재 로그인한 사용자 정보 (Spring Security)
     * @param model   Thymeleaf 템플릿으로 전달할 데이터 모델
     * @return 보여줄 Thymeleaf 템플릿 이름 (예: "reservation")
     */
    @GetMapping("/reservation")
    public String showReservationPage(
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) Long resNo,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model
    ) {
        boolean isModificationMode = false;
        Map<String, Object> originalReservationData = null; // 프론트 JS 전달용 Map

        // 1. 로그인 상태 확인 (Spring Security가 처리)
        if (userDetails == null) {
            System.out.println("로그인되지 않은 사용자 접근 시도 -> 로그인 페이지로 리다이렉트");
            return "redirect:/login";
        }

        // 2. 사용자 ID 추출
        Long currentUserId;
        String currentUserAuth = null;
        try {
            String username = userDetails.getUsername();
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("DB 사용자 정보 없음: " + username));
            currentUserId = user.getUserNo();
            if (currentUserId == null) throw new IllegalStateException("userNo 없음");
            currentUserAuth = user.getAuthCd();
        } catch (Exception e) {
            System.err.println("사용자 ID 처리 실패: " + e.getMessage());
            model.addAttribute("errorMessage", "사용자 정보 처리 오류");
            return "error/custom-error";
        }

        // 3. 예약 변경 모드 처리
        if ("modify".equalsIgnoreCase(mode) && resNo != null) {
            System.out.println("[Controller] 예약 변경 모드 진입 시도: resNo=" + resNo);
            try {
                // 4. 원본 예약 정보 조회
                Reservation reservation = reservationRepository.findById(resNo)
                        .orElseThrow(() -> new EntityNotFoundException("원본 예약 정보 없음: " + resNo));

                // 5. 예약 소유권 확인
                if (!reservation.getUserNo().equals(currentUserId)) {
                    System.err.println("예약 변경 권한 없음: 요청자=" + currentUserId + ", 예약자=" + reservation.getUserNo());
                    model.addAttribute("errorMessage", "본인의 예약만 변경할 수 있습니다.");
                    return "error/custom-error";
                }

                // --- !!! 좌석 이름 조회 추가 !!! ---
                String seatName = "좌석 이름"; // 기본값
                try {
                    seatName = seatRepository.findById(reservation.getSeatNo()) // 예약된 seatNo로 좌석 조회
                            .map(Seat::getSeatNm) // 좌석 이름(seatNm) 가져오기
                            .orElse("삭제된 좌석?"); // 좌석 정보 없으면 표시할 텍스트
                } catch (Exception seatEx) {
                    System.err.println("좌석 이름 조회 중 오류: " + seatEx.getMessage());
                }

                // 6. 변경 모드 활성화 및 프론트 전달 데이터 준비
                isModificationMode = true;
                originalReservationData = new HashMap<>();
                originalReservationData.put("resNo", reservation.getResNo());
                originalReservationData.put("itemId", reservation.getSeatNo()); // 좌석 번호 전달
                originalReservationData.put("seatName", seatName); // 좌석 번호 전달


                String planType = reservation.getPlanType();
                if (planType == null || planType.isBlank()) {
                    System.err.println("!!! CRITICAL: Reservation resNo=" + resNo + " has missing or blank planType! !!!");
                    throw new IllegalStateException("DB에 저장된 예약 정보(planType)가 올바르지 않습니다.");
                }
                originalReservationData.put("planType", planType);
                System.out.println("DB에서 가져온 planType: " + planType);
                // -------------------------------------------------------

                originalReservationData.put("reservationDate", reservation.getResStart().toLocalDate().toString()); // 시작일

                // 시간제일 경우 시간 목록 추출
                if ("HOURLY".equals(planType)) {
                    originalReservationData.put("selectedTimes", extractOriginalTimes(reservation));
                } else {
                    originalReservationData.put("selectedTimes", List.of());
                }
                System.out.println("[Controller] 원본 예약 정보 준비 완료: " + originalReservationData);

            } catch (Exception e) {
                // DB 조회 실패 또는 권한 없음 외 다른 예외 처리
                System.err.println("예약 변경 모드 진입 중 오류: " + e.getMessage());
                e.printStackTrace(); // 개발 중 상세 에러 확인
                model.addAttribute("errorMessage", "예약 정보를 불러오는 중 오류가 발생했습니다.");
                isModificationMode = false; // 안전하게 신규 모드로
                originalReservationData = null;
                // return "error"; // 또는 신규 예약으로 계속 진행
            }
        } // 변경 모드 처리 끝

        // 7. 모델에 최종 데이터 추가
        model.addAttribute("isModificationMode", isModificationMode);
        model.addAttribute("originalReservation", originalReservationData); // 변경 모드 아니면 null
        model.addAttribute("currentUserId", currentUserId);
        model.addAttribute("currentUserAuth", currentUserAuth);

        System.out.println("isModificationMode = " + model.getAttribute("isModificationMode"));
        System.out.println("originalReservation = " + model.getAttribute("originalReservation")); // 이 Map 내용 확인!

        return "reservation/reservation";
    }

    // 시간 목록 추출 헬퍼 메소드
    private List<String> extractOriginalTimes(Reservation reservation){
        List<String> originalTimes = new ArrayList<>();
        // resStart, resEnd가 null이 아닌지 확인 후 진행
        if (reservation.getResStart() == null || reservation.getResEnd() == null) {
            System.err.println("Warning: Reservation resNo=" + reservation.getResNo() + " has null start or end time.");
            return originalTimes; // 빈 리스트 반환
        }
        LocalDateTime current = reservation.getResStart();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        while (current.isBefore(reservation.getResEnd())) { // 종료 시간 직전까지만
            originalTimes.add(current.toLocalTime().format(timeFormatter));
            current = current.plusHours(1);
        }
        return originalTimes;
    }
}