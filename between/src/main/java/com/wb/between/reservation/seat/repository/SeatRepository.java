package com.wb.between.reservation.seat.repository;

import com.wb.between.reservation.seat.domain.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    // useAt 필드가 true ('Y')인 좌석 목록 조회
    List<Seat> findByUseAtTrue();

    // 또는 JPQL 사용:
    // @Query("SELECT s FROM Seat s WHERE s.useAt = true")
    // List<Seat> findActiveSeats();
}