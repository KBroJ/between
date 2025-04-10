package com.wb.between.reservation.reserve.service;

import com.wb.between.reservation.reserve.domain.Reservation;
import com.wb.between.reservation.reserve.dto.ReservationRequestDto;
import com.wb.between.reservation.reserve.repository.ReservationRepository;
import com.wb.between.reservation.seat.repository.SeatRepository;
import com.wb.between.user.domain.User;
import com.wb.between.user.repository.UserRepository;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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

    private static final long LOCK_TIMEOUT_SECONDS = 10; // 락 유지 시간 (초)
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final LocalTime OPEN_TIME = LocalTime.of(9, 0);  // 운영 시간 (설정 필요)
    private static final LocalTime CLOSE_TIME = LocalTime.of(22, 0); // 운영 시간 (설정 필요)

    /**
     * Redis 락을 사용하여 예약을 생성하고 '보류' 상태로 DB에 저장합니다.
     *
     * @param requestDto 예약 요청 정보 (프론트엔드에서 받음)
     * @param username   현재 로그인된 사용자의 username (Controller에서 전달받음)
     * @return 생성된 Reservation 객체 (상태: 보류)
     * @throws RuntimeException 예약 불가 시 또는 처리 중 오류 발생 시
     */
    @Transactional // DB 저장/수정 작업이 있으므로 트랜잭션 필요
    public Reservation createReservationWithLock(ReservationRequestDto requestDto, String username) {
        // 1. 필수 요청 데이터 유효성 검사
        Objects.requireNonNull(requestDto.getItemId(), "좌석 ID는 필수입니다.");
        Objects.requireNonNull(requestDto.getReservationDate(), "예약 날짜는 필수입니다.");
        Objects.requireNonNull(requestDto.getPlanType(), "요금제는 필수입니다.");
        Objects.requireNonNull(username, "사용자 정보(username)는 필수입니다.");

        // 2. 사용자 정보 조회 (username(email) 기반으로 userNo 찾기)
        User user = userRepository.findByEmail(username) // !!! 실제 UserRepository 와 User Entity 확인 !!!
                .orElseThrow(() -> new UsernameNotFoundException("서비스에서 사용자를 찾을 수 없습니다: " + username));
        Long userNo = user.getUserNo(); // !!! User Entity에 getUserNo() 확인 !!!
        if (userNo == null) {
            throw new IllegalStateException("사용자 번호(userNo)를 가져올 수 없습니다.");
        }
        System.out.println("[Service] 사용자 확인 완료: userNo=" + userNo);

        // 3. 예약 시작/종료 시각 계산
        LocalDate reservationDate = LocalDate.parse(requestDto.getReservationDate());
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;
        switch (requestDto.getPlanType()) {
            case "HOURLY":
                if (requestDto.getSelectedTimes() == null || requestDto.getSelectedTimes().isEmpty()) throw new IllegalArgumentException("시간제는 예약 시간을 선택해야 합니다.");
                requestDto.getSelectedTimes().sort(Comparator.naturalOrder());
                LocalTime startTime = LocalTime.parse(requestDto.getSelectedTimes().get(0), TIME_FORMATTER);
                LocalTime lastTime = LocalTime.parse(requestDto.getSelectedTimes().get(requestDto.getSelectedTimes().size() - 1), TIME_FORMATTER);
                startDateTime = reservationDate.atTime(startTime);
                endDateTime = reservationDate.atTime(lastTime.plusHours(1)); // 마지막 시간 다음 시간까지
                break;
            case "DAILY": startDateTime = reservationDate.atTime(OPEN_TIME); endDateTime = reservationDate.atTime(CLOSE_TIME); break;
            case "MONTHLY": startDateTime = reservationDate.atTime(OPEN_TIME); endDateTime = reservationDate.plusMonths(1).atTime(CLOSE_TIME); break;
            default: throw new IllegalArgumentException("알 수 없는 요금제 타입입니다.");
        }
        System.out.println("[Service] 예약 시간 계산 완료: " + startDateTime + " ~ " + endDateTime);

        // 4. Redis 락 키 정의 및 락 값 생성
        String lockKey = String.format("lock:seat:%s:%s", requestDto.getItemId(), requestDto.getReservationDate()); // 좌석 ID가 Long 이므로 %d 사용 고려
        String lockValue = UUID.randomUUID().toString();
        Boolean lockAcquired = false; // try 블록 시작 전에 false로 초기화

        try {
            // 5. 락 획득 시도
            lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(LOCK_TIMEOUT_SECONDS));

            if (lockAcquired != null && lockAcquired) { // 락 획득 성공
                System.out.println("[Service] Redis 락 획득 성공: " + lockKey);

                // --- CRITICAL SECTION START ---
                try {
                    // 6. DB에서 예약 가능 여부 최종 확인 (겹치는 예약 확인)
                    long overlappingCount = reservationRepository.countOverlappingReservations(
                            requestDto.getItemId(), startDateTime, endDateTime);
                    if (overlappingCount > 0) {
                        throw new RuntimeException("선택하신 시간에 이미 다른 예약이 존재합니다."); // 중복 시 에러
                    }
                    System.out.println("[Service] DB 예약 가능 확인 완료 (중복 없음)");

                    // 7. 가격 재계산 (!!! 실제 쿠폰 로직 등 구현 필요 !!!)
                    String basePriceStr = calculateBasePrice(requestDto.getPlanType(), requestDto.getSelectedTimes());
                    String discountPriceStr = calculateDiscount(basePriceStr, requestDto.getCouponId());
                    String finalPriceStr = calculateFinalPrice(basePriceStr, discountPriceStr);
                    System.out.println("[Service] 가격 계산 완료: 최종 금액=" + finalPriceStr);

                    // 8. Reservation Entity 생성 및 필드 설정
                    Reservation reservation = new Reservation();
                    reservation.setUserNo(userNo); // 조회한 userNo 사용
                    reservation.setSeatNo(requestDto.getItemId());
                    reservation.setTotalPrice(finalPriceStr);
                    reservation.setResPrice(basePriceStr);
                    reservation.setDcPrice(discountPriceStr);
                    reservation.setUserCpNo(requestDto.getCouponId() != null ? Long.parseLong(requestDto.getCouponId()) : null);
                    reservation.setResStart(startDateTime);
                    reservation.setResEnd(endDateTime);
                    // reservation.setPlanType(requestDto.getPlanType()); // Entity에 필드 추가했다면 설정

                    // !!! 중요: 예약 상태를 '보류' (null)로 설정 !!!
                    // DB 스키마에서 resStatus가 boolean NULL 이므로 null을 보류 상태로 사용
                    reservation.setResStatus(null);

                    // 9. DB에 Reservation 저장
                    System.out.println("[Service] Reservation 정보 저장 시도...");
                    Reservation savedReservation = reservationRepository.save(reservation);
                    System.out.println("[Service] >>> DB Reservation 저장 성공! (상태: 보류) ResNo: " + savedReservation.getResNo());

                    // --- CRITICAL SECTION END ---
                    return savedReservation; // 저장된 예약 정보 반환

                } catch (Exception criticalException) {
                    // 크리티컬 섹션 내부 에러 발생 시 로그 남기고 다시 던짐 (롤백 유도)
                    System.err.println("[Service] !!! 크리티컬 섹션 처리 중 에러 발생 !!! " + criticalException.getMessage());
                    throw new RuntimeException("예약 처리 중 내부 오류 발생: " + criticalException.getMessage(), criticalException);
                }

            } else {
                // 락 획득 실패 시
                System.out.println("[Service] Redis 락 획득 실패 (다른 사용자 처리 중): " + lockKey);
                throw new RuntimeException("다른 사용자가 현재 좌석/날짜를 예약 중입니다. 잠시 후 다시 시도해주세요.");
            }
        } finally {
            // 10. 락 해제 (내가 획득했던 락만 해제)
            if (Boolean.TRUE.equals(lockAcquired) && lockValue.equals(redisTemplate.opsForValue().get(lockKey))) {
                redisTemplate.delete(lockKey);
                System.out.println("[Service] Redis 락 해제 성공: " + lockKey);
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
}
