package com.wb.between.admin.user.dto;

import lombok.Data;

@Data
public class UserUpdateReqDto {

    private String authCd;   // 변경할 회원 등급
    private String userStts;  // 변경할 회원 상태
    private String updateRs;  // 수정 사유

}
