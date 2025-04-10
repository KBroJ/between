package com.wb.between.reservation.reserve.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
// import java.time.LocalDate; // 필요시 사용
// import java.time.LocalTime; // 필요시 사용

@Entity
@Getter
@Setter
@Table(name = "Reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long resNo; // 예약 번호 (PK)

    @Column(nullable = false)
    private Long userNo; // 예약한 사용자 번호

    @Column(nullable = false)
    private Long seatNo; // 예약된 좌석 번호 (Seat 테이블의 PK 타입과 일치해야 함)

    @Column(nullable = false, length = 10)
    private String totalPrice; // 최종 결제 금액 (!!! DB 스키마상 String, 숫자 타입 권장 !!!)

    @Column(nullable = false, length = 10)
    private String resPrice; // 쿠폰/할인 전 좌석 가격 (String)

    @Column(length = 10)
    private String dcPrice; // 할인 금액 (String)

    private Long userCpNo; // 사용된 사용자 쿠폰 번호/ID

    @CreationTimestamp // 레코드 생성 시 자동 입력
    @Column(nullable = false, updatable = false)
    private LocalDateTime resDt; // 예약 요청/생성 시각

    @UpdateTimestamp // 레코드 수정 시 자동 업데이트
    private LocalDateTime moDt; // 예약 변경/취소 시각

    @Column // nullable = true 기본값
    private Boolean resStatus; // 예약 상태 (null: 보류, 1: 완료 (true), 0 : 취소 (false))

    @Column(nullable = false)
    private LocalDateTime resStart; // 예약 시작 시각 (날짜 + 시간)

    @Column(nullable = false)
    private LocalDateTime resEnd; // 예약 종료 시각 (날짜 + 시간)

    // --- 추가될 수 있는 필드 ---
    // @Column(nullable = false)
    // private String planType; // HOURLY, DAILY, MONTHLY - DTO에는 있지만 DB 스키마에는 없음! 추가 필요?

    // @Column(length = 50)
    // private String orderId; // 토스페이먼츠용 주문 ID 저장 필드

    // @Column(length = 255)
    // private String paymentKey; // 토스페이먼츠 결제 키 저장 필드

    // @Column(length = 20)
    // private String paymentStatus = "PENDING"; // 결제 상태 (PENDING, PAID, FAILED, CANCELED) - String 또는 Enum 권장

    public Reservation() {} // 기본 생성자


    public Reservation(
            long l,
            LocalDateTime localDateTime, String s, LocalDateTime localDateTime1, LocalDateTime localDateTime2, String number, String 예약완료, boolean b
    ) {
    }
}


