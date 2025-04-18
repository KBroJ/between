package com.wb.between.usercoupon.domain;

import com.wb.between.coupon.domain.Coupon;
import com.wb.between.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
//@ToString(exclude = "user, coupon")
@EqualsAndHashCode(of = "userCpNo")
@Builder
@Table(name = "usercoupon")
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userCpNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userNo", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cpNo", nullable = false)
    private Coupon coupon;

    @Column(nullable = false, insertable = false, updatable = false)
    private LocalDateTime issueDt;

    @Column(length = 10)
    private String useAt;

    private LocalDateTime useDt;
}
