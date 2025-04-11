package com.wb.between.mypage.service;

import com.wb.between.common.exception.CustomException;
import com.wb.between.mypage.dto.MyReservationDetailDto;
import com.wb.between.mypage.dto.MyReservationDto; // Import DTO
import com.wb.between.mypage.repository.MyReservationRepository;
import com.wb.between.reservation.reserve.domain.Reservation;
import com.wb.between.reservation.seat.domain.Seat;
import com.wb.between.reservation.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.Pageable; // Import Pageable
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션 적용
public class MyReservationService {

    private final MyReservationRepository myReservationRepository; // 예약 리포지토리 주입
    private final SeatRepository seatRepository; // 좌석 리포지토리 주입

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 사용자의 예약 내역을 조건에 맞게 조회합니다. (Controller에서 호출됨)
     *
     * @param userNo       사용자 번호
     * @param tab          조회 탭 ("upcoming", "past", "cancelled")
     * @param startDateStr 조회 시작일 문자열 (YYYY-MM-DD)
     * @param endDateStr   조회 종료일 문자열 (YYYY-MM-DD)
     * @param pageable     페이징 정보 (정렬 포함)
     * @return 페이징된 예약 내역 DTO
     */
    public Page<MyReservationDto> findMyReservations(Long userNo, String tab, String startDateStr, String endDateStr, Pageable pageable) {
        log.debug("findMyReservations Service - userNo: {}, tab: {}, startDate: {}, endDate: {}, pageable: {}",
                userNo, tab, startDateStr, endDateStr, pageable);

        // 1. 날짜 파싱 및 LocalDateTime 으로 변환
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;

        try {

            // 시작일: YYYY-MM-DD 00:00:00
            startDateTime = LocalDate.parse(startDateStr, DATE_FORMATTER).atStartOfDay();
            // 종료일: YYYY-MM-DD 23:59:59.999999999 (해당 일자 전체 포함)
            endDateTime = LocalDate.parse(endDateStr, DATE_FORMATTER).atTime(LocalTime.MAX);

        } catch (DateTimeParseException e) {
            log.error("날짜 파싱 오류: startDate={}, endDate={}", startDateStr, endDateStr, e);
            // 오류 시 기본값 설정 또는 예외 처리 (컨트롤러에서 기본값 처리했으므로 여기선 로깅 위주)
            startDateTime = LocalDate.now().minusMonths(3).atStartOfDay();
            endDateTime = LocalDate.now().atTime(LocalTime.MAX);
            log.warn("날짜 파싱 오류로 기본 기간 사용: {} ~ {}", startDateTime, endDateTime);
            // throw new CustomException("날짜 형식이 올바르지 않습니다."); // 필요시 예외 발생
        }

        // 2. tab 파라미터 값을 status 코드로 변환 ('1', '2', '3')
        String tabStatus;
        switch (tab.toLowerCase()) {
            case "upcoming":
                tabStatus = "1"; // 예정
                break;
            case "past":
                tabStatus = "2"; // 지난
                break;
            case "cancelled":
                tabStatus = "3"; // 취소
                break;
            default:
                log.warn("알 수 없는 tab 값: {}, 기본값 'upcoming'('1') 사용", tab);
                tabStatus = "1"; // 기본값 설정 또는 예외 처리
                // throw new CustomException("유효하지 않은 탭입니다.");
        }

        // 3. 현재 시각 가져오기 (JPQL의 :now 파라미터에 전달)
        LocalDateTime now = LocalDateTime.now();

        // 4. Repository 호출하여 예약 내역 조회
        Page<MyReservationDto> results = myReservationRepository.findUserReservationsWithStatus(
                userNo, startDateTime, endDateTime, tabStatus, now, pageable
        );

        log.debug("findMyReservations Service - Found {} reservations for tab '{}'", results.getTotalElements(), tab);

        return results; // 조회 결과(Page 객체) 반환
    }



    /**
     * 마이페이지 > 예약 내역 > 예약 상세 조회
     * 특정 예약의 상세 정보를 조회합니다.
     * @param userNo 현재 로그인한 사용자
     * @param resNo 조회할 예약 번호
     * @return ReservationDetailDto
     * @throws CustomException 예약이 없거나 접근 권한이 없을 경우
     */
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션
    public MyReservationDetailDto findMyReservationDetail(Long userNo, Long resNo) {
        log.debug("findMyReservationDetail Service - userNo: {}, resNo: {}", userNo, resNo);


        // 1. 예약 정보 조회 (findById 사용)
        Reservation reservation = myReservationRepository.findById(resNo)
                .orElseThrow(() -> {
                    log.warn("Reservation not found for resNo: {}", resNo);
//                    return new NotFoundException("예약 번호 " + resNo + "에 해당하는 예약 정보를 찾을 수 없습니다.");
                    return null;
                });

        // 2. 사용자 권한 확인
        if (!reservation.getUserNo().equals(userNo)) {
            log.warn("Authorization denied for userNo {} trying to access reservation resNo {}", userNo, resNo);
//            throw new AuthorizationDeniedException("해당 예약 정보에 접근할 권한이 없습니다.");
        }

        // 3. 좌석 정보 조회
        Seat seat = seatRepository.findById(reservation.getSeatNo())
                .orElseGet(() -> {
                    log.warn("Seat not found for seatNo: {} (referenced by resNo: {})", reservation.getSeatNo(), resNo);

                    // 좌석 정보가 없어도 예약을 보여줘야 할 수 있으므로, 기본 Seat 객체나 null 처리 고려
                    Seat unknownSeat = new Seat();
                    unknownSeat.setSeatNm("알 수 없는 좌석");
                    unknownSeat.setSeatSort("-");
                    return unknownSeat;
                    // 또는 return null; DTO 빌더에서 null 체크 필요
                });


        // 4. DTO 생성 및 반환
        LocalDateTime now = LocalDateTime.now();
        String statusCode = calculateStatusCode(reservation, now);
        String displayStatus = getDisplayStatus(statusCode);
        boolean canModify = calculateCanModify(reservation, now); // 예약 변경 가능 여부 로직
        boolean canCancel = calculateCanCancel(reservation, now); // 예약 취소 가능 여부 로직

        // MyReservationDetailDto 빌더 사용
        return MyReservationDetailDto.builder()
                .resNo(reservation.getResNo())
                .resDt(reservation.getResDt())
                .statusCode(statusCode)
                .displayStatus(displayStatus)
                .seatNm(seat.getSeatNm())
                .seatSort(seat.getSeatSort())               // SeatSort가 null일 수 있음에 유의
                .resStart(reservation.getResStart())
                .resEnd(reservation.getResEnd())
                .resPrice(reservation.getResPrice())        // 필요시 Long/BigDecimal 변환
                .dcPrice(reservation.getDcPrice())          // 필요시 Long/BigDecimal 변환 및 null 처리
                .totalPrice(reservation.getTotalPrice())    // 필요시 Long/BigDecimal 변환
                // --- 결제 정보 ---
                // 현재 Reservation 엔티티에 없으므로 임시로 null 또는 기본값 처리
                // TODO: Payment 테이블 연동 시 실제 값 조회 로직 추가 필요
                .paymentMethod(null) // 예: paymentService.getPaymentMethod(resNo)
                .paymentApproveDt(null) // 예: paymentService.getPaymentApproveDt(resNo)
                // --- 액션 플래그 ---
                .canModify(canModify)
                .canCancel(canCancel)
                .build();
    }

    private String calculateStatusCode(Reservation reservation, LocalDateTime now) {

        if (Boolean.FALSE.equals(reservation.getResStatus())) { // resStatus가 false (0) 이면 취소
            return "3"; // 취소됨
        } else if (Boolean.TRUE.equals(reservation.getResStatus())) { // resStatus가 true (1) 이면 예약완료 상태
            if (reservation.getResEnd() != null && reservation.getResEnd().isAfter(now)) {
                return "1"; // 이용예정
            } else {
                return "2"; // 이용완료
            }
        }

        // resStatus가 null이거나 예상치 못한 경우 (정책에 따라 처리)
        log.warn("Unknown reservation status for resNo: {}, resStatus: {}", reservation.getResNo(), reservation.getResStatus());
        return "UNKNOWN"; // 또는 기본 상태
    }

    // --- 표시용 상태 문자열 변환 ---
    private String getDisplayStatus(String statusCode) {
        switch (statusCode) {
            case "1": return "이용예정";
            case "2": return "이용완료";
            case "3": return "취소됨";
            default: return "상태확인필요";
        }
    }

    // --- 예약 변경 가능 여부 계산 로직 (예시) ---
    private boolean calculateCanModify(Reservation reservation, LocalDateTime now) {
        // 예시: 이용 시작 전이고, 취소되지 않은 예약만 변경 가능
        return "1".equals(calculateStatusCode(reservation, now)) && reservation.getResStart().isAfter(now);
        // TODO: 실제 비즈니스 규칙에 맞게 수정
    }

    // --- 예약 취소 가능 여부 계산 로직 (예시) ---
    private boolean calculateCanCancel(Reservation reservation, LocalDateTime now) {
        // 예시: 이용 시작 전이고, 취소되지 않은 예약만 취소 가능
        return "1".equals(calculateStatusCode(reservation, now)) && reservation.getResStart().isAfter(now);
        // TODO: 실제 비즈니스 규칙에 맞게 수정 (취소 정책 반영 - 예: 1시간 전까지만 가능)
    }

}
