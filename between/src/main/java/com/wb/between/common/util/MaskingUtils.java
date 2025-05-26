package com.wb.between.common.util;

public class MaskingUtils {

    /**
     * 유틸리티 클래스이므로 인스턴스화 방지
     */
    private MaskingUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 이메일 주소를 마스킹 처리합니다.
     * 예: testuser@example.com -> te****er@example.com 또는 us***@example.com 등
     * @param email 마스킹할 이메일 주소
     * @return 마스킹된 이메일 주소, 또는 입력값이 유효하지 않으면 "정보없음" 또는 원본 반환
     */
    public static String maskEmail(String email) {
        if (email == null || email.length() < 5 || !email.contains("@")) {
            return "정보없음"; // 또는 예외 발생, 또는 원본 반환 등 정책에 따라 결정
        }

        int atIndex = email.indexOf('@');
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex); // @ 포함

        if (localPart.length() <= 1) {
            return "*" + domainPart;
        } else if (localPart.length() == 2) {
            // 예: ab@example.com -> a*@example.com
            return localPart.charAt(0) + "*" + domainPart;
        } else if (localPart.length() == 3) {
            // 예: abc@example.com -> a*c@example.com
            return localPart.charAt(0) + "*" + localPart.charAt(localPart.length() - 1) + domainPart;
        } else { // 4글자 이상
            // 예: testuser@example.com -> te****er@example.com
            // 또는 testuser@example.com -> tes***@example.com
            return localPart.substring(0, 2) + "****" + localPart.substring(localPart.length() - 2) + domainPart;
            // 좀 더 간단한 마스킹: return localPart.substring(0, 3) + "****" + domainPart;
        }
    }

    /**
     * 휴대폰 번호를 마스킹 처리합니다.
     * 예: 010-1234-5678 -> 010-****-5678
     * @param phone 마스킹할 휴대폰 번호 (하이픈 포함/미포함 가능)
     * @return 마스킹된 휴대폰 번호, 또는 입력값이 유효하지 않으면 "정보없음" 또는 원본 반환
     */
    public static String maskPhoneNumber(String phone) {

        if (phone == null || phone.isEmpty()) {
            return "정보없음";
        }

        String cleanPhone = phone.replaceAll("[^0-9]", ""); // 숫자만 추출

        if (cleanPhone.length() < 7) { // 예: 123456 -> 12**56
            if (cleanPhone.length() <= 4) return "****";
            return cleanPhone.substring(0, 2) + "*".repeat(cleanPhone.length() - 4) + cleanPhone.substring(cleanPhone.length() - 2);
        } else if (cleanPhone.length() <= 10) { // 예: 021234567 -> 02-***-4567
            return cleanPhone.substring(0, cleanPhone.length() - 7) + // 지역번호 등 (02)
                    "-" +
                    "*".repeat(3) + // 중간 3자리 고정 마스킹
                    "-" +
                    cleanPhone.substring(cleanPhone.length() - 4); // 마지막 4자리
        } else { // 11자리 휴대폰 번호 (010-1234-5678 -> 010-****-5678)
            return cleanPhone.substring(0, 3) +
                    "-" +
                    "*".repeat(Math.max(0,cleanPhone.length() - 7)) + // 국번 (최대 4자리, 최소 3자리) 마스킹
                    "-" +
                    cleanPhone.substring(cleanPhone.length() - 4);
        }
    }

}
