package com.wb.between.admin.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/*
    DTO : 화면에 필요한 데이터만 전달하고 데이터를 가공하기 위한 객체
*/
@Data
@Builder
public class UserDetailDto {

    private Long userNo;
    private String email;
    private String name;
    private String phoneNo;
    private String authCd;
    private String userStts;                            // ("정상", "휴면", "탈퇴")
    private LocalDateTime createDt;
    private List<UserDetailReservationListDto> recentReservations;    // 최근 예약 목록

}
