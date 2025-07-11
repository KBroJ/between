package com.wb.between.reservation.reserve.service;

import com.wb.between.pay.domain.Payment;
import com.wb.between.pay.dto.KakaoPayCancelResponseDto;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 중요!

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

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

    @Value("${redis.host}") // 설정 파일에 값이 없으면 기본값 "localhost" 사용
    private String redisHost;

    @Value("${redis.port}")    // 설정 파일에 값이 없으면 기본값 6379 사용
    private int redisPort;

    private static final long LOCK_TIMEOUT_SECONDS = 10; // 락 유지 시간 (초)
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final LocalTime OPEN_TIME = LocalTime.MIDNIGHT; // 운영 시간
    private static final LocalTime CLOSE_TIME = LocalTime.MAX; // 운영 시간

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
        String planType = requestDto.getPlanType();

        switch (planType) {
            case "HOURLY":
                List<String> selectedTimes = requestDto.getSelectedTimes();
                if (selectedTimes == null || selectedTimes.isEmpty()) {
                    throw new IllegalArgumentException("시간제 예약은 시간을 1개 이상 선택해야 합니다.");
                }
                List<LocalTime> times = selectedTimes.stream()
                        .map(timeStr -> LocalTime.parse(timeStr, TIME_FORMATTER))
                        .sorted()
                        .collect(Collectors.toList());

                if (times.size() > 1) { // 여러 시간 선택 시 연속성 검사
                    for (int i = 0; i < times.size() - 1; i++) {
                        if (!times.get(i).plusHours(1).equals(times.get(i + 1))) {
                            throw new IllegalArgumentException("시간제 예약은 연속된 시간으로만 한 번에 요청할 수 있습니다. 떨어진 시간은 개별적으로 예약해주세요.");
                        }
                    }
                }
                startDateTime = reservationDate.atTime(times.get(0));
                endDateTime = reservationDate.atTime(times.get(times.size() - 1).plusHours(1));
                break;
            case "DAILY":
                startDateTime = reservationDate.atTime(OPEN_TIME);
                endDateTime = reservationDate.plusDays(1).atTime(OPEN_TIME); // 다음 날 00:00 미포함
                break;
            case "MONTHLY":
                startDateTime = reservationDate.atTime(OPEN_TIME);
                endDateTime = reservationDate.plusMonths(1).atTime(OPEN_TIME); // 다음 달 같은 날짜 00:00 미포함
                break;
            default:
                throw new IllegalArgumentException("알 수 없는 요금제 타입입니다: " + planType);
        }
        System.out.printf("[Service] 계산된 예약 시간: PlanType=%s, Start=%s, End=%s%n", planType, startDateTime, endDateTime);

        // 3. Redis 락 키 정의 및 획득
        // 시간제는 선택된 시간 범위 전체를 하나의 락 대상으로 봄
        String lockKey = String.format("lock:seat:%s:%s:%s-%s",
                requestDto.getItemId(),
                reservationDate.toString(),
                startDateTime.toLocalTime().format(TIME_FORMATTER),
                endDateTime.toLocalTime().format(TIME_FORMATTER)
        );
        if (!"HOURLY".equals(planType)) { // 일일권, 월정액권은 날짜까지만으로도 충분할 수 있음 (정책에 따라 조정)
            lockKey = String.format("lock:seat:%s:%s:%s", requestDto.getItemId(), reservationDate.toString(), planType);
        }

        String lockValue = UUID.randomUUID().toString();
        Boolean lockAcquired = false;

        System.out.printf("[Service] Redis 락 시도 예정 - 대상 Redis: %s:%d, Lock Key: %s%n", redisHost, redisPort, lockKey);

        try {
            lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(LOCK_TIMEOUT_SECONDS));

            if (lockAcquired == null || !lockAcquired) {
                System.err.printf("[Service] 락 획득 실패 (이미 사용 중) - Lock Key: %s%n", lockKey);
                throw new RuntimeException("다른 사용자가 해당 좌석/시간에 대한 작업을 진행 중입니다. 잠시 후 다시 시도해주세요.");
            }
            System.out.printf("[Service] Redis 락 획득 성공 - Lock Key: %s%n", lockKey);

            // --- CRITICAL SECTION START ---
            // 4. 예약 가능 여부 DB에서 최종 확인
            long overlappingCount = reservationRepository.countAnyOverlappingReservations(
                    requestDto.getItemId(), startDateTime, endDateTime);

            if (overlappingCount > 0) {
                System.out.println("[Service] 중복 예약 발견됨: " + lockKey);
                throw new RuntimeException("선택하신 시간에 이미 다른 예약이 존재합니다.");
            }
            System.out.println("[Service] DB 예약 가능 확인 완료.");

            // 5. 가격 계산
            Seat targetSeat = seatRepository.findById(requestDto.getItemId()) // 좌석 정보 조회 (가격 가져오기 위해)
                    .orElseThrow(() -> new EntityNotFoundException("좌석 정보를 찾을 수 없습니다: " + requestDto.getItemId()));

            String basePriceStr = calculateBasePrice(requestDto.getPlanType(), requestDto.getSelectedTimes());
            String discountPriceStr = calculateDiscount(basePriceStr, requestDto.getCouponId());
            String finalPriceStr = calculateFinalPrice(basePriceStr, discountPriceStr);
            System.out.printf("[Service] 가격 계산 완료: Base=%s, Discount=%s, Final=%s%n", basePriceStr, discountPriceStr, finalPriceStr);



            // 6. 임직원 0원 처리
            boolean isEmployee = "임직원".equals(authCd); // authCd 사용
            if (isEmployee) {
                System.out.println("[Service] 임직원 확인. 최종 가격 0원으로 조정.");
                finalPriceStr = "0";
                discountPriceStr = basePriceStr; // 할인액을 원금으로 (0원 만들기 위해)
            }

            // 7. Reservation Entity 생성 및 저장
            Reservation reservation = new Reservation();
            reservation.setUserNo(userNo);
            reservation.setSeatNo(requestDto.getItemId());
            reservation.setTotalPrice(finalPriceStr);
            reservation.setResPrice(basePriceStr);
            reservation.setDcPrice(discountPriceStr);
            if (requestDto.getCouponId() != null && !requestDto.getCouponId().isBlank()) {
                reservation.setUserCpNo(Long.parseLong(requestDto.getCouponId()));
            }
            reservation.setResStart(startDateTime);
            reservation.setResEnd(endDateTime);
            reservation.setPlanType(planType);

            boolean isZeroPrice = "0".equals(finalPriceStr);
            boolean isConfirmedImmediately = isEmployee && isZeroPrice;
            reservation.setResStatus(isConfirmedImmediately ? Boolean.TRUE : null); // 상태 설정
            System.out.println("[Service] DB 저장 직전 reservation 객체 상태: " + reservation.getResStatus());

            Reservation savedReservation = reservationRepository.save(reservation);
            System.out.println("[Service] >>> DB Reservation 저장 완료! ResNo: " + savedReservation.getResNo() + ", Status: " + savedReservation.getResStatus());
            // --- CRITICAL SECTION END ---
            return savedReservation;

        } finally {
            // 8. 락 해제
            if (Boolean.TRUE.equals(lockAcquired)) {
                String redisValue = redisTemplate.opsForValue().get(lockKey);
                if (lockValue.equals(redisValue)) {
                    redisTemplate.delete(lockKey);
                    System.out.println("[Service] 락 해제 성공: " + lockKey);
                } else {
                    System.out.println("[Service] 락 해제 실패: 락 소유자가 다르거나 만료됨 - " + lockKey);
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

        // LocalDateTime now = LocalDateTime.now();
        ZoneId seoulZoneId =  ZoneId.of("Asia/Seoul");
        LocalDateTime now = LocalDateTime.now(seoulZoneId);
        if (reservation.getResStart() != null && now.isAfter(reservation.getResStart())) {
            System.out.println("[Service] 경고: 이미 시작된 예약 취소 시도 (현재 로직 허용)");
        }

        // 4. 연결된 Payment 정보 조회 (카카오페이 취소에 필요한 정보 가져오기)
        Optional<Payment> paymentOpt = paymentRepository.findByResNo(resNo);
        boolean kakaoCancelSuccess = false; // 카카오 취소 성공 여부 플래그

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

            if (paidAmount > 0 && "KAKAO".equals(payment.getPayProvider()) && !"CANCELED".equals(payment.getPayStatus())) {
                try {
                    String tid  =payment.getTid();
                    if(tid == null || tid.isBlank()){
                        throw new IllegalStateException("취소에 필요한 카카오페이 거래번호(tid)가 저장되어 있지 않습니다.");
                    }
                    int cancelAmount = Integer.parseInt(payment.getPayPrice());
                    int taxFreeAmount = 0; // 비과세 금액 계산 로직 필요시 추가
                    
                    // 카카오페이 취소 API 호출
                    KakaoPayCancelResponseDto cancleResponse = kakaoPayService.canclePayment(tid, paidAmount, taxFreeAmount, "사용자 예약 취소");

                    /*payment.setPayStatus("CANCELED"); // Payment 상태 변경*/
                    if(cancleResponse != null && "CANCEL_PAYMENT".equals(cancleResponse.getStatus())){
                        kakaoCancelSuccess = true;
                        System.out.println("[예약 Service] 카카오페이 결제 취소 성공");
                    } else {
                        System.err.println("[Service] 카카오페이 취소는 성공했으나 응답 상태 이상: " + cancleResponse);
                        throw new RuntimeException("카카오페이 취소는 성공했으나 응답 상태가 올바르지 않습니다.");
                    }

                    payment.setPayCanclDt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)); // 시간 기록
                    paymentRepository.save(payment);

                } catch (Exception e) {
                    System.err.println("!!! 카카오페이 결제 취소 중 오류 발생 !!! - " + e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException("카카오페이 결제 취소 중 오류가 발생했습니다. 관리자에게 문의하세요.", e);
                }

            } else {
                System.out.println("[Service] 0원 예약 또는 카카오페이 결제 건이 아니므로 외부 API 취소 호출 생략.");
                kakaoCancelSuccess = true; // 내부 처리만 하면 되므로 성공으로 간주
            }

            if (kakaoCancelSuccess) {
                payment.setPayStatus("CANCELED");
                payment.setPayCanclDt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
                paymentRepository.save(payment);
                System.out.println("[Service] Payment 상태 'CANCELED' 업데이트 완료");
            }

            // 6. Payment 테이블 상태 업데이트
            payment.setPayStatus("CANCELED"); // 결제 상태 '취소'
            payment.setPayCanclDt(now.format(DateTimeFormatter.ISO_DATE_TIME)); // 취소 시각 기록 (String)
            paymentRepository.save(payment);
            System.out.println("[Service] Payment 상태 'CANCELED' 업데이트 완료");

        } else {
            System.out.println("[Service] 해당 예약(" + resNo + ")에 대한 결제 정보 없음. Reservation 상태만 변경.");
            kakaoCancelSuccess = true;
        }

        if (kakaoCancelSuccess) {
            reservation.setResStatus(false); // false = 취소 상태로 가정
            reservationRepository.save(reservation);
            System.out.println("[Service] Reservation 상태 '취소' 업데이트 완료: ResNo=" + resNo);
        } else {
            System.err.println("[Service] Reservation 상태 업데이트 실패 (Kakao 취소 실패)");
            throw new RuntimeException("결제 취소는 실패했으나 예약 상태 변경 시도 오류 (내부 로직 오류)");
        }

        System.out.println("[Service] 예약 취소 처리 완료: ResNo=" + resNo);
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


