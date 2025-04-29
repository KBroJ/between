package com.wb.between.admin.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 예약 목록 필터의 좌석 선택 드롭다운에 사용될 DTO
@Data                // Lombok: Getter, Setter 등 자동 생성
@NoArgsConstructor   // Lombok: 파라미터 없는 기본 생성자 생성
@AllArgsConstructor  // Lombok: 모든 필드를 받는 생성자 생성
public class SeatDto {

    private Long seatNo;     // 좌석 번호 (option의 value)
    private String seatNm;   // 좌석 이름 (option의 text)

}
