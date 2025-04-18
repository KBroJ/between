package com.wb.between.mypage.dto;

import com.wb.between.user.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserInfoEditReqDto {

    private Long userNo;

    private String email;

    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = "^(010|011)[0-9]{7,8}$", message = "전화번호 형식이 올바르지 않습니다.")
    private String phoneNo;

    private LocalDateTime createDt;

    public static UserInfoEditReqDto from(User user) {
        return UserInfoEditReqDto.builder()
                .userNo(user.getUserNo())
                .email(user.getEmail())
                .name(user.getName())
                .phoneNo(user.getPhoneNo())
                .createDt(user.getCreateDt())
                .build();
    }

}
