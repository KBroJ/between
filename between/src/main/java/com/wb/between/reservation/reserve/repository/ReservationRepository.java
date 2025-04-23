package com.wb.between.reservation.reserve.repository;


import com.wb.between.reservation.reserve.domain.Reservation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * 특정 좌석, 특정 날짜에 시작하는 확정된(resStatus=true) 예약을 조회합니다.
     *
     * @param seatNo     좌석 번호 (Long 타입 가정)
     * @param targetDate 조회할 날짜 (LocalDate)
     * @param status     조회할 예약 상태 (Boolean 타입 가정 - true: 확정)
     * @return 예약 목록
     */
    @Query("SELECT r FROM Reservation r WHERE r.seatNo = :seatNo AND FUNCTION('DATE', r.resStart) = :targetDate AND r.resStatus = :status")
    List<Reservation> findBySeatNoAndDateAndStatus(@Param("seatNo") Long seatNo, @Param("targetDate") LocalDate targetDate, @Param("status") Boolean status);
    // !!! 중요: Reservation Entity의 resStatus 타입이 Boolean이 아니라면 위 :status 부분과 파라미터 타입을 맞춰야 함 !!!

    // (겹치는 예약 카운트 메소드 - 이전 답변 참고)
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.seatNo = :seatNo AND r.resStatus = true AND r.resEnd > :newResStart AND r.resStart < :newResEnd")
    long countOverlappingReservations(@Param("seatNo") Long seatNo, @Param("newResStart") LocalDateTime newResStart, @Param("newResEnd") LocalDateTime newResEnd);

    /**
     * 특정 날짜(LocalDate)에 시작하는 확정된(resStatus=true) 예약을 조회합니다.
     * (resStart 필드의 날짜 부분만 비교)
     *
     * @param targetDate 조회할 날짜 (LocalDate)
     * @param status     조회할 예약 상태 (Boolean 타입 가정 - true: 확정)
     * @return 예약 목록
     */
    @Query("SELECT r FROM Reservation r WHERE FUNCTION('DATE', r.resStart) = :targetDate AND r.resStatus = :status")
    List<Reservation> findByDateAndStatus(@Param("targetDate") LocalDate targetDate, @Param("status") Boolean status);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.seatNo = :seatNo AND r.resStatus = true AND r.resEnd > :newResStart AND r.resStart < :newResEnd AND r.resNo != :excludeResNo")
    long countOverlappingReservationsExcludingSelf(
            @Param("seatNo") Long seatNo,
            @Param("newResStart") LocalDateTime newResStart,
            @Param("newResEnd") LocalDateTime newResEnd,
            @Param("excludeResNo") Long excludeResNo // 제외할 예약 번호 추가
    );


    // 특정 사용자의 예약을 'resDt'(예약 생성일시) 기준 내림차순으로 조회하는 메소드
    // Pageable 객체를 통해 조회할 개수(limit)와 시작점(offset) 등을 지정할 수 있음 (예: 최근 5개)
    List<Reservation> findByUserNoOrderByResDtDesc(Long userNo, Pageable pageable);


}

