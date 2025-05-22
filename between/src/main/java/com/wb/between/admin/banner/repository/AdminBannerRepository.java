package com.wb.between.admin.banner.repository;

import com.wb.between.banner.domain.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminBannerRepository extends JpaRepository<Banner, Long> {

    @Query("SELECT b FROM Banner b WHERE b.bTitle LIKE CONCAT('%', :searchBannerName, '%')")
    Page<Banner> findBannerList(Pageable pageable, @Param("searchBannerName") String searchBannerName);
}
