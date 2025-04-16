package com.wb.between.reservation.reserve.service;

import com.wb.between.pay.domain.Payment;
import com.wb.between.pay.repository.PaymentRepository;
import com.wb.between.pay.service.KakaoPayService;
import com.wb.between.reservation.reserve.domain.Reservation;
import com.wb.between.reservation.reserve.dto.ReservationModificationDetailDto;
import com.wb.between.reservation.reserve.dto.ReservationRequestDto;
import com.wb.between.reservation.reserve.dto.ReservationUpdateRequestDto;
import com.wb.between.reservation.reserve.repository.ReservationRepository;
import com.wb.between.reservation.seat.domain.Seat;
import com.wb.between.reservation.seat.repository.SeatRepository;
import com.wb.between.user.domain.User;
import com.wb.between.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 중요!

import java.time.Duration; // Redis TTL 설정용
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ReservationService {

    @Autowired
    private StringRedisTemplate redisTemplate; // Redis 사용 위해 주입

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private SeatRepository seatRepository; // 좌석 정보 확인 등에 필요시 주입

    @Autowired
    private UserRepository userRepository; // User 정보 조회 위해 주입

    // @Autowired
    // private CouponService couponService; // 쿠폰 할인 계산 등에 필요시 주입

    @Autowired
    private PaymentRepository paymentRepository; // 결제 정보 조회/수정 위해 추가

    @Autowired
    private KakaoPayService kakaoPayService; // 카카오페이 취소 API 호출 위해 추가

    private static final long LOCK_TIMEOUT_SECONDS = 10; // 락 유지 시간 (초)
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final LocalTime OPEN_TIME = LocalTime.of(9, 0);  // 운영 시간 (설정 필요)
    private static final LocalTime CLOSE_TIME = LocalTime.of(22, 0); // 운영 시간 (설정 필요)

    /**
     * Redis 락을 사용하여 예약을 생성합니다.
     *
     * @param requestDto 예약 요청 정보
     * @param username
     * @return 생성된 Reservation 객체 (상태: PENDING)
     * @throws RuntimeException 예약 불가 시 예외 발생
     */
    @Transactional // DB 저장 작업이 있으므로 트랜잭션 필요
    public Reservation createReservationWithLock(ReservationRequestDto requestDto, String username) {
        // 1. 요청 데이터 유효성 검사 (간단 예시)
        Objects.requireNonNull(requestDto.getItemId(), "좌석 ID는 필수입니다.");
        Objects.requireNonNull(requestDto.getReservationDate(), "예약 날짜는 필수입니다.");
        Objects.requireNonNull(requestDto.getPlanType(), "요금제는 필수입니다.");
        Objects.requireNonNull(username, "사용자 정보(username)는 필수입니다.");
        // --- !!! 사용자 정보 조회 로직 추가 !!! ---
        User user = userRepository.findByEmail(username) // 이메일로 사용자 조회
                .orElseThrow(() -> new UsernameNotFoundException("예약 서비스에서 사용자를 찾을 수 없습니다: " + username));
        Long userNo = user.getUserNo(); // User Entity에서 userNo 가져오기 (getUserNo() 메소드 필요)
        if (userNo == null) {
            throw new IllegalStateException("사용자 번호(userNo)를 가져올 수 없습니다.");
        }

        // 2. 예약 시작/종료 시각 계산
        LocalDate reservationDate = LocalDate.parse(requestDto.getReservationDate());
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;

        switch (requestDto.getPlanType()) {
            case "HOURLY":
                if (requestDto.getSelectedTimes() == null || requestDto.getSelectedTimes().isEmpty()) {
                    throw new IllegalArgumentException("시간제는 예약 시간을 선택해야 합니다.");
                }
                // 선택된 시간 중 가장 빠른 시간과 가장 마지막 시간 + 1시간으로 계산 (연속 사용 가정)
                requestDto.getSelectedTimes().sort(Comparator.naturalOrder());
                LocalTime startTime = LocalTime.parse(requestDto.getSelectedTimes().get(0), TIME_FORMATTER);
                LocalTime lastTime = LocalTime.parse(requestDto.getSelectedTimes().get(requestDto.getSelectedTimes().size() - 1), TIME_FORMATTER);
                startDateTime = reservationDate.atTime(startTime);
                endDateTime = reservationDate.atTime(lastTime.plusHours(1)); // 마지막 시간 + 1시간
                break;
            case "DAILY":
                startDateTime = reservationDate.atTime(OPEN_TIME);
                endDateTime = reservationDate.atTime(CLOSE_TIME); // 당일 종료 시간까지
                break;
            case "MONTHLY":
                startDateTime = reservationDate.atTime(OPEN_TIME); // 시작일 운영 시작 시간
                endDateTime = reservationDate.plusMonths(1).atTime(CLOSE_TIME); // 한 달 뒤 종료 시간
                break;
            default:
                throw new IllegalArgumentException("알 수 없는 요금제 타입입니다.");
        }

        // 3. Redis 락 키 정의 (좌석 + 날짜 + 시간대)
        //    시간대별로 락을 거는 것이 가장 정확하지만, 키가 너무 많아질 수 있음.
        //    여기서는 좌석 + 날짜 단위로 락을 걸고, DB 조회로 시간 중복을 재확인하는 방식 사용.
        String lockKey = String.format("lock:seat:%s:%s", requestDto.getItemId(), requestDto.getReservationDate());
        String lockValue = UUID.randomUUID().toString(); // 락 소유자 식별 위한 값
        Boolean lockAcquired = false;
        try {
            // 4. 락 획득 시도 (setIfAbsent: 키가 없을 때만 true 반환)
            lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(LOCK_TIMEOUT_SECONDS));

            if (lockAcquired == null || !lockAcquired) {
                System.out.println("락 획득 실패: " + lockKey);
                throw new RuntimeException("다른 사용자가 현재 좌석/날짜를 예약 중입니다. 잠시 후 다시 시도해주세요.");
            }
            System.out.println("락 획득 성공: " + lockKey);

            // --- !!! CRITICAL SECTION START (락 확보 상태) !!! ---

            // 5. 예약 가능 여부 DB에서 최종 확인 (중복 예약 방지)
            long overlappingCount = reservationRepository.countOverlappingReservations(
                    requestDto.getItemId(), startDateTime, endDateTime);

            if (overlappingCount > 0) {
                System.out.println("중복 예약 발견됨: " + lockKey);
                throw new RuntimeException("선택하신 시간에 이미 예약이 존재합니다.");
            }
            System.out.println("DB 예약 가능 확인 완료");

            // 6. 가격 재계산
            String basePriceStr = calculateBasePrice(requestDto.getPlanType(), requestDto.getSelectedTimes());
            String discountPriceStr = calculateDiscount(basePriceStr, requestDto.getCouponId());
            String finalPriceStr = calculateFinalPrice(basePriceStr, discountPriceStr);

            // 7. Reservation Entity 생성 및 저장
            Reservation reservation = new Reservation();
            reservation.setUserNo(userNo);
            reservation.setSeatNo(requestDto.getItemId());
            reservation.setTotalPrice(finalPriceStr); // 계산된 최종 가격
            reservation.setResPrice(basePriceStr);    // 할인 전 가격
            reservation.setDcPrice(discountPriceStr); // 할인액
            // reservation.setUserCpNo(...); // 사용된 쿠폰 ID 저장 필요시
            reservation.setResStart(startDateTime);
            reservation.setResEnd(endDateTime);
            // reservation.setPlanType(requestDto.getPlanType()); // Entity에 필드 추가 시
            reservation.setResStatus(null); // 초기 상태: 보류 (결제 전)
            // resDt, moDt는 @CreationTimestamp, @UpdateTimestamp로 자동 관리


            // --- !!! 테스트를 위해 임시로 바로 '확정' 상태로 저장 !!! ---
            // 실제 결제 연동 시에는 null 또는 "PENDING"으로 저장 후, 결제 성공 시 업데이트 필요
            reservation.setResStatus(true); // Boolean 타입일 경우 (true=완료)
            // 또는 reservation.setStatus("CONFIRMED"); // String 타입이고 "CONFIRMED"를 완료 상태로 쓴다면

            Reservation savedReservation = reservationRepository.save(reservation);
            System.out.println("!!! 임시: DB에 예약 정보 저장 성공 (상태: 확정) !!!: " + savedReservation.getResNo());
            /*Reservation savedReservation = reservationRepository.save(reservation);
            System.out.println("DB에 예약 정보 저장 성공 (상태: 보류): " + savedReservation.getResNo());*/

            // --- !!! CRITICAL SECTION END !!! ---

            return savedReservation; // 생성된 예약 정보 반환

        } finally {
            // 8. 락 해제 (내가 획득한 락만 해제)
            if (Boolean.TRUE.equals(lockAcquired)) { // null 체크 포함
                String redisValue = redisTemplate.opsForValue().get(lockKey);
                if (lockValue.equals(redisValue)) { // 내가 설정한 값이 맞는지 확인 후 삭제 (안전장치)
                    redisTemplate.delete(lockKey);
                    System.out.println("락 해제 성공: " + lockKey);
                } else {
                    System.out.println("락 해제 실패: 락 소유자가 다르거나 만료됨 - " + lockKey);
                }
            }
        }
    }

    // --- 가격 계산 헬퍼 메소드 (실제 로직 구현 필요) ---
    private String calculateBasePrice(String planType, List<String> selectedTimes) {
        switch (planType) {
            case "HOURLY": return String.valueOf((selectedTimes != null ? selectedTimes.size() : 0) * 2000);
            case "DAILY": return "10000";
            case "MONTHLY": return "99000";
            default: return "0";
        }
    }
    private String calculateDiscount(String basePriceStr, String couponId) {
        if (couponId == null || couponId.isEmpty()) return "0";
        int basePrice = Integer.parseInt(basePriceStr);
        int discount = 0;
        // !!! 실제 쿠폰 정보 조회 및 할인액 계산 로직 필요 !!!
        // Coupon coupon = couponService.getCoupon(couponId);
        // discount = coupon.calculateDiscount(basePrice);
        if ("D1000".equals(couponId)) discount = 1000; // 임시 로직
        else if ("P10".equals(couponId)) discount = (int) (basePrice * 0.1); // 임시 로직
        return String.valueOf(Math.min(discount, basePrice)); // 할인이 원금 초과 불가
    }
    private String calculateFinalPrice(String basePriceStr, String discountPriceStr) {
        return String.valueOf(Integer.parseInt(basePriceStr) - Integer.parseInt(discountPriceStr));
    }

    /**
     * 예약을 취소하고 관련된 카카오페이 결제를 취소합니다.
     * @param resNo 취소할 예약 번호
     * @param username 요청한 사용자 ID (본인 확인용)
     * @throws RuntimeException 예약 정보를 찾을 수 없거나, 취소 권한이 없거나, 카카오페이 취소 실패 시
     */
    @Transactional // DB 업데이트와 API 호출을 묶어서 처리
    public void cancelReservation(Long resNo, String username) {
        System.out.printf("[Service] 예약 취소 요청 수신 - ResNo: %d, Username: %s%n", resNo, username);


        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("서비스에서 사용자를 찾을 수 없습니다: " + username));
        Long userId = user.getUserNo();

        // 1. 예약 정보 조회 및 유효성 검사
        Reservation reservation = reservationRepository.findById(resNo)
                .orElseThrow(() -> new EntityNotFoundException("취소할 예약 정보를 찾을 수 없습니다: " + resNo));

        // 2. 사용자 권한 확인 (본인 예약만 취소 가능)
        if (!Objects.equals(reservation.getUserNo(), userId)) {
            // 실제 User 객체 비교 또는 권한 비교 로직 필요시 추가
            throw new SecurityException("해당 예약을 취소할 권한이 없습니다.");
        }

        // 3. 이미 취소되었거나 완료되지 않은 예약인지 확인
        // resStatus가 true(완료)일 때만 취소 가능하다고 가정 (정책에 따라 변경)
        if (reservation.getResStatus() == null || !reservation.getResStatus()) {
            throw new IllegalStateException("이미 취소되었거나 완료되지 않은 예약은 취소할 수 없습니다.");
        }

        // 4. 연결된 Payment 정보 조회 (카카오페이 취소에 필요한 정보 가져오기)
        // Payment 테이블에 resNo로 조회하는 기능 필요 (Repository에 findByResNo 추가 가정)
        Optional<Payment> paymentOpt = paymentRepository.findByResNo(resNo);
        if (paymentOpt.isEmpty()) {
            System.err.println("!!! 경고: 예약(ResNo:" + resNo + ")에 연결된 결제 정보를 찾을 수 없습니다. DB 상태만 취소로 변경합니다.");
        }

        Payment payment = paymentOpt.orElse(null); // 없으면 null

        // --- !!! 중요: 카카오페이 결제 취소 로직 !!! ---
        if ("KAKAO".equals(payment.getPayProvider()) && "DONE".equals(payment.getPayStatus())) {
            try {
                String tid = payment.getPaymentKey(); // tid가 저장된 필드 사용
                int cancelAmount = Integer.parseInt(payment.getPayPrice());
                // kakaoPayService.cancelPayment(tid, cancelAmount, 0); // 카카오 취소 API 호출
                System.out.println("카카오페이 결제 취소 API 호출 성공 (가정)");
                payment.setPayStatus("CANCELED"); // Payment 상태 변경
                payment.setPayCanclDt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)); // 시간 기록
                paymentRepository.save(payment);

            } catch (Exception e) {
                System.err.println("!!! 카카오페이 결제 취소 중 오류 발생 !!! - " + e.getMessage());
                e.printStackTrace();
                // 카카오페이 취소 실패 시 어떻게 처리할지 정책 결정 필요
                // 1. DB 롤백 (현재 @Transactional 이므로 자동 롤백됨)
                // 2. 사용자에게 취소 실패 알림 (Controller에서 처리)
                throw new RuntimeException("카카오페이 결제 취소 중 오류가 발생했습니다. 관리자에게 문의하세요.", e);
            }
        } else {
            System.out.println("카카오페이 취소 대상이 아니거나 이미 취소된 결제입니다. DB 예약 상태만 변경합니다.");
        }
        // ------------------------------------------

        // 5. Reservation 상태 '취소'로 업데이트
        reservation.setResStatus(false); // false = 취소 상태로 가정
        reservation.setMoDt(LocalDateTime.now());
        reservationRepository.save(reservation);
        System.out.println("Reservation 상태 업데이트 완료 (취소)");
    }

    /**
     * 예약 변경 화면에 필요한 기존 예약 상세 정보를 조회합니다.
     * @param resNo 예약 번호
     * @param username 요청 사용자 (권한 확인용)
     * @return ReservationModificationDetailDto
     */
    @Transactional(readOnly = true) // 조회 작업이므로 readOnly
    public ReservationModificationDetailDto getReservationDetailsForModification(Long resNo, String username) {
        System.out.printf("[Service] 예약 상세 조회 요청 (변경용) - ResNo: %d, Username: %s%n", resNo, username);

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("서비스에서 사용자를 찾을 수 없습니다: " + username));
        Long userId = user.getUserNo();

        // 1. 예약 정보 조회
        Reservation reservation = reservationRepository.findById(resNo)
                .orElseThrow(() -> new EntityNotFoundException("예약 정보를 찾을 수 없습니다: " + resNo));

        // 2. 권한 확인
        if (!Objects.equals(reservation.getUserNo(), userId)) {
            throw new SecurityException("해당 예약 정보를 조회할 권한이 없습니다.");
        }

        // 3. 좌석 정보 조회
        Seat seat = seatRepository.findById(reservation.getSeatNo())
                .orElseThrow(() -> new EntityNotFoundException("예약된 좌석 정보를 찾을 수 없습니다: " + reservation.getSeatNo()));

        // 4. 시간제일 경우 시간 목록 재구성
        List<String> selectedTimes = new ArrayList<>();
        if (reservation.getResStart() != null && reservation.getResEnd() != null &&
                !reservation.getResStart().toLocalTime().equals(OPEN_TIME) && // 시작/종료가 운영시간과 다르면 시간제로 간주
                !reservation.getResEnd().toLocalTime().equals(CLOSE_TIME))
        {
            LocalDateTime current = reservation.getResStart();
            while(current.isBefore(reservation.getResEnd())) {
                selectedTimes.add(current.format(TIME_FORMATTER));
                current = current.plusHours(1);
            }
        }

        // 5. DTO 생성 및 반환
        return ReservationModificationDetailDto.builder()
                .resNo(reservation.getResNo())
                .planType(determinePlanTypeFromTimes(reservation.getResStart(), reservation.getResEnd())) // 임시 추정
                .reservationDate(reservation.getResStart().toLocalDate().format(DateTimeFormatter.ISO_DATE))
                .seatInfo(ReservationModificationDetailDto.SeatInfo.builder()
                        .id(seat.getSeatNo())
                        .name(seat.getSeatNm())
                        .type(seat.getSeatSort()) // SEAT, ROOM 등
                        .build())
                .selectedTimes(selectedTimes)
                // .couponId(reservation.getUserCpNo())
                .status(reservation.getResStatus() != null && reservation.getResStatus() ? "CONFIRMED" : "CANCELLED") // 상태 문자열로 변환
                .originalTotalPrice(reservation.getTotalPrice()) // 원래 가격
                .build();
    }

    /**
     * 예약 변경 요청을 처리합니다.
     * @param resNo 변경할 원본 예약 번호
     * @param updateDto 변경 요청 정보
     * @param username 요청 사용자
     * @return 처리 결과 Map (success, message, paymentRequired 등 포함)
     */
    @Transactional
    public Map<String, Object> updateReservation(Long resNo, ReservationUpdateRequestDto updateDto, String username) {
        System.out.printf("[Service] 예약 변경 처리 시작 - ResNo: %d, Username: %s%n", resNo, username);
        System.out.println("Update Request DTO: " + updateDto);

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("서비스에서 사용자를 찾을 수 없습니다: " + username));
        Long userId = user.getUserNo();

        // 1. 원본 예약 조회 및 권한/상태 확인
        Reservation originalReservation = reservationRepository.findById(resNo)
                .orElseThrow(() -> new EntityNotFoundException("변경할 예약 정보를 찾을 수 없습니다: " + resNo));

        if (!Objects.equals(originalReservation.getUserNo(), userId)) {
            throw new SecurityException("해당 예약을 변경할 권한이 없습니다.");
        }

        // 취소되었거나 이미 지난 예약은 변경 불가 (정책에 따라 기준 변경 가능)
        if (originalReservation.getResStatus() == null || !originalReservation.getResStatus()) {
            throw new IllegalStateException("취소된 예약은 변경할 수 없습니다.");
        }
        if (originalReservation.getResEnd().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("이미 지난 예약은 변경할 수 없습니다.");
        }

        // 2. 요금제 변경 불가 확인
        String originalPlanType = determinePlanTypeFromTimes(originalReservation.getResStart(), originalReservation.getResEnd());
        if (!Objects.equals(updateDto.getPlanType(), originalPlanType)) {
            throw new IllegalArgumentException(String.format("해당 요금제는 변경할 수 없습니다. 예약을 취소하고 새로 예약해주세요.", originalPlanType));
        }

        // 3. 변경될 시간 계산
        LocalDate newReservationDate = LocalDate.parse(updateDto.getReservationDate());
        LocalDateTime newStartDateTime;
        LocalDateTime newEndDateTime;
        switch (updateDto.getPlanType()) {
            case "HOURLY":
                if (updateDto.getSelectedTimes() == null || updateDto.getSelectedTimes().isEmpty()) throw new IllegalArgumentException("시간제는 예약 시간을 선택해야 합니다.");
                updateDto.getSelectedTimes().sort(Comparator.naturalOrder());
                newStartDateTime = newReservationDate.atTime(LocalTime.parse(updateDto.getSelectedTimes().get(0), TIME_FORMATTER));
                newEndDateTime = newReservationDate.atTime(LocalTime.parse(updateDto.getSelectedTimes().get(updateDto.getSelectedTimes().size() - 1), TIME_FORMATTER).plusHours(1));
                break;
            case "DAILY":
                newStartDateTime = newReservationDate.atTime(OPEN_TIME);
                newEndDateTime = newReservationDate.atTime(CLOSE_TIME);
                break;
            case "MONTHLY":
                newStartDateTime = newReservationDate.atTime(OPEN_TIME);
                newEndDateTime = newReservationDate.plusMonths(1).atTime(CLOSE_TIME);
                break;
            default: throw new IllegalArgumentException("알 수 없는 요금제 타입입니다.");
        }

        // --- Redis 락 획득 (변경될 대상 좌석/날짜 기준) ---
        String lockKey = String.format("lock:seat:%s:%s", updateDto.getItemId(), updateDto.getReservationDate());
        String lockValue = UUID.randomUUID().toString();
        Boolean lockAcquired = false;

        try {
            lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(LOCK_TIMEOUT_SECONDS));
            if (lockAcquired == null || !lockAcquired) {
                System.out.println("락 획득 실패 (Update): " + lockKey);
                throw new RuntimeException("다른 사용자가 현재 좌석/날짜를 변경/예약 중입니다. 잠시 후 다시 시도해주세요.");
            }
            System.out.println("락 획득 성공 (Update): " + lockKey);

            // --- !!! CRITICAL SECTION START (락 확보 상태) !!! ---
            long overlappingCount = reservationRepository.countOverlappingReservationsExcludingSelf(
                    updateDto.getItemId(), newStartDateTime, newEndDateTime, originalReservation.getResNo());

            if (overlappingCount > 0) {
                System.out.println("중복 예약 발견됨 (Update): " + lockKey);
                throw new RuntimeException("변경하려는 시간에 이미 다른 예약이 존재합니다.");
            }
            System.out.println("DB 예약 가능 확인 완료 (Update)");

            // 5. 변경된 내용으로 가격 재계산
            String newBasePriceStr = calculateBasePrice(updateDto.getPlanType(), updateDto.getSelectedTimes());
            String newDiscountPriceStr = calculateDiscount(newBasePriceStr, updateDto.getCouponId());
            String newFinalPriceStr = calculateFinalPrice(newBasePriceStr, newDiscountPriceStr);
            int newFinalPrice = Integer.parseInt(newFinalPriceStr);

            // 6. 프론트엔드 계산 가격과 비교 (검증)
            if (!Objects.equals(newFinalPrice, updateDto.getTotalPrice())) {
                System.err.printf("가격 불일치. Frontend: %d, Backend: %d%n", updateDto.getTotalPrice(), newFinalPrice);
                // throw new IllegalArgumentException("요청된 가격과 서버 계산 가격이 일치하지 않습니다. 다시 시도해주세요.");
            }

            // 7. 원본 가격과 비교하여 추가 결제 필요 여부 판단
            int originalFinalPrice = Integer.parseInt(originalReservation.getTotalPrice());
            int priceDifference = newFinalPrice - originalFinalPrice;

            Map<String, Object> result = new HashMap<>();

            if (priceDifference > 0) {
                // --- 추가 결제 필요 ---
                System.out.println("가격 증가: 추가 결제 필요 (" + priceDifference + "원)");
                result.put("success", true);
                result.put("paymentRequired", true);
                result.put("message", String.format("예약 변경으로 %s원의 추가 결제가 필요합니다.", String.format("%,d", priceDifference)));
                // 결제 위한 정보 생성 (기존 createReservation과 유사하게)
                // 주의: 새 주문 ID 생성 및 관리 필요
                String newOrderId = "MOD_" + originalReservation.getResNo() + "_" + System.currentTimeMillis();
                result.put("orderId", newOrderId);
                result.put("orderName", String.format("예약 변경 (No.%d) 추가 결제", originalReservation.getResNo()));
                result.put("amount", priceDifference); // !!! 차액만 결제 !!!
                result.put("customerKey", String.valueOf(userId));

                // !!! 중요: 실제 DB 업데이트는 결제 성공 후 웹훅 등에서 처리해야 함 !!!
                // 여기서는 결제 정보만 반환하고 종료. 임시 데이터 저장 로직 필요 시 추가.
                System.out.println("추가 결제 정보를 반환합니다. DB 업데이트는 보류됩니다.");

            } else {
                // --- 가격 동일 또는 감소 (즉시 변경 처리) ---
                System.out.println("가격 동일 또는 감소: 즉시 변경 처리");

                // TODO: 가격 감소 시 환불 로직 필요!
                // if (priceDifference < 0) {
                //     int refundAmount = -priceDifference;
                //     // 1. 연결된 Payment 정보 조회
                //     Payment payment = paymentRepository.findByResNo(resNo).orElse(null);
                //     // 2. 카카오페이 부분 취소 API 호출 (kakaoPayService 사용)
                //     // kakaoPayService.cancelPayment(payment.getPaymentKey(), refundAmount, 0); // 부분 취소
                //     // 3. Payment 테이블 상태 업데이트 (부분 취소 기록)
                // }

                // Reservation 엔티티 업데이트
                originalReservation.setSeatNo(updateDto.getItemId());
                originalReservation.setResStart(newStartDateTime);
                originalReservation.setResEnd(newEndDateTime);
                // originalReservation.setPlanType(updateDto.getPlanType()); // Entity에 필드 추가 시
                // originalReservation.setUserCpNo(updateDto.getCouponId()); // Entity에 필드 추가 시
                originalReservation.setResPrice(newBasePriceStr);
                originalReservation.setDcPrice(newDiscountPriceStr);
                originalReservation.setTotalPrice(newFinalPriceStr);
                originalReservation.setMoDt(LocalDateTime.now()); // 수정 시간 업데이트

                reservationRepository.save(originalReservation);
                System.out.println("DB 예약 정보 업데이트 완료 (즉시 변경)");

                result.put("success", true);
                result.put("paymentRequired", false); // 추가 결제 불필요
                result.put("message", "예약 내용이 성공적으로 변경되었습니다.");
            }

            // --- !!! CRITICAL SECTION END !!! ---
            return result;

        } finally {
            // 락 해제
            if (Boolean.TRUE.equals(lockAcquired)) {
                String redisValue = redisTemplate.opsForValue().get(lockKey);
                if (lockValue.equals(redisValue)) {
                    redisTemplate.delete(lockKey);
                    System.out.println("락 해제 성공 (Update): " + lockKey);
                } else {
                    System.out.println("락 해제 실패 (Update): 소유자 다르거나 만료됨 - " + lockKey);
                }
            }
        }
    }

    // --- 임시: Reservation Entity에 planType 필드가 없을 경우 시간으로 추정 ---
    private String determinePlanTypeFromTimes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return "UNKNOWN";
        if (start.toLocalTime().equals(OPEN_TIME) && end.toLocalTime().equals(CLOSE_TIME)) {
            if (ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()) == 0) {
                return "DAILY";
            } else if (ChronoUnit.MONTHS.between(start.toLocalDate(), end.toLocalDate()) == 1) {
                return "MONTHLY"; // 정확히 한 달 차이
            }
        }
        // 나머지는 HOURLY로 간주 (더 정확한 로직 필요)
        return "HOURLY";
    }


}


