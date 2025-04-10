package com.wb.between.pay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor // 기본 생성자
@AllArgsConstructor // 모든 필드 생성자
@Builder // 빌더 패턴 사용 (객체 생성 시 편리)
@Table(name = "Payment") // 실제 테이블명 확인
public class Payment {

    @Id // 기본 키
    @Column(length = 255)
    private String paymentKey; // 카카오페이: aid 또는 tid (aid가 승인 후 고유값)

    @Column(nullable = false)
    private Long resNo; // 연결된 예약 번호 (Reservation 테이블 FK)

    @Column(length = 255)
    private String payPrice; // 최종 결제 금액 (VARCHAR)

    @Column(length = 255)
    private String payStatus; // 결제 상태 ("DONE", "CANCELED", "FAILED" 등)

    @Column(length = 255)
    private String payApproveDt; // 결제 승인 시각 (카카오 응답 그대로 저장)

    @Column(length = 255)
    private String payCanclDt; // 결제 취소 시각 (String)

    @Column(nullable = false, length = 255)
    private String errCode = ""; // 오류 코드 (성공 시 빈 문자열)

    @Column(nullable = false, length = 255)
    private String errMsg = ""; // 오류 메시지 (성공 시 빈 문자열)

    @Column(length = 255)
    private String registDt; // 결제 준비 요청 시각 (카카오 응답 created_at)

    @Column(length = 255)
    private String method; // 결제 수단 ("CARD", "MONEY" 등)

    @Column(length = 255)
    private String payProvider = "KAKAO"; // 결제 제공자

    // @UpdateTimestamp // 수정 시 자동 업데이트 (필요시 활성화)
    // private LocalDateTime updateDt; // LocalDateTime 타입 권장
    @Column(length = 255)
    private String updateDt; // 스키마에 맞춰 String 유지

}