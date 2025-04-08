package com.wb.between.mypage.dto;

import com.wb.between.menu.domain.Menu;
import com.wb.between.menu.dto.MenuListResponseDto;
import com.wb.between.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Builder
public class MypageResponseDto {

    private Long userNo;

    private String email;

    private String name;

    private String phoneNo;

    private LocalDateTime createDt;

    public static MypageResponseDto from(User user) {
        return MypageResponseDto.builder()
                .userNo(user.getUserNo())
                .email(user.getEmail())
                .name(user.getName())
                .phoneNo(user.getPhoneNo())
                .createDt(user.getCreateDt())
                .build();
    }

}
