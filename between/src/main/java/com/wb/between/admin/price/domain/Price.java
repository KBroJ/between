package com.wb.between.admin.price.domain;

import com.wb.between.admin.seat.domain.adminSeat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "price")
public class Price {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long priceNo;

    @ManyToOne(fetch = FetchType.LAZY) // LAZY 로딩 권장
    @JoinColumn(name = "seatNo", nullable = false) // DB의 seatNo 컬럼과 매핑 (FK)
    private adminSeat seat;

    @Column(length = 15)
    private String price;

    @Column(length = 15)
    private String type;

    public Price(adminSeat seat, String price, String type) {
        this.seat = seat;
        this.price = price;
        this.type = type;
    }


}
