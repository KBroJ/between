package com.wb.between.admin.reservation.dto;

import lombok.Data;


// 관리자 > 예약 관리 페이지 : 예약 목록 조회 시 필터 조건을 담는 DTO
@Data
public class ReservationFilterParamsDto {

    private String startDate;   // 예약 시작일 (검색 조건, yyyy-MM-dd 형식)
    private String endDate;     // 예약 종료일 (검색 조건, yyyy-MM-dd 형식)
    private String searchType;  // 검색 타입 ("email" 또는 "name")
    private String searchText;  // 검색어
    private Long seatNo;        // 좌석 번호 (좌석 필터링용, null 가능 - null이면 '전체')

    // 기본 생성자 (필요시 기본값 설정 등에 사용)
    public ReservationFilterParamsDto() {
        this.searchType = "email"; // 기본 검색 타입을 이메일로 설정
        this.searchText = "";
        this.seatNo = null;        // 기본값으로 null 설정
    }

}
