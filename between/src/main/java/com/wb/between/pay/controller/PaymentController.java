package com.wb.between.pay.controller;

import com.wb.between.pay.dto.KakaoPayApproveResponseDto;
import com.wb.between.pay.dto.KakaoPayReadyRequestDto;
import com.wb.between.pay.dto.KakaoPayReadyResponseDto;
import com.wb.between.pay.service.KakaoPayService;
import com.wb.between.pay.service.PaymentService;
import com.wb.between.reservation.reserve.domain.Reservation;
import com.wb.between.reservation.reserve.dto.ReservationRequestDto;
import com.wb.between.reservation.reserve.service.ReservationService;
import com.wb.between.user.domain.User;
import com.wb.between.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payment/kakao") // 카카오페이 관련 API 경로
@CrossOrigin(origins = "http://localhost:8080")
public class PaymentController {

    @Autowired
    private KakaoPayService kakaoPayService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReservationService reservationService;


    /**
     * 카카오페이 결제 준비 요청
     */
    @PostMapping("/ready")
    public ResponseEntity<?> readyToKakaoPay(
            @RequestBody KakaoPayReadyRequestDto requestDto, // 프론트에서 받은 데이터
            @AuthenticationPrincipal UserDetails userDetails, // 사용자 정보 가져오기 (Spring Security)
            HttpSession session // <<<--- HttpSession 객체 주입 받기
    ) {

        String partnerOrderId = null;
        Reservation pendingReservation = null;

        try {
             String username = userDetails.getUsername();

            User user = userRepository.findByEmail(username) // 이메일로 사용자 조회
                    .orElseThrow(() -> new UsernameNotFoundException("예약 서비스에서 사용자를 찾을 수 없습니다: " + username));
            Long userNo = user.getUserNo(); // User Entity에서 userNo 가져오기 (getUserNo() 메소드 필요)
            String partnerUserId = String.valueOf(userNo);
            if (partnerUserId == null) {
                throw new IllegalStateException("사용자 번호(userNo)를 가져올 수 없습니다.");
            }

            ReservationRequestDto reserveDto = new ReservationRequestDto();
            reserveDto.setItemId(requestDto.getItemId()); // Kakao DTO에서 값 가져오기
            // reserveDto.setItemType(...); // 필요하다면 itemType도 설정
            reserveDto.setPlanType(requestDto.getPlanType());
            reserveDto.setReservationDate(requestDto.getReservationDate());
            reserveDto.setSelectedTimes(requestDto.getSelectedTimes());
            reserveDto.setCouponId(requestDto.getCouponId());
            reserveDto.setUserId(Long.parseLong(partnerUserId)); // 서비스는 Long 타입 userId 기대
            
            // 임직원이면 카카오페이 생략하게 처리
            pendingReservation = reservationService.createReservationWithLock(reserveDto, username);
            Long resNo = pendingReservation.getResNo(); // 생성된 예약 번호 가져오기
            if (resNo == null) {
                throw new IllegalStateException("예약 정보 저장 후 예약 번호를 가져올 수 없습니다.");
            }
            boolean reservationConfirmed = Boolean.TRUE.equals(pendingReservation.getResStatus());
            int finalAmount = Integer.parseInt(pendingReservation.getTotalPrice());
            
            if(reservationConfirmed && finalAmount == 0){
                System.out.println("임직원 및 결제 0원 처리 실행");
                try{
                    paymentService.zeroPricePayment(pendingReservation);
                } catch (Exception exception){
                    System.err.println("!!! 0원 결제 기록 저장 실패 ResNo: " + resNo + ", Error: " + exception.getMessage());
                }

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "0원 결제 (임직원 결제) 처리가 완료되었습니다.");
                response.put("reservationId", resNo);
                response.put("paymentSkipped", true); // 결제 건너뜀
                return ResponseEntity.ok(response);

            } else if (reservationConfirmed && finalAmount > 0){
                System.err.println("[Controller] 오류: 예약은 확정 상태이나 금액이 0보다 큽니다. ResNo: " + resNo);
                throw new IllegalStateException("잘못된 예약 상태입니다(확정/금액있음).");
            } else {
                // 주문 고유번호 생성
                partnerOrderId = "RESERVE_" + resNo + "_" + System.currentTimeMillis();


                System.out.printf("결제 준비 시작: 주문번호=%s, 사용자ID=%s, 금액=%d%n",
                        partnerOrderId, partnerUserId, requestDto.getTotalAmount());

                // === 2. KakaoPayService 호출 ===
                KakaoPayReadyResponseDto readyResponse = kakaoPayService.readyPayment(requestDto, partnerOrderId, partnerUserId);
                // -----------------------------

                // === 3. 중요 정보 세션에 저장 (approve 단계에서 사용) ===
                session.setAttribute("kakaoTid", readyResponse.getTid());
                session.setAttribute("kakaoPartnerOrderId", partnerOrderId);
                session.setAttribute("kakaoPartnerUserId", partnerUserId);
                System.out.printf("세션 저장: tid=%s, orderId=%s, userId=%s%n", readyResponse.getTid(), partnerOrderId, partnerUserId);
                // =============================================

                // 4. 성공 응답 (리다이렉트 URL 포함) 프론트엔드로 전달
                System.out.println("카카오페이 준비 성공, 리다이렉트 URL 전달: " + readyResponse.getNextRedirectPcUrl());
                return ResponseEntity.ok(readyResponse);
            }
        } catch (Exception e) {
            System.err.println("카카오페이 준비 실패: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "카카오페이 결제 준비 중 오류가 발생했습니다."));
        }
    }

    /**
     * 카카오페이 결제 성공 리다이렉트 처리 (승인 API 호출)
     * 카카오에서 여기로 GET 요청 보냄 (pg_token 포함)
     * @param pgToken 카카오에서 전달하는 pg_token
     * @return 최종 예약 완료/실패 페이지로 리다이렉트
     */
    @GetMapping("/approve") // 또는 application.properties 등에서 설정한 경로 (예: /payment/success)
    public RedirectView approveKakaoPayment(
            @RequestParam("pg_token") String pgToken,
            HttpSession session // 세션 사용 위해 파라미터 추가
    ) {
        // 기본 리다이렉트 URL은 실패 페이지로 설정
        String finalRedirectUrl = "/payment-fail.html"; // !!! 실제 실패 페이지 경로로 수정 !!!
        String partnerOrderId = null; // try 블록 밖에서도 사용하기 위해 미리 선언

        try {
            System.out.println("카카오페이 승인 요청 받음, pg_token: " + pgToken);

            // --- 1. 세션 등에서 결제 준비 시 저장했던 정보 가져오기 ---
            String tid = (String) session.getAttribute("kakaoTid");
            partnerOrderId = (String) session.getAttribute("kakaoPartnerOrderId"); // 변수에 할당
            String partnerUserId = (String) session.getAttribute("kakaoPartnerUserId");


            // 사용 후 세션 정보 즉시 삭제 (재사용 및 보안 위험 방지)
            session.removeAttribute("kakaoTid");
            session.removeAttribute("kakaoPartnerOrderId");
            session.removeAttribute("kakaoPartnerUserId");

            // 필수 정보 누락 시 에러 처리
          /*  if (tid == null || partnerOrderId == null || partnerUserId == null) {
                System.err.println("세션 정보 누락! tid, orderId, userId 중 하나 이상 없음");
                throw new IllegalStateException("결제 승인에 필요한 정보가 없습니다. 처음부터 다시 시도해주세요.");
            }
            System.out.printf("세션에서 로드 성공: tid=%s, orderId=%s, userId=%s%n", tid, partnerOrderId, partnerUserId);*/
            // ----------------------------------------------------

        // --- 2. KakaoPayService를 통해 결제 승인 API 호출 ---
            // 이 메소드는 KakaoPayApproveResponseDto 를 반환한다고 가정
            KakaoPayApproveResponseDto approveResponse = kakaoPayService.approvePayment(tid, partnerOrderId, partnerUserId, pgToken);
            // ------------------------------------------------

            // --- 3. 결제 결과 검증 및 DB 처리 ---
            // PaymentService 호출하여 결제 정보 저장 및 예약 상태 업데이트
            // 이 메소드는 성공 시 업데이트된 Reservation 객체 또는 boolean 등을 반환한다고 가정
            // !!! paymentService.recordPaymentAndUpdateReservation 메소드 구현 필요 !!!
            paymentService.recordPaymentAndUpdateReservation(approveResponse, partnerOrderId, partnerUserId);
            // -----------------------------------

            // --- !!! 4. 모든 처리가 성공했을 때만 성공 URL 설정 (주석 해제!) !!! ---
            finalRedirectUrl = "/payment-success?orderId=" + partnerOrderId;
            System.out.println("결제 최종 승인 및 DB 처리 완료.");
            // -------------------------------------------------------------

        } catch (Exception e) {
            // try 블록 내에서 발생한 모든 예외 처리
            System.err.println("카카오페이 승인 또는 DB 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace(); // 개발 중 에러 확인 위해 스택 트레이스 출력
            // 필요시 에러 종류에 따라 다른 실패 페이지나 파라미터 추가 가능
            finalRedirectUrl = "/payment-fail.html?error=" + e.getMessage(); // 예: 에러 메시지 전달
        }

        System.out.println("최종 리다이렉트 URL: " + finalRedirectUrl);
        return new RedirectView(finalRedirectUrl); // 계산된 최종 URL로 사용자 브라우저 리다이렉트
    }

    /**
     * 카카오페이 결제 취소 처리
     */
    @GetMapping("/cancel")
    public RedirectView cancelKakaoPayment(/* HttpSession session */) {
        System.out.println("카카오페이 결제 취소됨");
        // 필요한 경우 예약 상태 변경 (예: PENDING -> CANCELED)
        // 임시 저장 정보 삭제 (세션 등)
        return new RedirectView("/payment-cancel"); // 취소 안내 페이지로 리다이렉트
    }

    /**
     * 카카오페이 결제 실패 처리
     */
    @GetMapping("/fail")
    public RedirectView failKakaoPayment(/* HttpSession session */) {
        System.out.println("카카오페이 결제 실패");
        // 필요한 경우 예약 상태 변경 (예: PENDING -> FAILED)
        // 임시 저장 정보 삭제 (세션 등)
        return new RedirectView("/payment-fail"); // 실패 안내 페이지로 리다이렉트
    }
}