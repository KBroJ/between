package com.wb.between.admin.coupon.repository;

import com.wb.between.coupon.domain.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminCouponRepository extends JpaRepository<Coupon, Long> {

    @Query("SELECT c FROM Coupon c WHERE c.cpnNm LIKE CONCAT('%', :searchCpnNm, '%')")
    Page<Coupon> findCouponsWithFilter(Pageable pageable,
                                       @Param("searchCpnNm") String searchCouponName);
}
