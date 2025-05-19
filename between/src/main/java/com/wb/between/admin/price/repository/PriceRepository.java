package com.wb.between.admin.price.repository;

import com.wb.between.admin.price.domain.Price;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PriceRepository extends JpaRepository<Price, Long> {

    @Modifying
    @Query("DELETE FROM Price p WHERE p.seat.seatNo = :seatNo")
    void deleteAllBySeatNo(@Param("seatNo") Long seatNo);

    List<Price> findBySeat_SeatNo(Long seatNo);



}
