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
                .phoneNo(phoneNumber(user.getPhoneNo()))
                .createDt(user.getCreateDt())
                .build();
    }

    // 휴대폰 번호 커스텀
    private static String phoneNumber(String phoneNo) {
        if (phoneNo != null && phoneNo.length() == 11) {
            return phoneNo.substring(0, 3) + "-" +  phoneNo.substring(3, 7)+ "-" + phoneNo.substring(7, 11);
        } else if (phoneNo != null && phoneNo.length() == 10) {
            return phoneNo.substring(0, 3) + "-" + phoneNo.substring(3, 6) + "-" + phoneNo.substring(6, 10);
        }
        return phoneNo;
    }

}
