package com.wb.between.admin.reservation.service;

import com.wb.between.admin.reservation.dto.*;
import com.wb.between.admin.reservation.repository.AdminReservationRepository;
import com.wb.between.pay.domain.Payment;
import com.wb.between.pay.repository.PaymentRepository;
import com.wb.between.pay.service.KakaoPayService;
import com.wb.between.reservation.reserve.domain.Reservation;
import com.wb.between.reservation.reserve.repository.ReservationRepository;
import com.wb.between.reservation.seat.domain.Seat;
import com.wb.between.reservation.seat.repository.SeatRepository;
import com.wb.between.user.domain.User;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor        // final 필드 생성자 주입
@Transactional(readOnly = true) // 조회 중심 서비스이므로 readOnly 설정
public class AdminReservationService {

    private final AdminReservationRepository adminReservationRepository; // 예약 관련 데이터베이스 작업을 위한 리포지토리
    private final SeatRepository seatRepository;


    @Autowired
    private ReservationRepository reservationRepository; // 결제 정보 조회/수정 위해 추가
    @Autowired
    private PaymentRepository paymentRepository; // 결제 정보 조회/수정 위해 추가
    @Autowired
    private KakaoPayService kakaoPayService; // 카카오페이 취소 API 호출 위해 추가
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");


    /**
     * 필터링/페이징된 예약 목록 조회
     */
    public Page<ReservationListDto> getReservations(ReservationFilterParamsDto filterParams, Pageable pageable) {
        log.info("예약 목록 조회 서비스 시작. Params: {}, Pageable: {}", filterParams, pageable);

        Specification<Reservation> spec = buildReservationSpecification(filterParams); // 동적 쿼리 조건 생성

        Page<Reservation> reservationPage = adminReservationRepository.findAll(spec, pageable); // 전체 예약 목록 DB 조회
        log.info("DB 조회 완료. 조회된 예약 수: {}", reservationPage.getTotalElements());

        /*
            reservationPage.map(this::mapToReservationListDto)
                reservationPage 안에 있는 예약 리스트 각각의 Reservation 엔티티에 대해 mapToReservationListDto 메소드를 순차적으로 호출
                각 호출에서 반환된 ReservationListDto 객체들을 모음
                원본 reservationPage와 동일한 페이지네이션 정보를 가지는 새로운 Page<ReservationListDto> 객체(dtoPage)를 생성하여 반환
        */
        Page<ReservationListDto> dtoPage = reservationPage.map(this::mapToReservationListDto); // DTO 변환

        if (log.isDebugEnabled()) {
            log.debug("ReservationListDto Page Content ({}개):", dtoPage.getNumberOfElements());
            // 각 DTO 객체를 개별 라인으로 출력 (DTO의 toString() 사용)
            dtoPage.getContent().forEach(dto -> log.debug("예약 목록 DTO {} : {}", dto.getResNo(), dto));
        }

        return dtoPage;
    }

    /**
     * 필터 조건 DTO를 바탕으로 JPA Specification 객체 생성 (익명 클래스 사용)
     */
    private Specification<Reservation> buildReservationSpecification(ReservationFilterParamsDto filterParams) {

        return new Specification<Reservation>() { // 익명 클래스로 Specification 구현

            @Override
            public Predicate toPredicate(Root<Reservation> root, CriteriaQuery<?> query, CriteriaBuilder cb) {

                List<Predicate> predicates = new ArrayList<>(); // WHERE 절 조건들을 담을 리스트

                // Reservation과 User, Seat 엔티티를 Left Join (데이터가 없어도 Reservation은 조회되도록)
                Join<Reservation, User> userJoin = root.join("user", JoinType.LEFT);
                Join<Reservation, Seat> seatJoin = root.join("seat", JoinType.LEFT);

                // 1. 예약일 필터 (시작일 ~ 종료일)
                try {
                    if (StringUtils.hasText(filterParams.getStartDate())) { // 시작일 값이 있으면
                        LocalDateTime startDateTime = LocalDate.parse(filterParams.getStartDate()).atStartOfDay();  // yyyy-MM-dd -> yyyy-MM-dd 00:00:00
                        predicates.add(cb.greaterThanOrEqualTo(root.get("resDt"), startDateTime));                  // 조건: resDt >= startDateTime
                    }
                    if (StringUtils.hasText(filterParams.getEndDate())) { // 종료일 값이 있으면
                        LocalDateTime endDateTime = LocalDate.parse(filterParams.getEndDate()).atTime(LocalTime.MAX); // yyyy-MM-dd -> yyyy-MM-dd 23:59:59...
                        predicates.add(cb.lessThanOrEqualTo(root.get("resDt"), endDateTime)); // 조건: resDt <= endDateTime
                    }
                } catch (DateTimeParseException e) {
                    log.error("잘못된 날짜 형식입니다: {}", e.getMessage());
                    // 날짜 파싱 실패 시 처리 (옵션): 로그만 남기거나, 검색 안되게 하거나
                }

                // 2. 검색어 필터 (이메일 또는 이름)
                if (StringUtils.hasText(filterParams.getSearchText()) && StringUtils.hasText(filterParams.getSearchType())) { // 검색어와 타입이 모두 있으면
                    String keywordPattern = "%" + filterParams.getSearchText().toLowerCase() + "%"; // LIKE 검색 패턴 (%검색어%)
                    if ("email".equals(filterParams.getSearchType())) { // 검색 타입이 이메일이면
                        // 조건: LOWER(user.email) LIKE '%keyword%' (대소문자 무시)
                        predicates.add(cb.like(cb.lower(userJoin.get("email")), keywordPattern));
                    } else if ("name".equals(filterParams.getSearchType())) { // 검색 타입이 이름이면
                        // 조건: LOWER(user.name) LIKE '%keyword%' (대소문자 무시)
                        predicates.add(cb.like(cb.lower(userJoin.get("name")), keywordPattern));
                    }
                }

                // 3. 좌석 필터 (seatNo)
                if (filterParams.getSeatNo() != null) { // 좌석 번호가 선택되었으면 (null이 아니면)
                    // 조건: seat.seatNo = 선택된_좌석번호
                    predicates.add(cb.equal(seatJoin.get("seatNo"), filterParams.getSeatNo()));
                }

                // 생성된 모든 조건들을 AND 로 연결하여 최종 WHERE 절 생성
                return cb.and(predicates.toArray(new Predicate[0]));
            }
        };
    }

    /**
     * Reservation 엔티티를 ReservationListDto로 변환
     */
    private ReservationListDto mapToReservationListDto(Reservation reservation) {

        /*
            각각의 reservation객체에 관한 getUser(), getSeat() 호출 시 관련 DB 조회하여 객체에 담음 > 여러번 동일 쿼리 발생
            => 해결
            AdminReservationRepository.findAll(spec, pageable) 메소드에서
            @EntityGraph(attributePaths = {"user", "seat"})를 사용한 Eager 로딩으로 지정한 연관 엔티티도 미리 함께 조회하여
            매번 DB 조회할 필요가 없게됨
        */
        User user = reservation.getUser();
        Seat seat = reservation.getSeat();

        // 예약 상태(Boolean)를 화면에 표시할 문자열("완료", "취소" 등)로 변환
        String statusString;
        if (reservation.getResStatus() == null) {
            statusString = "확인 불가"; // DB 값이 null 일 때
        } else {
            statusString = Boolean.TRUE.equals(reservation.getResStatus()) ? "완료" : "취소"; // true면 "완료", false면 "취소"
        }

        // ReservationListDto 객체를 빌더 패턴으로 생성하여 반환
        return ReservationListDto.builder()
                .resNo(reservation.getResNo())                             // 예약 번호
                .resDt(reservation.getResDt())                             // 예약 신청 일시
                .userEmail(user != null ? user.getEmail() : "정보 없음")    // User 객체가 null이 아니면 이메일, null이면 "정보 없음"
                .userName(user != null ? user.getName() : "정보 없음")      // User 객체가 null이 아니면 이름, null이면 "정보 없음"
                .seatNm(seat != null ? seat.getSeatNm() : "정보 없음")      // Seat 객체가 null이 아니면 좌석 이름, null이면 "정보 없음"
                .resStart(reservation.getResStart()) // 이용 시작 시간
                .resEnd(reservation.getResEnd())                          // 이용 종료 시간
                .totalPrice(reservation.getTotalPrice())                  // 결제 금액 (Integer)
                .resStatus(statusString)                                  // 변환된 상태 문자열
                .build();
    }

    /**
     * 검색 필터의 좌석 선택 드롭다운에 사용할 좌석 목록 조회
     */
    public List<SeatDto> getAllSeatsForFilter() {
        log.info("좌석 필터용 좌석 목록 조회 서비스 시작");
        List<Seat> seats = seatRepository.findAll(Sort.by(Sort.Direction.ASC, "seatNm")); // 이름순 정렬

        if (seats.isEmpty()) {
            return Collections.emptyList();
        }

        // Seat 리스트 -> SeatDto 리스트 변환
        List<SeatDto> seatDtos = seats.stream()
                .map(seat -> new SeatDto(seat.getSeatNo(), seat.getSeatNm()))
                .collect(Collectors.toList());

        log.info("좌석 목록 DTO 변환 완료. 조회된 좌석 수: {}", seatDtos.size());
        return seatDtos;
    }

    /**
     * 특정 예약의 상세 정보를 조회합니다.
     * @param resNo 조회할 예약 번호
     * @return AdminReservationDetailDto
     * @throws EntityNotFoundException 해당 예약 번호가 없을 경우
     */
    public ReservationDetailDto getReservationDetail(Long resNo) {
        log.info("관리자 - 예약 상세 정보 조회 서비스 시작. resNo: {}", resNo);

        // 1. 예약 정보 조회
        Reservation reservation = adminReservationRepository.findById(resNo)
                .orElseThrow(() -> new EntityNotFoundException("해당 예약을 찾을 수 없습니다. 예약번호: " + resNo));

        User user = reservation.getUser();
        Seat seat = reservation.getSeat();

        // 2. 예약 상태 문자열 변환
        String statusString;
        if (reservation.getResStatus() == null) {
            statusString = "확인 불가";
        } else if (Boolean.TRUE.equals(reservation.getResStatus())) {
            // 예약 시작 시간이 현재 시간보다 이전이고, 아직 이용완료 처리가 안 되었다면 '이용중' 등으로 표시 가능
            if (reservation.getResEnd() != null && reservation.getResEnd().isBefore(LocalDateTime.now())) {
                statusString = "이용완료"; // 또는 별도 상태값이 있다면 그것 사용
            } else {
                statusString = "이용예정";
            }
        } else {
            statusString = "취소됨";
        }

        // 3. 쿠폰 정보 처리 (예시: userCpNo가 있고, UserCoupon 테이블이 있다면)
        String couponNameDisplay = "해당 없음";
        boolean couponActuallyUsed = false;
        if (reservation.getUserCpNo() != null) {
            // 예시: userCouponRepository.findById(reservation.getUserCpNo()).ifPresent(coupon -> couponNameDisplay = coupon.getCouponName());
            // 실제 쿠폰명 조회 로직 필요. 여기서는 ID로 간단히 표시하거나 고정값 사용.
            // dcPrice가 0이 아니거나 "0"이 아니면 쿠폰이 사용된 것으로 간주할 수도 있음.
            if (reservation.getDcPrice() != null && !reservation.getDcPrice().equals("0") && !reservation.getDcPrice().isEmpty()) {
                couponNameDisplay = "쿠폰 사용 (ID: " + reservation.getUserCpNo() + ")"; // 실제 쿠폰 이름으로 대체
                couponActuallyUsed = true;
            }
        }
        String dcPriceDisplay = (reservation.getDcPrice() == null || reservation.getDcPrice().isEmpty()) ? "0" : reservation.getDcPrice();


        // 4. 수정/취소 가능 여부 판단 로직
        boolean canModifyReservation = false;
        boolean canCancelReservation = false;
        if (Boolean.TRUE.equals(reservation.getResStatus())) { // "예약완료" 상태일 때
            if (reservation.getResStart() != null && reservation.getResStart().isAfter(LocalDateTime.now())) {
                // 예약 시작 시간이 미래인 경우에만 수정/취소 가능
                canModifyReservation = true;
                canCancelReservation = true;
            }
        }

        // 5. ReservationDetailDto 빌드
        return ReservationDetailDto.builder()
                .resNo(reservation.getResNo())
                // 예약자 정보
                .userEmail(user != null ? user.getEmail() : "N/A")
                .userName(user != null ? user.getName() : "N/A")
                .userPhoneNo(user != null ? PhoneNumber(user.getPhoneNo()) : "N/A") // User 엔티티에 getPhoneNo() 필요
                .userGrade(user != null ? user.getAuthCd() : "N/A")   // User 엔티티에 getAuthCd() 필요
                // 예약 정보
                .currentSeatNo(seat != null ? seat.getSeatNo() : null)
                .seatNm(seat != null ? seat.getSeatNm() : "N/A")
                .resDt(reservation.getResDt())
                .resStart(reservation.getResStart())
                .resEnd(reservation.getResEnd())
                .statusNm(statusString)
                .planType(reservation.getPlanType())
                // 결제 정보
                .totalPrice(reservation.getTotalPrice())
                .resPrice(reservation.getResPrice())
                .couponName(couponNameDisplay)
                .dcPrice(dcPriceDisplay)
                .couponUsed(couponActuallyUsed)
                // 버튼 활성화 플래그
                .canModify(canModifyReservation)
                .canCancel(canCancelReservation)
                .build();
    }

    // 휴대폰 번호 '-'로 구분하여 포맷팅
    private String PhoneNumber(String phoneNo) {
        if (phoneNo != null && phoneNo.length() == 11) {
            return phoneNo.substring(0, 3) + "-" +  phoneNo.substring(3, 7)+ "-" + phoneNo.substring(7, 11);
        } else if (phoneNo != null && phoneNo.length() == 10) {
            return phoneNo.substring(0, 3) + "-" + phoneNo.substring(3, 6) + "-" + phoneNo.substring(6, 10);
        }
        return phoneNo;
    }


    /**
     * 관리자에 의한 예약 정보 변경 처리
     * @param resNo 변경할 예약 번호
     * @param reservationReqDto 관리자가 수정한 정보 및 사유
     * @param adminUsername 작업을 수행하는 관리자 username (로그용)
     * @return 업데이트된 Reservation 객체
     */
    @Transactional
    public Reservation updateReservationByAdmin(Long resNo, ReservationReqDto reservationReqDto, String adminUsername) {
        log.info("관리자 {}에 의한 예약 {} 수정 요청. 사유: {}", adminUsername, resNo, reservationReqDto.getMoReason());

        Reservation reservation = adminReservationRepository.findById(resNo)
                .orElseThrow(() -> new EntityNotFoundException("변경할 예약 정보를 찾을 수 없습니다: " + resNo));

        // 관리자 권한으로 수정하는 것이므로, 예약 소유권 체크는 생략 (또는 관리자 역할 확인)

        // 변경될 좌석/시간이 예약 가능한지 DB에서 최종 확인 (자기 자신 제외)
        if (reservationReqDto.getSeatNo() != null && reservationReqDto.getResStart() != null && reservationReqDto.getResEnd() != null) {
            long overlappingCount = reservationRepository.countOverlappingReservationsExcludingSelf(
                    reservationReqDto.getSeatNo(),
                    reservationReqDto.getResStart(),
                    reservationReqDto.getResEnd(),
                    resNo // 자기 자신 예약 제외
            );
            if (overlappingCount > 0) {
                throw new RuntimeException("관리자 수정: 변경하려는 시간에 이미 다른 예약이 존재합니다.");
            }
        }

        // 예약 정보 수정
        if (reservationReqDto.getSeatNo() != null) {
            reservation.setSeatNo(reservationReqDto.getSeatNo());
        }
        if (reservationReqDto.getResStart() != null) {
            reservation.setResStart(reservationReqDto.getResStart());
        }
        if (reservationReqDto.getResEnd() != null) {
            reservation.setResEnd(reservationReqDto.getResEnd());
        }
        if (reservationReqDto.getPlanType() != null && !reservationReqDto.getPlanType().isEmpty()) {
            reservation.setPlanType(reservationReqDto.getPlanType());
        }
        reservation.setMoReason(reservationReqDto.getMoReason()); // 수정 사유 저장
        reservation.setResStatus(true); // 관리자가 수정 시 확정 상태로 (정책에 따라)
        // moDt는 @UpdateTimestamp에 의해 자동 업데이트

        Reservation updatedReservation = adminReservationRepository.save(reservation);
        log.info("관리자 {}에 의해 예약 {}이(가) 성공적으로 수정되었습니다.", adminUsername, resNo);
        return updatedReservation;
    }

    /**
     * 관리자에 의한 예약 취소 처리
     * @param resNo 취소할 예약 번호
     * @param reservationReqDto 관리자 취소 요청 정보(사유 포함)
     * @param adminUsername 작업을 수행하는 관리자 username (로그용)
     */
    @Transactional
    public void cancelReservationByAdmin(Long resNo, ReservationReqDto reservationReqDto, String adminUsername) {
        log.info("관리자 {}에 의한 예약 {} 취소 요청. 사유: {}", adminUsername, resNo, reservationReqDto.getMoReason());

        Reservation reservation = adminReservationRepository.findById(resNo)
                .orElseThrow(() -> new EntityNotFoundException("취소할 예약 정보를 찾을 수 없습니다: " + resNo));

        // 관리자 권한으로 취소하는 것이므로, 예약 소유권 체크는 생략 (또는 관리자 역할 확인)
        if (Boolean.FALSE.equals(reservation.getResStatus())) {
            throw new IllegalStateException("이미 취소 처리된 예약입니다.");
        }

        // 연결된 Payment 정보 조회 및 카카오페이 취소 로직
        Optional<Payment> paymentOpt = paymentRepository.findByResNo(resNo);
        boolean paymentCancellationSuccess = true; // 결제가 없거나, 0원이거나, 성공적으로 취소된 경우 true

        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            int paidAmount = 0;
            try { paidAmount = Integer.parseInt(payment.getPayPrice()); } catch (Exception e) { /* 0원 처리 */ }

            if (paidAmount > 0 && "KAKAO".equals(payment.getPayProvider()) && !"CANCELED".equals(payment.getPayStatus())) {
                try {
                    String tid = payment.getTid();
                    if (tid == null || tid.isBlank()) {
                        log.warn("예약 {}의 카카오페이 거래번호(tid)가 없어 결제 취소를 진행할 수 없습니다. (관리자: {})", resNo, adminUsername);
                        // throw new IllegalStateException("카카오페이 거래번호(tid)가 없어 결제 취소를 진행할 수 없습니다.");
                        // 관리자 취소 시, TID 없어도 예약 상태는 변경해야 할 수 있으므로 예외 대신 경고 로깅 후 진행
                    } else {
                        // 카카오페이 취소 API 호출
                        kakaoPayService.canclePayment(tid, paidAmount, 0, reservationReqDto.getMoReason());
                        log.info("관리자 {}에 의해 예약 {}의 카카오페이 결제가 취소되었습니다. (TID: {})", adminUsername, resNo, tid);
                    }
                    payment.setPayStatus("CANCELED");
                    payment.setPayCanclDt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
                    paymentRepository.save(payment);
                } catch (Exception e) {
                    log.error("관리자 {}에 의한 예약 {}의 카카오페이 결제 취소 중 오류 발생: {}", adminUsername, resNo, e.getMessage(), e);
                    paymentCancellationSuccess = false; // 결제 취소 실패 플래그
                    // throw new RuntimeException("카카오페이 결제 취소 중 오류: " + e.getMessage(), e); // 여기서 바로 예외를 던지면 예약 상태 변경 안됨.
                }
            } else { // 결제금액 0원이거나 카카오페이 아니거나 이미 취소된 경우
                if(paymentOpt.isPresent() && paidAmount > 0 && !"CANCELED".equals(payment.getPayStatus())){
                    payment.setPayStatus("CANCELED");
                    payment.setPayCanclDt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
                    paymentRepository.save(payment);
                }
                log.info("예약 {}은(는) 카카오페이 결제 취소 대상이 아닙니다. (관리자: {})", resNo, adminUsername);
            }
        } else {
            log.info("예약 {}에 대한 결제 정보가 없습니다. (관리자: {})", resNo, adminUsername);
        }

        // 예약 상태 변경 및 사유 기록
        if (paymentCancellationSuccess) { // 결제 취소가 성공했거나 필요 없었던 경우에만 예약 상태 변경
            reservation.setResStatus(false); // 취소 상태
            reservation.setMoReason(reservationReqDto.getMoReason()); // 취소 사유 저장

            // moDt는 @UpdateTimestamp에 의해 자동 업데이트
            adminReservationRepository.save(reservation);
            log.info("관리자 {}에 의해 예약 {}이(가) 성공적으로 취소 상태로 변경되었습니다.", adminUsername, resNo);
        } else {
            // 카카오페이 취소 실패 시 예약 상태를 변경하지 않거나, 별도 상태(예: 취소실패)로 관리할 수 있음
            // 여기서는 예외를 던져서 트랜잭션 롤백 유도
            throw new RuntimeException("결제 취소에 실패하여 예약 상태를 변경할 수 없습니다. 예약번호: " + resNo);
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

    private String calculateFinalPrice(String basePriceStr, String discountPriceStr) {
        return String.valueOf(Integer.parseInt(basePriceStr) - Integer.parseInt(discountPriceStr));
    }

    /**
     * 대시보드 - 최근 예약 5개 조회
     */
    @Transactional(readOnly = true)
    public List<ReservationListDto> dashboardReservation() {
        List<Reservation> reservationList = adminReservationRepository.findTop5ByOrderByResDtDesc();
        return reservationList.stream().map(this::mapToReservationListDto).toList();
    }

    /**
     * 오늘 예약 건수 조회
     */
    @Transactional(readOnly = true)
    public long countReservationByResDt() {
        LocalDateTime start = LocalDate.now().atStartOfDay();             // 오늘 00:00:00
        log.debug("start day = {}", start);

        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();   // 내일 00:00:00
        log.debug("end day = {}", end);
        long todayCount = adminReservationRepository.countByResDt(start, end);
        log.debug("countReservationByResDt|todayCount = {}", todayCount);
        return todayCount;
    }

    /**
     * 현재 예약 건수 조회
     */
    @Transactional(readOnly = true)
    public long countReservationNow() {
        LocalDateTime now = LocalDateTime.now();
        boolean resStatus = true;
        long todayCount = adminReservationRepository.countReservationNow(now, resStatus);
        log.debug("countReservationNow|todayCount = {}", todayCount);
        return todayCount;
    }

    /**
     * 오늘 수익 합계 조회
     */
    @Transactional(readOnly = true)
    public BigDecimal revenueByToday() {
        BigDecimal totalRevenue = BigDecimal.ZERO;
        LocalDateTime start = LocalDate.now().atStartOfDay();             // 오늘 00:00:00
        log.debug("start day = {}", start);

        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();   // 내일 00:00:00
        log.debug("end day = {}", end);
        List<Reservation> totalPrice = adminReservationRepository.totalPrice(start, end);
        log.debug("revenueByToday|totalPrice = {}", totalPrice);

        for (Reservation r : totalPrice) {
            BigDecimal revenue = new BigDecimal(r.getTotalPrice());
            log.debug("revenue = {}", revenue);
            totalRevenue = totalRevenue.add(revenue);
            log.debug("revenueByToday|totalRevenue|for = {}", totalRevenue);
         }

        log.debug("revenueByToday|totalRevenue = {}", totalRevenue);

        return totalRevenue;
    }

}
