package com.wb.between.coupon.domain;

import com.wb.between.usercoupon.domain.UserCoupon;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Builder
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "usercoupon")
@EqualsAndHashCode(of = "cpNo")
@Table(name = "coupon")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cpNo;

    @Column(length = 30)
    private String cpnNm;

    @Column(length = 10)
    private String discount;

    @Column(length = 5)
    private String discountAt;

    private Long cpnLimit;

    private LocalDateTime createDate;

    private LocalDateTime cpnStartDt;

    private LocalDateTime cpnEndDt;

    @Column(length = 100)
    private String cpnDsc;

    @Column(length = 1)
    private String activeYn;

    @OneToMany(mappedBy = "coupon", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<UserCoupon> usercoupon = new HashSet<>();
}
