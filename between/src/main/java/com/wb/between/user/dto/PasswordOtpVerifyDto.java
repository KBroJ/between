package com.wb.between.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PasswordOtpVerifyDto {
    private String email;
    private String code;
}