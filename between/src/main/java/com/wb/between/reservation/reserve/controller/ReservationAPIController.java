package com.wb.between.reservation.reserve.controller;

import com.wb.between.reservation.reserve.domain.Reservation;
import com.wb.between.reservation.reserve.dto.ReservationRequestDto;
import com.wb.between.reservation.reserve.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations") // 예약 관련 기본 경로
@CrossOrigin(origins = "http://localhost:8080") // !!! 프론트엔드 주소 확인 !!!
public class ReservationAPIController {

    @Autowired
    private ReservationService reservationService;

    /**
     * 새로운 예약을 생성하는 API (결제 전 단계)
     * Spring Security를 통해 인증된 사용자 정보를 가져와 사용합니다.
     *
     * @param requestDto 프론트엔드에서 받은 예약 정보 (userId 제외)
     * @param userDetails 현재 로그인된 사용자의 정보 (Spring Security가 주입)
     * @return 성공 시 예약 정보 및 결제 정보, 실패 시 에러 메시지
     */
    @PostMapping
    public ResponseEntity<?> createReservation(
            @RequestBody ReservationRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails // 현재 로그인 사용자 정보 주입
    ) {
        // 1. 사용자 인증 확인
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        try {
            // 2. UserDetails에서 사용자 식별자(여기서는 email/username) 가져오기
            String username = userDetails.getUsername(); // Spring Security의 기본 username (이메일)
            if (username == null || username.isEmpty()) {
                return ResponseEntity.internalServerError()
                        .body(Map.of("success", false, "message", "사용자 식별 정보를 확인할 수 없습니다."));
            }

            // 3. ReservationService 호출 시 username 전달 (userId 직접 전달 안 함)
            //    Service 내부에서 username으로 실제 userNo를 조회하여 사용
            Reservation savedReservation = reservationService.createReservationWithLock(requestDto, username);

            // 4. 성공 응답 생성 (프론트엔드 결제에 필요한 정보 포함)
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "예약 요청이 접수되었습니다. 결제를 진행해주세요.");
            response.put("reservationId", savedReservation.getResNo()); // 생성된 예약 DB ID

            // --- 토스페이먼츠 연동 위한 정보 생성 (예시) ---
            response.put("orderId", "ORD_" + savedReservation.getResNo() + "_" + System.currentTimeMillis()); // 고유 주문 ID
            response.put("orderName", createOrderName(savedReservation)); // 주문 이름
            response.put("amount", Integer.parseInt(savedReservation.getTotalPrice())); // 최종 결제 금액
            response.put("customerKey", String.valueOf(savedReservation.getUserNo())); // 사용자 고유 키
            // ---------------------------------------------

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) { // 유효성 검사 오류 등
            System.err.println("Reservation creation failed (Bad Request): " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (RuntimeException e) { // 예약 불가(락 실패, 중복 예약) 또는 기타 런타임 오류
            System.err.println("Reservation creation failed (Runtime): " + e.getMessage());
            // Service에서 던진 메시지 확인하여 상태 코드 구분
            if (e.getMessage().contains("다른 사용자") || e.getMessage().contains("이미 예약")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("success", false, "message", e.getMessage())); // 409 Conflict
            }
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "예약 처리 중 오류 발생: " + e.getMessage())); // 500 Internal Server Error
        } catch (Exception e) { // 그 외 예상치 못한 오류
            System.err.println("Reservation creation failed (Unexpected): " + e.getMessage());
            e.printStackTrace(); // 개발 중 상세 로그 확인
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "예기치 않은 오류가 발생했습니다."));
        }
    }

    // 주문 이름 생성 헬퍼 메소드 (예시)
    private String createOrderName(Reservation reservation) {
        // 실제로는 Seat 정보 등을 조회해서 더 자세히 만드는 것이 좋음
        return String.format("좌석 %d 예약", reservation.getSeatNo());
    }

    // --- !!! 토스페이먼츠 결제 승인(/success) 및 실패(/fail) 처리 엔드포인트 구현 필요 !!! ---
}