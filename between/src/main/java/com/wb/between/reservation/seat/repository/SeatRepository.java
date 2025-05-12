package com.wb.between.reservation.seat.repository;

import com.wb.between.reservation.seat.domain.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    // useAt 필드가 true ('Y')인 좌석 목록 조회
    List<Seat> findByUseAtTrue();

    // 특정 층 사용 가능한 좌석 목록
    List<Seat> findByFloorAndUseAtTrue(Integer floor);

    // 또는 JPQL 사용:
    // @Query("SELECT s FROM Seat s WHERE s.useAt = true")
    // List<Seat> findActiveSeats();

    @Query("SELECT DISTINCT s.floor FROM Seat s WHERE s.useAt = 'Y' ORDER BY s.floor ASC")
    List<Integer> findDistinctActiveFloors();
}