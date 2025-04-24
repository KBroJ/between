package com.wb.between.admin.user.dto;

import lombok.Data;

@Data
public class UserFilterParamsDto {

    private String startDate;   // yyyy-MM-dd 형식의 문자열
    private String endDate;     // yyyy-MM-dd 형식의 문자열
    private String searchType;  // "email", "name", "phone"
    private String searchText;  // 검색어
    private String status;      // "", "정상", "휴면" (DB쿼리 시 "정상" -> "일반" 변환 필요)
    private String grade;       // "", "일반", "임직원", "관리자"
    private Integer page;       // 현재 페이지 번호 (페이징 링크 생성 시 필요) - 컨트롤러에서 자동 매핑됨

    // Thymeleaf에서 쿼리 스트링 생성 시 null 방지를 위해 기본값 설정 가능
    public UserFilterParamsDto() {
        this.searchType = "email"; // 기본 검색 타입
        this.status = "";
        this.grade = "";
        this.searchText = "";
    }

}
