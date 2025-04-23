package com.wb.between.reservation.reserve.controller;

import com.wb.between.reservation.reserve.domain.Reservation;
import com.wb.between.reservation.reserve.dto.ReservationModificationDetailDto;
import com.wb.between.reservation.reserve.dto.ReservationRequestDto;
import com.wb.between.reservation.reserve.dto.ReservationUpdateRequestDto;
import com.wb.between.reservation.reserve.service.ReservationService;
import com.wb.between.user.domain.User;
import com.wb.between.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations") // 예약 관련 기본 경로
@CrossOrigin(origins = "http://localhost:8080")
public class ReservationAPIController {

    private static final Logger log = LoggerFactory.getLogger(ReservationAPIController.class);

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private UserRepository userRepository;

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

    /**
     * 예약 변경 화면 로딩 시 필요한 기존 예약 상세 정보를 조회하는 API.
     * 프론트엔드 JavaScript(loadReservationForModification)가 호출합니다.
     *
     * @param resNo 조회할 예약 번호
     * @param userDetails 현재 로그인 사용자 정보
     * @return ResponseEntity<ReservationModificationDetailDto> 또는 에러 응답
     */
    @GetMapping("/details/{resNo}")
    public ResponseEntity<?> getReservationDetailsForModificationApi( // 메서드 이름 변경 (View Controller와 구분)
                                                                      @PathVariable("resNo") Long resNo,
                                                                      @AuthenticationPrincipal UserDetails userDetails) {

        log.info("[API] 예약 상세 정보 조회 요청 (변경용) - ResNo: {}, User: {}");

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        String username = userDetails.getUsername();

        try {
            ReservationModificationDetailDto details = reservationService.getReservationDetailsForModification(resNo, username);
            log.info("[API] 예약 상세 정보 조회 성공 (변경용) - ResNo: {}");
            return ResponseEntity.ok(details); // 성공 시 DTO 직접 반환 (JSON 변환됨)

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "해당 예약을 찾을 수 없습니다."));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "예약 정보를 조회할 권한이 없습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "예약 정보 조회 중 오류 발생"));
        }
    }

    /**
     * 기존 예약을 변경하는 API
     * @param resNo 변경할 예약 번호 (URL 경로 변수)
     * @param modificationDto 변경할 내용 DTO (요청 본문)
     * @param userDetails 현재 로그인 사용자 정보
     * @return 성공 시 업데이트된 정보 또는 메시지, 실패 시 에러 메시지
     */
    @PutMapping("/{resNo}") // HTTP PUT 메소드 사용, 경로 변수로 예약 번호 받음
    public ResponseEntity<?> updateReservation(
            @PathVariable Long resNo, // URL 경로의 {resNo} 값을 Long 타입으로 받음
            @RequestBody ReservationUpdateRequestDto modificationDto, // 요청 본문의 JSON을 DTO로 변환
            @AuthenticationPrincipal UserDetails userDetails // 현재 사용자 정보
    ) {


        if (userDetails == null) { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "로그인 필요")); }


        Long currentUserId;
        try {
            String username = userDetails.getUsername();
            User user = userRepository.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("DB 사용자 정보 없음: " + username));
            currentUserId = user.getUserNo(); // User Entity의 getUserNo() 사용
            if (currentUserId == null) throw new IllegalStateException("userNo 없음");
            System.out.println("[Controller Update] 사용자 정보 확인: userNo=" + currentUserId);
        } catch (Exception e) {
            System.err.println("[Controller Update] 사용자 ID 처리 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "사용자 정보 처리 오류"));
        }

        try {
            // 서비스 호출하여 예약 변경 시도
            Reservation updatedReservation = reservationService.updateReservation(resNo, modificationDto, currentUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "예약이 성공적으로 변경되었습니다.");
            response.put("reservationId", updatedReservation.getResNo());
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage())); }
        catch (SecurityException e) { return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", e.getMessage())); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage())); }
        catch (RuntimeException e) { if (e.getMessage().contains("다른 사용자") || e.getMessage().contains("이미 다른 예약")) { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("success", false, "message", e.getMessage())); } return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "예약 변경 중 오류 발생: " + e.getMessage())); }
        catch (Exception e) { e.printStackTrace(); return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "예기치 않은 오류 발생")); }
    }


    // 주문 이름 생성 헬퍼 메소드 (예시)
    private String createOrderName(Reservation reservation) {
        return String.format("좌석 %d 예약", reservation.getSeatNo());
    }


    /**
     * 예약 취소 처리 API
     * @param resNo 취소할 예약 번호 (URL 경로에서 받음)
     * @param userDetails 현재 로그인 사용자 정보
     * @return 처리 결과 (JSON)
     */
    @PostMapping("/{resNo}/cancel") // POST 방식으로 변경 권장
    public ResponseEntity<?> cancelReservation(
            @PathVariable("resNo") Long resNo, // URL 경로의 {resNo} 값을 받음
            @AuthenticationPrincipal UserDetails userDetails) {

        // 사용자 인증 확인
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        Long currentUserId;
        try {

            String username = userDetails.getUsername();
            User user = userRepository.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("..."));
            currentUserId = user.getUserNo();
            if (currentUserId == null) throw new IllegalStateException("userNo 없음");

            reservationService.cancelReservation(resNo, currentUserId); // Service 메소드 호출
            return ResponseEntity.ok(Map.of("success", true, "message", "예약이 성공적으로 취소되었습니다."));

        } catch (EntityNotFoundException | IllegalStateException | SecurityException e) {
            // 예상 가능한 오류 (예약 없음, 취소 불가 상태, 권한 없음 등)
            System.err.println("Reservation cancellation failed (Client Error): " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (RuntimeException e) { // 카카오페이 취소 실패 등 서비스 내부 오류
            System.err.println("Reservation cancellation failed (Runtime): " + e.getMessage());
            // 실제 서비스에서는 오류 유형에 따라 더 구체적인 메시지 또는 상태 코드 반환 가능
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "예약 취소 중 오류가 발생했습니다."));
        } catch (Exception e) { // 그 외 예상치 못한 오류
            System.err.println("Reservation cancellation failed (Unexpected): " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "예기치 않은 오류가 발생했습니다."));
        }
    }

}