package com.wb.between.admin.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/*
    관리자 > 회원 관리 페이지 접근 시 회원 목록을 조회하기 위해
    테이블에 필요한 데이터만 담는 DTO
*/
@Data
@Builder // @Builder 어노테이션을 사용하여 Builder 패턴을 적용 :
public class UserListDto {

    private Long userNo;
    private String email;
    private String name;
    private String phoneNo; // 마스킹된 번호
    private LocalDateTime createDt;
    private String userStts; // DB 값 그대로 ("일반", "휴면", "탈퇴") 또는 매핑된 값 ("정상", "휴면", "탈퇴")
    private String authCd; // DB 값 그대로 ("일반", "임직원", "관리자")

}
