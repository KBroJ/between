package com.wb.between.mypage.repository;

import com.wb.between.mypage.dto.MyReservationDto;
import com.wb.between.reservation.reserve.domain.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface MyReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * 사용자의 예약 내역을 조건에 맞게 조회하여 MyReservationDto 페이지로 반환합니다.
     *
     * @param userNo        사용자 번호
     * @param startDateTime 조회 시작 일시 (resDt 기준)
     * @param endDateTime   조회 종료 일시 (resDt 기준)
     * @param tabStatus     조회할 예약 상태 코드 ('1': 예정, '2': 지난, '3': 취소)
     * @param now           현재 시각 (예정/지난 예약 구분용)
     * @param pageable      페이징 정보
     * @return 페이징 처리된 MyReservationDto 리스트
     */
    @Query(
            "SELECT NEW com.wb.between.mypage.dto.MyReservationDto(" +
            "r.resNo, r.resDt, s.seatNm, r.resStart, r.resEnd, r.totalPrice, r.resPrice, r.dcPrice, " +
            "CASE " +
            "  WHEN r.resStatus = true AND r.resEnd > :now THEN '1' " +     // 예약완료 & 종료시간 미래 -> '1' (예정)
            "  WHEN r.resStatus = true AND r.resEnd <= :now THEN '2' " +    // 예약완료 & 종료시간 과거/현재 -> '2' (지난)
            "  WHEN r.resStatus = false THEN '3' " +                        // 예약취소 -> '3' (취소)
            "  ELSE 'UNKNOWN' " +                                           // 예외 케이스 처리
            "END" +
            ") " +
            "FROM Reservation r JOIN Seat s ON r.seatNo = s.seatNo " +      // Seat 테이블 조인 (좌석 이름 가져오기)
            "WHERE r.userNo = :userNo " +                                   // 1. 로그인 사용자 필터링
            "  AND r.resDt BETWEEN :startDateTime AND :endDateTime " +      // 2. 예약일(resDt) 기준 날짜 필터링
            "  AND (CASE " +
            "         WHEN r.resStatus = true AND r.resEnd > :now THEN '1' " +
            "         WHEN r.resStatus = true AND r.resEnd <= :now THEN '2' " +
            "         WHEN r.resStatus = false THEN '3' " +
            "         ELSE 'UNKNOWN' " +
            "       END) = :tabStatus"                                      // 3. 탭 상태 필터링
    )
    Page<MyReservationDto> findUserReservationsWithStatus(
            @Param("userNo") Long userNo,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("tabStatus") String tabStatus,
            @Param("now") LocalDateTime now,
            Pageable pageable); // 페이징 및 정렬 정보

    /**
     * 마이페이지 예약 내역 조회 (JPQL @Query 사용)
     */
/*
    @Query("SELECT r FROM Reservation r, Seat s " +
            "WHERE r.userNo = :user.userNo " +
            "AND r.seatNo = s.seatNo " +
            "AND ( " +
            "  (:tab = 'upcoming' AND r.resStatus = true AND r.resEnd > :now) OR " +
            "  (:tab = 'past' AND r.resStatus = true AND r.resEnd <= :now) OR " +
            "  (:tab = 'cancelled' AND r.resStatus = false) " +
            ") " +
            "AND (:startDateTime IS NULL OR r.resDt >= :startDateTime) " +
            "AND (:endDateTime IS NULL OR r.resDt <= :endDateTime) ")
    Page<Reservation> findMyReservationsWithJpql(
            @Param("user") User user,
            @Param("tab") String tab,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
*/
}
