package com.wb.between.pay.service;






import com.wb.between.pay.domain.Payment;
import com.wb.between.pay.dto.KakaoPayApproveResponseDto;
import com.wb.between.pay.repository.PaymentRepository;
import com.wb.between.reservation.reserve.domain.Reservation;
import com.wb.between.reservation.reserve.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException; // JPA 예외
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 트랜잭션 사용

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    // 카카오페이 취소 로직이 필요하다면 KakaoPayService 주입
    // @Autowired
    // private KakaoPayService kakaoPayService;

    /**
     * 카카오페이 결제 승인 정보를 받아 Payment 테이블에 저장하고 Reservation 상태를 업데이트합니다.
     *
     * @param approveResponse 카카오페이 승인 응답 DTO
     * @param partnerOrderId  가맹점 주문번호 (resNo 추출 및 검증 위해 필요)
     * @param partnerUserId   가맹점 사용자 ID (검증용)
     * @return 최종 업데이트된 Reservation 객체
     * @throws RuntimeException 검증 실패 또는 DB 오류 시
     */

    @Transactional
    public Reservation recordPaymentAndUpdateReservation(
            KakaoPayApproveResponseDto approveResponse,
            String partnerOrderId,
            String partnerUserId) {
        System.out.println("[PaymentService] DB 저장/업데이트 시작 - 주문번호: " + partnerOrderId);
        Long resNo = null; // 예외 발생 시 로깅 위해 바깥으로 뺌

        try {
            // --- 1. 예약번호 추출 ---
            resNo = extractResNoFromPartnerOrderId(partnerOrderId);
            if (resNo == null) throw new IllegalArgumentException("주문번호 형식 오류: " + partnerOrderId);

            // --- 2. 원본 Reservation 조회 ---
            Long finalResNo = resNo;
            Reservation reservation = reservationRepository.findById(resNo)
                    .orElseThrow(() -> new EntityNotFoundException("예약 정보 없음: " + finalResNo));
            System.out.println("[PaymentService] 원본 예약 조회 완료, ResNo: " + resNo);

            // --- 3. 예약자/금액 검증 ---
            // ... (검증 로직은 이전과 동일) ...
            int approvedAmount = approveResponse.getAmount().getTotal();
            int expectedAmount = Integer.parseInt(reservation.getTotalPrice());
            if (approvedAmount != expectedAmount) {
                System.err.printf("!!! 결제 금액 불일치 !!! 예상: %d, 승인: %d%n", expectedAmount, approvedAmount);
                // !!! 카카오페이 취소 로직 필요 !!!
                throw new RuntimeException(String.format("결제 금액 검증 실패 [%d != %d]", approvedAmount, expectedAmount));
            }
            System.out.println("[PaymentService] 금액 검증 완료: " + approvedAmount);
            // ----------------------------------

            // --- 4. Payment Entity 생성 ---
            Payment payment = Payment.builder()
                    .paymentKey(approveResponse.getAid())
                    .resNo(reservation.getResNo())
                    .payPrice(String.valueOf(approvedAmount))
                    .payStatus("DONE")
                    .payApproveDt(approveResponse.getApprovedAt())
                    .registDt(approveResponse.getCreatedAt())
                    .method(approveResponse.getPaymentMethodType())
                    .payProvider("KAKAO")
                    .errCode("").errMsg("")
                    .build();
            // ---------------------------

            // --- 5. DB 작업 및 상세 로깅 추가 ---
            System.out.println("[PaymentService] Payment 정보 저장 시도...");
            Payment savedPayment = paymentRepository.save(payment); // Save Payment
            System.out.println("[PaymentService] >>> Payment 정보 저장 성공! Key: " + savedPayment.getPaymentKey()); // 성공 로그

            System.out.println("[PaymentService] Reservation 상태 업데이트 시도...");
            reservation.setResStatus(true); // 상태 변경 (Boolean=true 가정)
            Reservation updatedReservation = reservationRepository.save(reservation); // Update Reservation
            System.out.println("[PaymentService] >>> Reservation 상태 업데이트 성공! ResNo: " + updatedReservation.getResNo()); // 성공 로그
            // --- DB 작업 끝 ---

            // 6. 모든 DB 작업 성공 시 결과 반환
            System.out.println("[PaymentService] 모든 DB 작업 성공, 트랜잭션 커밋 예정");
            return updatedReservation;

        } catch (Exception e) {
            System.err.println("[PaymentService] !!! DB 작업 중 심각한 오류 발생 !!! - ResNo: " + resNo + ", Error: " + e.getMessage());
            e.printStackTrace(); // <<<--- 전체 에러 내용 확인 위해 꼭 필요!
            // 에러 발생 시 @Transactional에 의해 롤백됨
            throw new RuntimeException("결제 정보 처리 중 오류 발생 [" + e.getMessage() + "]", e); // 예외 다시 던지기
        }
    }

    /*
    * 임직원 0원 결제 저장 로직
    * */
    @Transactional
    public Payment zeroPricePayment(Reservation reservation){
        System.out.println("임직원 결제 (0원) 결제 로직 실행");

        // 해당 좌석 예약 중복 방지 로직
        Optional<Payment> exisPayment = paymentRepository.findByResNo(reservation.getResNo());
        if(exisPayment.isPresent()){
             System.out.println("이미 해당 예약에 대한 결제 기록 존재. 건너<0xEB><0x9B><0x84>니다.");
             return exisPayment.get();
        }

        // paymeny Entity 생성
        Payment payment = Payment.builder()
                .paymentKey("ZERO_" + reservation.getResNo() + System.currentTimeMillis())
                .resNo(reservation.getResNo())
                .payPrice("0") // 결제 금액 0원
                .payStatus("DONE") // 결제 처리 완료
                .payApproveDt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .registDt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .method("DISCOUNT") // 결제 수단 대신 '할인' 또는 'INTERNAL', 'EMPLOYEE' 등 내부 처리 구분 값
                .payProvider("SYSTEM") // 카카오페이/토스 등이 아닌 시스템 내부 처리
                .errCode("") // 성공 시 빈 값
                .errMsg("")  // 성공 시 빈 값
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        System.out.println("Payment 테이블에 0원 결제 기록 저장 완료: " + savedPayment.getPaymentKey());
        return savedPayment;
    }


    private Long extractResNoFromPartnerOrderId(String partnerOrderId) {
        // !!! 중요: Controller에서 생성한 partnerOrderId 형식과 정확히 일치해야 함 !!!
        if (partnerOrderId != null && partnerOrderId.startsWith("RESERVE_")) { // "RESERVE_" 로 시작하는지 확인
            try {
                // "_" 로 분리하고, 두 번째 부분(index 1)을 Long으로 변환
                String[] parts = partnerOrderId.split("_");
                if (parts.length > 1) {
                    return Long.parseLong(parts[1]); // "RESERVE_" 다음의 숫자 부분
                }
            } catch (Exception e) {
                System.err.println("주문번호에서 예약번호 추출/파싱 중 오류: " + partnerOrderId + ", Error: " + e.getMessage());
            }
        }
        System.err.println("주문번호 형식이 'RESERVE_{resNo}_...' 와 다르거나 추출 불가: " + partnerOrderId);
        return null; // 추출 실패 시 null 반환
    }
}