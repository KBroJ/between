package com.wb.between.admin.seat.repository;

import com.wb.between.admin.seat.domain.adminSeat;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface adminSeatRepository extends JpaRepository<adminSeat, Long> {

    // 사용 가능한 좌석 목록 조회 쿼리
    List<adminSeat> findByFloorAndUseAtTrue(Integer floor);

    List<adminSeat> findByUseAtTrue();

    // 목록 (중복 없이 오름차순)
    @Query("SELECT DISTINCT s.floor FROM Seat s WHERE s.useAt = true ORDER BY s.floor ASC")
    List<Integer> findDistinctActiveFloors();

    // 특정 층 좌석 조회 시 가격 정보 함께 로딩
    @Query("SELECT DISTINCT s FROM adminSeat s LEFT JOIN FETCH s.prices WHERE s.floor = :floor AND s.useAt = true ORDER BY s.seatNo ASC")
    List<adminSeat> findByFloorAndUseAtTrueWithPrices(@Param("floor") Integer floor);

    // 모든 활성 좌석 조회 시 가격 정보 함께 로딩
    @Query("SELECT DISTINCT s FROM adminSeat s LEFT JOIN FETCH s.prices WHERE s.useAt = true ORDER BY s.floor ASC, s.seatNo ASC")
    List<adminSeat> findByUseAtTrueWithPrices();
}
