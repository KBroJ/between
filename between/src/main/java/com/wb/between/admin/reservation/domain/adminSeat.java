package com.wb.between.admin.reservation.domain;

import com.wb.between.admin.price.domain.Price;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.YesNoConverter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "seat")
public class adminSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seatNo;

    @Column(nullable = false, length = 100)
    private String seatNm;

    @Column(name = "useAt", nullable = false, length = 1)
    @Convert(converter = YesNoConverter.class)
    private boolean useAt;

    @Column(length = 100)
    private String seatSort;

    @Column(name = "gridRow" ,length = 50)
    private String gridRow;

    @Column(name = "gridColumn", length = 50)
    private String gridColumn;

    @Column(name = "floor")
    private Integer floor;

    // Price Entity와 일대다 관계 설정
    // mappedBy: Price Entity의 'seat' 필드가 이 관계의 주인임을 명시
    // cascade: Seat이 저장/삭제될 때 Price도 함께 처리 (ALL: 모든 작업, PERSIST: 저장 시)
    // orphanRemoval: Seat에서 Price가 제거되면 DB에서도 삭제
    @OneToMany(mappedBy = "seat", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Price> prices = new ArrayList<>();

    @Column(length = 100)
    private String register;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createDt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateDt;


}
