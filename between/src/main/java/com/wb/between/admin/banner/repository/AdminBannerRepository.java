package com.wb.between.admin.banner.repository;

import com.wb.between.banner.domain.Banner;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminBannerRepository extends JpaRepository<Banner, Long> {

}
