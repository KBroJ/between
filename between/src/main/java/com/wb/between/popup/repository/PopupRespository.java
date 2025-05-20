package com.wb.between.popup.repository;

import com.wb.between.popup.domain.Popups;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface PopupRespository extends JpaRepository<Popups, Integer> {

    @Query("SELECT p FROM Popups p " +
            "WHERE p.useAt = :useAt " +
            "AND :currentDate BETWEEN p.startDt AND p.endDt")
    List<Popups> findByUseAt( @Param("useAt") String useAt,
                              @Param("currentDate") LocalDateTime currentDt,
                              Sort displayOrder);
}
