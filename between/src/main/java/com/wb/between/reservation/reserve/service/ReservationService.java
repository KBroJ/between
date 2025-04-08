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

            // 6. 가격 재계산 (!!! 중요: 실제 가격 정책 및 쿠폰 적용 로직 필요 !!!)
            String basePriceStr = calculateBasePrice(requestDto.getPlanType(), requestDto.getSelectedTimes());
            String discountPriceStr = calculateDiscount(basePriceStr, requestDto.getCouponId());
            String finalPriceStr = calculateFinalPrice(basePriceStr, discountPriceStr);

            // 7. Reservation Entity 생성 및 저장
            Reservation reservation = new Reservation();
            reservation.setUserNo(requestDto.getUserId()); // !!! 실제 사용자 ID 설정 !!!
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
}
