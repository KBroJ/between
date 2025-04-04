package com.wb.between.reservation.seat.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.YesNoConverter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "Seat") // 테이블명 대문자 주의
public class Seat {
    @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY) // DB에서 자동 증가 시 사용
    private Long seatNo; // PK, Long 타입

    @Column(nullable = false, length = 100)
    private String seatNm; // 좌석 이름

    @CreationTimestamp // 엔티티 생성 시 자동으로 현재 시간 입력
    @Column(nullable = false, updatable = false) // 수정 불가
    private LocalDateTime createDt;

    @UpdateTimestamp // 엔티티 수정 시 자동으로 현재 시간 입력
    private LocalDateTime updateDt;

    @Column(nullable = false, length = 100)
    private String register; // 등록자 정보

    // --- !!! useAt 필드 수정 !!! ---
    @Column(name = "useAt", nullable = false, length = 1)
    @Convert(converter = YesNoConverter.class) // !!! 이 줄 추가 !!!
    private boolean useAt;

    @Column(length = 100)
    private String seatSort; // 좌석 종류 (DB 컬럼명 그대로 사용)

    // --- 위치 정보 필드 추가 ---
    @Column(name = "grid_row" ,length = 50) // DB 컬럼: grid_row (또는 gridRow)
    private String gridRow;

    @Column(name = "grid_column", length = 50) // DB 컬럼: grid_column (또는 gridColumn)
    private String gridColumn;

    // branchId, gridRow, gridColumn 필드 제거됨
    // 기본 생성자, 다른 생성자 등 필요시 추가
    public Seat() {} // JPA는 기본 생성자가 필요할 수 있음
}