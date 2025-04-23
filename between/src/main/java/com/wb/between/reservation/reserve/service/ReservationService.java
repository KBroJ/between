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
        Long userNo = user.getUserNo();
        String authCd = user.getAuthCd();   // 권한 관리
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

            // 임직원 0원 무료 처리 로직
            boolean isAuth = "임직원".equals(authCd);
            if(isAuth){
                System.out.println("임직원 권한 유저 확인 0원으로 계산 처리 합시다.");
                finalPriceStr = "0";
                discountPriceStr = basePriceStr;
            }
            boolean isZeroPrice = "0".equals(finalPriceStr); // 0원 확인
            boolean isConfirmedImmediately = isAuth && isZeroPrice;
            Boolean statusToSet = isConfirmedImmediately ? Boolean.TRUE : null;

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
            reservation.setPlanType(requestDto.getPlanType()); // Entity에 필드 추가 시
            // resDt, moDt는 @CreationTimestamp, @UpdateTimestamp로 자동 관리
            // 임직원 0원 처리
            reservation.setResStatus(statusToSet); // 상태 설정
            System.out.println("[Service] DB 저장 직전 reservation 객체 상태: " + reservation.getResStatus()); // <<<--- 로그 1: 저장 직전 상태

            Reservation savedReservation = reservationRepository.save(reservation);
            System.out.println("[Service] >>> DB Reservation 저장 완료! ResNo: " + savedReservation.getResNo() + ", Status: " + savedReservation.getResStatus());
            /*Reservation savedReservation = reservationRepository.save(reservation);
            System.out.println("DB에 예약 정보 저장 성공 (상태: 보류): " + savedReservation.getResNo());*/
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
     * @param currentUserId 요청한 사용자 ID (본인 확인용)
     * @throws RuntimeException 예약 정보를 찾을 수 없거나, 취소 권한이 없거나, 카카오페이 취소 실패 시
     */
    @Transactional // DB 업데이트와 API 호출을 묶어서 처리
    public void cancelReservation(Long resNo, Long currentUserId) {
        System.out.printf("[Service] 예약 취소 요청 수신 - ResNo: %d, Username: %s%n", resNo, currentUserId);
        // 1. 예약 정보 조회 및 유효성 검사
        Reservation reservation = reservationRepository.findById(resNo)
                .orElseThrow(() -> new EntityNotFoundException("취소할 예약 정보를 찾을 수 없습니다: " + resNo));


        // 2. 본인 예약만 취소할 수 있게
        if (!reservation.getUserNo().equals(currentUserId)) {
            throw new SecurityException("해당 예약을 취소할 권한이 없습니다.");
        }

        // 3. 이미 취소되었거나 완료되지 않은 예약인지 확인
        if (Boolean.FALSE.equals(reservation.getResStatus())) { // 이미 취소된 경우 (false = 취소 가정)
            throw new IllegalStateException("이미 취소 처리된 예약입니다.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (reservation.getResStart() != null && now.isAfter(reservation.getResStart())) {
            System.out.println("[Service] 경고: 이미 시작된 예약 취소 시도 (현재 로직 허용)");
        }

        // 4. 연결된 Payment 정보 조회 (카카오페이 취소에 필요한 정보 가져오기)
        Optional<Payment> paymentOpt = paymentRepository.findByResNo(resNo);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            System.out.println("[Service] 관련 결제 정보 찾음: PaymentKey=" + payment.getPaymentKey());

            // 5. 실제 결제된 금액이 있었는지 확인 (0원 예약 여부)
            int paidAmount;
            try {
                paidAmount = Integer.parseInt(payment.getPayPrice());
            } catch (NumberFormatException | NullPointerException e) {
                System.err.println("결제 금액(payPrice) 파싱 오류 또는 없음: " + payment.getPayPrice());
                paidAmount = 0; // 오류 시 0원으로 간주 (또는 예외 처리)
            }

            if (paidAmount > 0 && "KAKAO".equals(payment.getPayProvider())) {
                try {
                    String tid = payment.getPaymentKey(); // tid가 저장된 필드 사용
                    int cancelAmount = Integer.parseInt(payment.getPayPrice());
                   //  kakaoPayService.cancelKakaoPayment(tid, cancelAmount, 0); // 카카오 취소 API 호출
                    System.out.println("카카오페이 결제 취소 API 호출 성공 (가정)");
                    payment.setPayStatus("CANCELED"); // Payment 상태 변경
                    payment.setPayCanclDt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)); // 시간 기록
                    paymentRepository.save(payment);

                } catch (Exception e) {
                    System.err.println("!!! 카카오페이 결제 취소 중 오류 발생 !!! - " + e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException("카카오페이 결제 취소 중 오류가 발생했습니다. 관리자에게 문의하세요.", e);
                }

            } else {
                System.out.println("[Service] 0원 예약 또는 카카오페이 결제 건이 아니므로 외부 API 취소 호출 생략.");
            }
            // ----------------------------------------------------------

            // 6. Payment 테이블 상태 업데이트
            payment.setPayStatus("CANCELED"); // 결제 상태 '취소'
            payment.setPayCanclDt(now.format(DateTimeFormatter.ISO_DATE_TIME)); // 취소 시각 기록 (String)
            paymentRepository.save(payment);
            System.out.println("[Service] Payment 상태 'CANCELED' 업데이트 완료");

        } else {
            System.out.println("[Service] 해당 예약(" + resNo + ")에 대한 결제 정보 없음. Reservation 상태만 변경.");

        }

        // 7. Reservation 테이블 상태 '취소'로 업데이트
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
     *
     * @param resNo           변경할 예약 번호
     * @param modificationDto 변경할 내용 DTO
     * @param currentUserId   현재 로그인한 사용자 ID (소유권 확인용)
     * @return 업데이트된 Reservation 객체
     * @throws RuntimeException, EntityNotFoundException, SecurityException, IllegalArgumentException
     */
    @Transactional // DB 업데이트가 있으므로 트랜잭션 필요
    public Reservation updateReservation(Long resNo, ReservationUpdateRequestDto modificationDto, Long currentUserId) {
        System.out.printf("[Service] 예약 변경 요청 시작: ResNo=%d, UserID=%d%n", resNo, currentUserId);

        // 1. 원본 예약 정보 조회
        Reservation originalReservation = reservationRepository.findById(resNo)
                .orElseThrow(() -> new EntityNotFoundException("변경할 예약 정보를 찾을 수 없습니다: " + resNo));

        // 2. 예약 소유권 확인
        if (!originalReservation.getUserNo().equals(currentUserId)) {
            throw new SecurityException("해당 예약을 변경할 권한이 없습니다."); // 예외 타입은 상황에 맞게
        }
        System.out.println("[Service] 예약 소유권 확인 완료");

/*
        // 3. 요금제 변경 시도 확인 (변경 불가 정책)
        if (modificationDto.getPlanType() != null && !modificationDto.getPlanType().equals(originalReservation.getPlanType())) {
            throw new IllegalArgumentException("예약 변경 시 요금제는 변경할 수 없습니다.");
        }
        System.out.println("[Service] 요금제 변경 없음 확인 완료");
*/

        // 4. 변경될 예약 시작/종료 시각 계산
        LocalDate newReservationDate = LocalDate.parse(modificationDto.getReservationDate());
        LocalDateTime newStartDateTime;
        LocalDateTime newEndDateTime;
        switch (modificationDto.getPlanType()) { // DTO의 planType 사용 (원본과 동일해야 함)
            case "HOURLY":
                if (modificationDto.getSelectedTimes() == null || modificationDto.getSelectedTimes().isEmpty()) throw new IllegalArgumentException("시간제는 예약 시간을 선택해야 합니다.");
                modificationDto.getSelectedTimes().sort(Comparator.naturalOrder());
                LocalTime st = LocalTime.parse(modificationDto.getSelectedTimes().get(0), TIME_FORMATTER);
                LocalTime lt = LocalTime.parse(modificationDto.getSelectedTimes().get(modificationDto.getSelectedTimes().size() - 1), TIME_FORMATTER);
                newStartDateTime = newReservationDate.atTime(st);
                newEndDateTime = newReservationDate.atTime(lt.plusHours(1));
                break;
            case "DAILY": newStartDateTime = newReservationDate.atTime(OPEN_TIME); newEndDateTime = newReservationDate.atTime(CLOSE_TIME); break;
            case "MONTHLY": newStartDateTime = newReservationDate.atTime(OPEN_TIME); newEndDateTime = newReservationDate.plusMonths(1).atTime(CLOSE_TIME); break;
            default: throw new IllegalArgumentException("알 수 없는 요금제 타입입니다.");
        }
        System.out.println("[Service] 변경될 예약 시간 계산 완료: " + newStartDateTime + " ~ " + newEndDateTime);

        // --- 변경될 좌석/시간에 대한 Redis Lock 시도 ---
        // (주의: 원래 예약 슬롯에 대한 락 해제는? -> 여기선 새 슬롯만 잠그고 DB로 최종 확인)
        String newLockKey = String.format("lock:seat:%s:%s", modificationDto.getItemId(), modificationDto.getReservationDate());
        String newLockValue = UUID.randomUUID().toString();
        Boolean newLockAcquired = false;

        try {
            newLockAcquired = redisTemplate.opsForValue().setIfAbsent(newLockKey, newLockValue, Duration.ofSeconds(LOCK_TIMEOUT_SECONDS));

            if (newLockAcquired != null && newLockAcquired) {
                System.out.println("[Service] 변경 대상 슬롯 Redis 락 획득 성공: " + newLockKey);

                // --- CRITICAL SECTION START ---
                try {
                    // 5. 변경될 좌석/시간이 예약 가능한지 DB에서 최종 확인 (자기 자신 제외)
                    long overlappingCount = reservationRepository.countOverlappingReservationsExcludingSelf(
                            modificationDto.getItemId(), newStartDateTime, newEndDateTime, resNo // !!! 자기 자신(resNo) 제외 !!!
                    );
                    if (overlappingCount > 0) {
                        throw new RuntimeException("변경하려는 시간에 이미 다른 예약이 존재합니다.");
                    }
                    System.out.println("[Service] DB 예약 가능 최종 확인 완료 (중복 없음)");

                    // 6. 가격 재계산
                    String basePriceStr = calculateBasePrice(modificationDto.getPlanType(), modificationDto.getSelectedTimes());
                    String discountPriceStr = calculateDiscount(basePriceStr, modificationDto.getCouponId());
                    String finalPriceStr = calculateFinalPrice(basePriceStr, discountPriceStr);
                    System.out.println("[Service] 변경 후 가격 계산 완료: " + finalPriceStr);

                    // 7. Reservation Entity 업데이트
                    originalReservation.setSeatNo(modificationDto.getItemId());
                    originalReservation.setResStart(newStartDateTime);
                    originalReservation.setResEnd(newEndDateTime);
                    originalReservation.setTotalPrice(finalPriceStr);
                    originalReservation.setResPrice(basePriceStr);
                    originalReservation.setDcPrice(discountPriceStr);
                    // originalReservation.setUserCpNo(...); // 쿠폰 ID 업데이트
                    originalReservation.setResStatus(true); // 변경 시 상태는 '확정' 유지 (정책에 따라 다름)
                    // moDt는 @UpdateTimestamp로 자동 업데이트될 것임

                    // 8. DB에 변경사항 저장
                    System.out.println("[Service] Reservation 정보 업데이트 시도...");
                    Reservation updatedReservation = reservationRepository.save(originalReservation); // JPA가 변경 감지 후 UPDATE
                    System.out.println("[Service] >>> Reservation 정보 업데이트 성공! ResNo: " + updatedReservation.getResNo());

                    // --- CRITICAL SECTION END ---
                    return updatedReservation;

                } catch (Exception criticalException) {
                    System.err.println("!!! 예약 변경 처리 중 에러 !!! " + criticalException.getMessage());
                    throw criticalException;
                }

            } else {
                System.out.println("[Service] 변경 대상 슬롯 Redis 락 획득 실패: " + newLockKey);
                throw new RuntimeException("다른 사용자가 해당 좌석/시간을 변경/예약 중입니다.");
            }
        } finally {
            // 락 해제 (내가 획득했던 락만 해제)
            if (Boolean.TRUE.equals(newLockAcquired) && newLockValue.equals(redisTemplate.opsForValue().get(newLockKey))) {
                redisTemplate.delete(newLockKey);
                System.out.println("[Service] 변경 대상 슬롯 Redis 락 해제 성공: " + newLockKey);
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


