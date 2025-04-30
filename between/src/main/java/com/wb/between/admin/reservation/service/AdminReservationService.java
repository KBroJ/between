package com.wb.between.admin.reservation.service;

import com.wb.between.admin.reservation.dto.ReservationFilterParamsDto;
import com.wb.between.admin.reservation.dto.ReservationListDto;
import com.wb.between.admin.reservation.dto.SeatDto;
import com.wb.between.admin.reservation.repository.AdminReservationRepository;
import com.wb.between.reservation.reserve.domain.Reservation;
import com.wb.between.reservation.seat.domain.Seat;
import com.wb.between.reservation.seat.repository.SeatRepository;
import com.wb.between.user.domain.User;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor        // final 필드 생성자 주입
@Transactional(readOnly = true) // 조회 중심 서비스이므로 readOnly 설정
public class AdminReservationService {

    private final AdminReservationRepository adminReservationRepository; // 예약 관련 데이터베이스 작업을 위한 리포지토리
    private final SeatRepository seatRepository;

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

    // --- 예약 상세 정보 조회 로직 (추후 구현) ---
    // public ReservationDetailDto getReservationDetail(Long resNo) { ... }

}
