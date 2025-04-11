package com.wb.between.usercoupon.repository;

import com.wb.between.usercoupon.domain.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    @Query("SELECT uc FROM UserCoupon uc LEFT JOIN FETCH uc.coupon c " +
            "WHERE uc.user.userNo = :userNo AND uc.useAt = :useAt ORDER BY uc.issueDt DESC")
    List<UserCoupon> findByUserCoupon(@Param("userNo") Long userNo, @Param("useAt") String useAt);
}
