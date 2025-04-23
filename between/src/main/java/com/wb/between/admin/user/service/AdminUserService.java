package com.wb.between.admin.user.service;

import com.wb.between.admin.user.dto.UserReservationDto;
import com.wb.between.admin.user.dto.UserDetailDto;
import com.wb.between.reservation.reserve.domain.Reservation;
import com.wb.between.reservation.reserve.repository.ReservationRepository;
import com.wb.between.reservation.seat.domain.Seat;
import com.wb.between.reservation.seat.repository.SeatRepository;
import com.wb.between.user.domain.User;
import com.wb.between.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor // final 필드 생성자 주입
@Transactional(readOnly = true) // 조회 중심 서비스이므로 readOnly 설정
public class AdminUserService {

    private final UserRepository userRepository; // 사용자 레포지토리 주입
    private final ReservationRepository reservationRepository; // 예약 레포지토리 주입
    private final SeatRepository seatRepository; // 좌석 리포지토리 주입
    private static final int RECENT_RESERVATION_COUNT = 5; // 보여줄 최근 예약 개수

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
        List<UserReservationDto> userReservationDtos = recentReservations.stream()
                .map(this::mapToReservationDto)
                .collect(Collectors.toList());

        // 4. 사용자 엔티티와 예약 DTO 목록 -> 최종 UserDetailDTO 변환 및 반환
        return mapToUserDetailDTO(user, userReservationDtos);
    }


// --- 데이터 변환 메소드 ---
    private UserDetailDto mapToUserDetailDTO(User user, List<UserReservationDto> reservations) {
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

    private UserReservationDto mapToReservationDto(Reservation reservation) {

        // 예약 상태(Boolean) -> 문자열("완료", "취소") 변환
        String statusString;

        if (reservation.getResStatus() == null) {
            statusString = "확인 불가"; // 또는 "진행중" 등 비즈니스 로직에 맞게
        } else {
            statusString = Boolean.TRUE.equals(reservation.getResStatus()) ? "완료" : "취소";
        }

        return UserReservationDto.builder()
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
    // (이전 답변의 updateUserAccountSettings, deleteUser 메소드 참고)

}
