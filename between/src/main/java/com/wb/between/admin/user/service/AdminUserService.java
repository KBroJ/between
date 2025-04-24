package com.wb.between.admin.user.service;

import com.wb.between.admin.user.dto.UserDetailReservationListDto;
import com.wb.between.admin.user.dto.UserDetailDto;
import com.wb.between.admin.user.dto.UserFilterParamsDto;
import com.wb.between.admin.user.dto.UserListDto;
import com.wb.between.admin.user.repository.AdminUserRepository;
import com.wb.between.reservation.reserve.domain.Reservation;
import com.wb.between.reservation.reserve.repository.ReservationRepository;
import com.wb.between.reservation.seat.domain.Seat;
import com.wb.between.reservation.seat.repository.SeatRepository;
import com.wb.between.user.domain.User;
import com.wb.between.user.repository.UserRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor        // final 필드 생성자 주입
@Transactional(readOnly = true) // 조회 중심 서비스이므로 readOnly 설정
public class AdminUserService {

    private final AdminUserRepository adminUserRepository; // 관리자_회원 관리 레포지토리 주입
    private final UserRepository userRepository; // 사용자 레포지토리 주입
    private final ReservationRepository reservationRepository; // 예약 레포지토리 주입
    private final SeatRepository seatRepository; // 좌석 리포지토리 주입
    private static final int RECENT_RESERVATION_COUNT = 5; // 보여줄 최근 예약 개수


    /**
     * 필터링 조건과 페이징 정보를 기반으로 사용자 목록을 조회합니다.
     * @param filterParams 필터링 조건 DTO
     * @param pageable 페이징 및 정렬 정보
     * @return Page<UserListDto> 형태의 사용자 목록
     */
    public Page<UserListDto> getUsers(UserFilterParamsDto filterParams, Pageable pageable) {
        log.info("사용자 목록 조회 서비스 시작. Params: {}, Pageable: {}", filterParams, pageable);

        // 1. Specification 생성 (서비스 내 메소드 호출)
        Specification<User> spec = buildUserSpecification(filterParams); // 내부 메소드 호출

        // 2. 회원목록 조회
        Page<User> userPage = adminUserRepository.findAll(spec, pageable);
        log.info("조회된 사용자 수: {}", userPage.getTotalElements());

        // 3. Page<User> -> Page<UserListDto> 변환
        // mapToUserListDto 메소드를 사용하여 User 객체를 UserListDto로 변환
        Page<UserListDto> userListDtoPage = userPage.map(this::mapToUserListDto);

        return userListDtoPage;
    }

    /**
     * UserFilterParamsDto를 기반으로 Specification<User> 객체를 생성합니다.
     * (람다식 대신 익명 클래스 사용)
     * @param filterParams 필터링 조건 DTO
     * @return 생성된 Specification 객체
     */
    private Specification<User> buildUserSpecification(UserFilterParamsDto filterParams) {
        // Specification 인터페이스를 구현하는 익명 클래스 반환
        return new Specification<User>() {
            @Override
            public Predicate toPredicate(Root<User> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                // Predicate 객체들을 담을 리스트 생성
                List<Predicate> predicates = new ArrayList<>();

                // 1. 가입일 (날짜 범위) 필터링
                try {
                    // 조건 중 시작일자가 있을 경우
                    if (StringUtils.hasText(filterParams.getStartDate())) {
                        LocalDateTime startDateTime = LocalDate.parse(filterParams.getStartDate()).atStartOfDay();
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createDt"), startDateTime));
                    }
                    // 조건 중 종료일자가 있을 경우
                    if (StringUtils.hasText(filterParams.getEndDate())) {
                        LocalDateTime endDateTime = LocalDate.parse(filterParams.getEndDate()).atTime(LocalTime.MAX);
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createDt"), endDateTime));
                    }
                } catch (DateTimeParseException e) {
                    log.error("잘못된 날짜 형식입니다: {}", e.getMessage());
                }

                // 2. 검색어 필터링(이메일/이름/연락처)
                if (StringUtils.hasText(filterParams.getSearchText()) && StringUtils.hasText(filterParams.getSearchType())) {
                    String keywordPattern = "%" + filterParams.getSearchText().toLowerCase() + "%";
                    switch (filterParams.getSearchType()) {
                        case "email":
                            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), keywordPattern));
                            break;
                        case "name":
                            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), keywordPattern));
                            break;
                        case "phone":
                            predicates.add(criteriaBuilder.like(root.get("phoneNo"), "%" + filterParams.getSearchText() + "%"));
                            break;
                    }
                }

                // 3. 회원 상태 필터링(정상/휴면)
                if (StringUtils.hasText(filterParams.getStatus())) {
                    String dbStatus = "정상".equals(filterParams.getStatus()) ? "일반" : filterParams.getStatus();
                    predicates.add(criteriaBuilder.equal(root.get("userStts"), dbStatus));
                }

                // 4. 회원 등급 필터링
                if (StringUtils.hasText(filterParams.getGrade())) {
                    predicates.add(criteriaBuilder.equal(root.get("authCd"), filterParams.getGrade()));
                }

                // 생성된 모든 Predicate들을 AND 연산으로 결합하여 최종 Predicate 반환
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            }
        };
    }

    private UserListDto mapToUserListDto(User user) {

        String mappedStatus = "일반".equals(user.getUserStts()) ? "정상" : user.getUserStts();

        return UserListDto.builder()
                .userNo(user.getUserNo())
                .email(user.getEmail())
                .name(user.getName())
                .phoneNo(PhoneNumber(user.getPhoneNo()))
                .createDt(user.getCreateDt())
                .userStts(mappedStatus)
                .authCd(user.getAuthCd())
                .build();
    }

// ========================= 회원 상세 정보 조회 =========================

    /**
     * 특정 사용자 상세 정보 및 최근 예약을 조회하여 DTO로 반환
     * @param userNo 조회할 사용자 ID (userNo)
     * @return UserDetailDTO
     */
    public UserDetailDto getUserDetail(Long userNo) {
        // 1. 사용자 조회
        User user = userRepository.findByUserNo(userNo)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다. userNo: " + userNo));

        // 2. 최근 예약 5개 조회
        Pageable pageable = PageRequest.of(0, RECENT_RESERVATION_COUNT);
        List<Reservation> recentReservations = reservationRepository.findByUserNoOrderByResDtDesc(userNo, pageable);

        // 3. 예약 엔티티 목록 -> 예약 목록 변환
        List<UserDetailReservationListDto> userDetailReservationListDtos = recentReservations.stream()
                .map(this::mapToReservationDto)
                .collect(Collectors.toList());

        // 4. 사용자 엔티티와 예약 DTO 목록 -> 최종 UserDetailDTO 변환 및 반환
        return mapToUserDetailDTO(user, userDetailReservationListDtos);
    }


// --- 데이터 변환 메소드 ---
    private UserDetailDto mapToUserDetailDTO(User user, List<UserDetailReservationListDto> reservations) {
        // DB 상태값("일반") -> 화면 표시값("정상") 매핑
        String mappedStatus = "일반".equals(user.getUserStts()) ? "정상" : user.getUserStts();

        return UserDetailDto.builder()
                .userNo(user.getUserNo())
                .email(user.getEmail())
                .name(user.getName())
                .phoneNo(PhoneNumber(user.getPhoneNo())) // - 적용
                .authCd(user.getAuthCd()) // 등급은 DB값 그대로 사용
                .userStts(mappedStatus) // 매핑된 상태값 사용
                .createDt(user.getCreateDt())
                .recentReservations(reservations)
                .build();
    }

    private UserDetailReservationListDto mapToReservationDto(Reservation reservation) {

        // 예약 상태(Boolean) -> 문자열("완료", "취소") 변환
        String statusString;

        if (reservation.getResStatus() == null) {
            statusString = "확인 불가"; // 또는 "진행중" 등 비즈니스 로직에 맞게
        } else {
            statusString = Boolean.TRUE.equals(reservation.getResStatus()) ? "완료" : "취소";
        }

        return UserDetailReservationListDto.builder()
                .resNo(reservation.getResNo())
                .resDt(reservation.getResDt())
                .seatNm(mapSeatNoToSeatNoNm(reservation.getSeatNo())) // 좌석번호 -> 좌석명 변환
                .resStatusNm(statusString)
                .resStart(reservation.getResStart())
                .resEnd(reservation.getResEnd())
                .build();
    }

    // 좌석 번호 -> 좌석명 변환 (실제 로직 구현 필요)
    private String mapSeatNoToSeatNoNm(Long seatNo) {

        if (seatNo != null) {

            // 좌석 정보 조회
            Seat seat = seatRepository.findById(seatNo)
                    .orElseGet(() -> {
                        log.warn("Seat not found for seatNo: {}", seatNo);

                        // 좌석 정보가 없어도 예약을 보여줘야 할 수 있으므로, 기본 Seat 객체나 null 처리 고려
                        Seat unknownSeat = new Seat();
                        unknownSeat.setSeatNm("알 수 없는 좌석");
                        unknownSeat.setSeatSort("-");
                        return unknownSeat;
                    });

            return seat.getSeatNm();
        }

        return "알 수 없음";
    }


    // 휴대폰 번호 커스텀
    private String PhoneNumber(String phoneNo) {
        if (phoneNo != null && phoneNo.length() == 11) {
            return phoneNo.substring(0, 3) + "-" +  phoneNo.substring(3, 7)+ "-" + phoneNo.substring(7, 11);
        } else if (phoneNo != null && phoneNo.length() == 10) {
            return phoneNo.substring(0, 3) + "-" + phoneNo.substring(3, 6) + "-" + phoneNo.substring(6, 10);
        }
        return phoneNo;
    }

    // TODO: 회원 정보 수정(등급/상태) 및 탈퇴 처리 로직 구현 필요

}
