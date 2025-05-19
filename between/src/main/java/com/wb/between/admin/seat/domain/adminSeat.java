package com.wb.between.admin.seat.domain;

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
import java.util.Objects;

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

    // 이 좌석 가격 정보를 추가하고 양방향 관계 설정
    public void addPrice(Price price) {
        Objects.requireNonNull(price, "추가할 Price 객체는 null일 수 없습니다.");

        this.prices.add(price); // Seat 객체의 prices 리스트에 Price 객체를 추가합니다.
        price.setSeat(this);    // Price 객체에도 현재 Seat 객체를 연결합니다.

    }
    
    // 이 좌석에서 특정 가격 정보를 제거하고 양방향 관계를 해제
    public void deletePrice(Price price) {
        Objects.requireNonNull(price, "제거할 Price 객체는 null일 수 없습니다.");
        if (this.prices.contains(price)) { // 리스트에 해당 Price 객체가 있는지 확인
            this.prices.remove(price); // 1. Seat 객체의 prices 리스트에서 Price 객체를 제거
            price.setSeat(null);      // 2. Price 객체에서 Seat 연결을 해제
        }
    }

    // 가격, 양방향 관계 모두 제거
    public void clearPrices() {
        // prices 리스트를 순회하면서 removePrice를 호출하면 ConcurrentModificationException 발생 가능성이 있으므로,
        // 안전하게 복사본을 만들거나, 아래처럼 Price 쪽의 연결만 끊고 리스트를 비우기
        for (Price price : new ArrayList<>(this.prices)) { // 리스트 복사본으로 안전하게 순회
            price.setSeat(null); // Price 쪽에서 Seat 연결 해제
        }
        this.prices.clear(); // Seat의 prices 리스트 비우기
    }


}
