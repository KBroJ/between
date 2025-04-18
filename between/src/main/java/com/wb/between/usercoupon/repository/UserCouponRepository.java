package com.wb.between.usercoupon.repository;

import com.wb.between.usercoupon.domain.UserCoupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    // 사용가능 쿠폰
    @Query("SELECT uc FROM UserCoupon uc LEFT JOIN FETCH uc.coupon c " +
            "WHERE uc.user.userNo = :userNo " +
            "AND uc.useAt = :useAt " +
            "AND (:startDate IS NULL OR uc.coupon.cpnStartDt >= :startDate) " + // 시작일 조건
            "AND (:endDatePlusOne IS NULL OR uc.coupon.cpnStartDt < :endDatePlusOne) " + // 종료일 조건
            "ORDER BY uc.issueDt DESC")
    List<UserCoupon> findByUserCoupon(@Param("userNo") Long userNo,
                                      @Param("useAt") String useAt,
                                      @Param("startDate") LocalDateTime startDate,
                                      @Param("endDatePlusOne") LocalDateTime endDatePlusOne);

    /**
     * 사용가능한 쿠폰 페이지
     * 기본 조회는 Fetch 처리로
     * 개수만 세면 되는 count 쿼리는 fetch 제외로 불필요한 엔티티 데이터 조회 방지
     */
    @Query(value = """
            SELECT uc FROM UserCoupon uc LEFT JOIN FETCH uc.coupon c
            WHERE uc.user.userNo = :userNo
            AND uc.useAt = :useAt
            AND c.cpnStartDt     <= :now
            AND c.cpnEndDt       >= :now             
            AND (:startDate IS NULL OR uc.coupon.cpnStartDt >= :startDate)
            AND (:endDatePlusOne IS NULL OR uc.coupon.cpnStartDt < :endDatePlusOne)
            ORDER BY uc.issueDt DESC
            """,
            countQuery = """
             SELECT COUNT(uc)
                FROM UserCoupon uc
                JOIN uc.coupon c
                WHERE uc.user.userNo   = :userNo
                  AND uc.useAt         = 'N'
                  AND c.cpnStartDt     <= :now
                  AND c.cpnEndDt       >= :now
                  AND (:startDate      IS NULL OR c.cpnStartDt >= :startDate)
                  AND (:endDatePlusOne IS NULL OR c.cpnStartDt <  :endDatePlusOne)
            """)
    Page<UserCoupon> findByUserCouponPage(@Param("userNo") Long userNo,
                                          @Param("useAt") String useAt,
                                          @Param("now") LocalDateTime now,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDatePlusOne") LocalDateTime endDatePlusOne,
                                          Pageable pageable);

    // 기간만료 쿠폰
    @Query("SELECT uc FROM UserCoupon uc LEFT JOIN FETCH uc.coupon c " +
            "WHERE uc.user.userNo = :userNo " +
            "AND uc.useAt = :useAt " +
            "AND (:startDate IS NULL OR uc.coupon.cpnStartDt >= :startDate) " + // 시작일 조건
            "AND (:endDatePlusOne IS NULL OR uc.coupon.cpnStartDt < :endDatePlusOne) " + // 종료일 조건
            "ORDER BY uc.issueDt DESC")
    List<UserCoupon> findExpiredCouponsWithDateFilter(
            @Param("userNo") Long userNo,
            @Param("useAt") String useAt,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDatePlusOne") LocalDateTime endDatePlusOne
    );

    // 기간만료 쿠폰
    @Query(value = """
            SELECT uc FROM UserCoupon uc LEFT JOIN FETCH uc.coupon c
            WHERE uc.user.userNo = :userNo
            AND c.cpnEndDt       < :now
            AND (:startDate IS NULL OR uc.coupon.cpnStartDt >= :startDate)
            AND (:endDatePlusOne IS NULL OR uc.coupon.cpnStartDt < :endDatePlusOne)
            ORDER BY uc.issueDt DESC
            """,
            countQuery = """
             SELECT COUNT(uc)
                FROM UserCoupon uc
                JOIN uc.coupon c
                WHERE uc.user.userNo   = :userNo
                  AND c.cpnEndDt       < :now
                  AND (:startDate      IS NULL OR c.cpnStartDt >= :startDate)
                  AND (:endDatePlusOne IS NULL OR c.cpnStartDt <  :endDatePlusOne)
            """)
    Page<UserCoupon> findExpiredCouponsWithDateFilterPage(
            @Param("userNo") Long userNo,
            @Param("now") LocalDateTime now,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDatePlusOne") LocalDateTime endDatePlusOne,
            Pageable pageable
    );
}
