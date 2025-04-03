package com.wb.between.user.dto;

public record VerificationResult(VerificationStatus status, String email) {

    // 인증성공 && 회원정보 있음
    public static VerificationResult success(String email) {
        return new VerificationResult(VerificationStatus.SUCCESS, email);
    }

    // 인증성공 && 회원정보 없음
    public static VerificationResult userNotFound() {
        return new VerificationResult(VerificationStatus.USER_NOT_FOUND, null);
    }

    // 인증실패
    public static VerificationResult otpInvalid() {
        return new VerificationResult(VerificationStatus.OTP_INVALID_OR_EXPIRED, null);
    }


}
