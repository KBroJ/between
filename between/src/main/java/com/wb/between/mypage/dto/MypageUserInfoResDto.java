package com.wb.between.mypage.dto;

import com.wb.between.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MypageUserInfoResDto {

    private Long userNo;

    private String email;

    private String name;

    private String phoneNo;

    private LocalDateTime createDt;

    public static MypageUserInfoResDto from(User user) {
        return MypageUserInfoResDto.builder()
                .userNo(user.getUserNo())
                .email(user.getEmail())
                .name(user.getName())
                .phoneNo(user.getPhoneNo())
                .createDt(user.getCreateDt())
                .build();
    }

}
