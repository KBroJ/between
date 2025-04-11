package com.wb.between.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

// 마이페이지 > 예약 내역 조회 DTO
@Getter
@Setter
@NoArgsConstructor // 기본 생성자
@AllArgsConstructor // 모든 필드를 받는 생성자
public class MyReservationDto {

    private Long resNo;                 // 예약 번호
    private String totalPrice;          // 총 결제 금액
    private String resPrice;            // 좌석 예약 비용
    private String dcPrice;             // 할인 비용

    private String userCpNo;            // 쿠폰 식별 번호

    private LocalDateTime resDt;        // 예약일자
    private String resStatus;            // 예약상태 (예: "예약완료", "이용완료", "취소됨")
    private LocalDateTime resStart;     // 예약 시작일자
    private LocalDateTime resEnd;       // 예약 종료일자

    private String seat;                // 좌석 번호
    private String seatNm;              // Seat 엔티티와 조인해서 가져와야 함 (실제 구현 시)


    public MyReservationDto(
            Long resNo,
            LocalDateTime resDt,
            String seatNm,
            LocalDateTime resStart,
            LocalDateTime resEnd,
            String totalPrice,
            String resPrice,
            String dcPrice,
            String resStatus
        )
    {
        this.resNo = resNo;
        this.resDt = resDt;
        this.seatNm = seatNm;
        this.resStart = resStart;
        this.resEnd = resEnd;
        this.totalPrice = totalPrice;
        this.resPrice = resPrice;
        this.dcPrice = dcPrice;
        this.resStatus = resStatus;
    }

}
