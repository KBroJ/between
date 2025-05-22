package com.wb.between.banner.repository;

import com.wb.between.banner.domain.Banner;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    //사용여부 기준, 정렬 조회
    List<Banner> findByUseAt(String useAt, Sort sort);

    @Query("SELECT b FROM Banner b " +
            "WHERE b.useAt = :useAt " +
            "AND b.startDt <= :now AND b.endDt >= :now " +
            "ORDER BY b.sortOrder ASC")
    List<Banner> findBannerByUseAt(@Param("useAt") String useAt, @Param("now") LocalDateTime now);
}
