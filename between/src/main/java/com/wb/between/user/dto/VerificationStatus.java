package com.wb.between.user.dto;

// 휴대폰 번호 인증 결과의 상태를 나타내기 위한 enum
public enum VerificationStatus {

    SUCCESS,                 // 인증 성공 및 사용자 찾음
    OTP_INVALID_OR_EXPIRED,  // OTP가 유효하지 않거나 만료됨
    USER_NOT_FOUND           // OTP는 유효했으나 해당 전화번호 사용자를 찾지 못함

}
