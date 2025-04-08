package com.wb.between.reservation.reserve.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ReservationRequestDto {
    private Long itemId; // 좌석/룸 ID (seatNo)
    // private String itemType; // SEAT / ROOM - DB 저장 필요시 Reservation Entity에 추가
    private String planType; // HOURLY, DAILY, MONTHLY
    private String reservationDate; // 시작 날짜 "YYYY-MM-DD" 시작 날짜만 있는 이유는 종료 날짜와 시간은 백엔드(서버)에서 계산해서 처리하기 때문~
    private List<String> selectedTimes; // 시간제 선택 시간 목록 ["HH:mm", ...]
    private String couponId; // 사용한 쿠폰 ID (userCpNo 와 매핑될 수 있음)
    private Long userId; // !!! 중요: 예약자 식별자 (로그인 정보 등에서 가져와야 함) !!!
    // private int totalPrice; // 가격은 백엔드에서 재계산하므로 필수 아님
}