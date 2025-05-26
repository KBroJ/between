package com.wb.between.user.controller;

import com.wb.between.common.util.MaskingUtils;
import com.wb.between.common.util.OAuth.CustomOAuth2UserService;
import com.wb.between.common.util.OAuth.OAuthAttributes;
import com.wb.between.user.domain.User;
import com.wb.between.user.repository.UserRepository;
import com.wb.between.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
// OAuth2AuthenticationToken 등을 사용하여 수동 로그인 시 필요
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/social/link-account")
@RequiredArgsConstructor
public class AccountLinkController {

    private final HttpSession httpSession;
    private final UserService userService; // OTP 발송/검증, 사용자 정보 업데이트 등
    private final UserRepository userRepository; // 사용자 조회/저장

    // OTP 발송/검증 시 사용할 세션 키 접두사 (기존 회원가입과 구분)
    private static final String OTP_ACCOUNT_LINKING_PREFIX = "OTP_ACC_LINK_";

    /**
     * 계정 연결 안내 및 휴대폰 인증 시작 페이지 표시
     */
    @GetMapping("/start")
    public String startAccountLinkingPage(Model model, RedirectAttributes redirectAttributes) {

        String userMessage = (String) httpSession.getAttribute("socialLinkUserMessage"); // 소셜로그인 인증 사용자 안내 메시지
        String errorCode = (String) httpSession.getAttribute("socialLinkErrorCode");    // 소셜로그인 인증 에러 코드
        // 소셜 로그인 정보
        OAuthAttributes pendingSocialAttributes = (OAuthAttributes) httpSession.getAttribute(CustomOAuth2UserService.PENDING_SOCIAL_ATTRIBUTES_SESSION_KEY);

        if (pendingSocialAttributes == null || errorCode == null) {
            log.warn("AccountLinkController:/start - 계정 연결에 필요한 세션 정보가 없습니다. 로그인 페이지로 리디렉션합니다.");
            redirectAttributes.addFlashAttribute("globalError", "계정 연결을 진행할 수 없습니다. 다시 시도해주세요.");
            return "redirect:/login";
        }

        model.addAttribute("pageTitle", "소셜 계정 연결");
        model.addAttribute("userMessage", userMessage); // 사용자 안내 메시지
        model.addAttribute("errorCode", errorCode);     // 어떤 시나리오인지 구분하기 위함

        String phoneToVerify = null;
        String displayContextMessage = "";

        if (CustomOAuth2UserService.ERROR_CODE_EMAIL_EXISTS_PHONE_MISMATCH_LINK.equals(errorCode)) {

            phoneToVerify = (String) httpSession.getAttribute(CustomOAuth2UserService.LINK_VERIFICATION_PHONE_SESSION_KEY);
            String targetEmail = (String) httpSession.getAttribute(CustomOAuth2UserService.LINK_TARGET_EMAIL_SESSION_KEY);

            displayContextMessage = "이메일(" + targetEmail + ") 계정에 " + pendingSocialAttributes.getRegistrationId().toUpperCase() + " 계정을 연결합니다.";

            model.addAttribute("subTitle", "기존 계정 휴대폰 인증");

        } else if (CustomOAuth2UserService.ERROR_CODE_NEW_EMAIL_PHONE_CONFLICT_LINK.equals(errorCode)) {
            phoneToVerify = (String) httpSession.getAttribute(CustomOAuth2UserService.CONFLICTING_ACCOUNT_PHONE_SESSION_KEY);
            String conflictingEmail = (String) httpSession.getAttribute(CustomOAuth2UserService.CONFLICTING_ACCOUNT_EMAIL_SESSION_KEY);
            displayContextMessage = "휴대폰 번호가 등록된 기존 계정(" + MaskingUtils.maskEmail(conflictingEmail) + ")에 " +
                    pendingSocialAttributes.getRegistrationId().toUpperCase() + " 계정을 연결합니다.";
            model.addAttribute("subTitle", "기존 계정 소유 확인 및 소셜 계정 연결");
        }

        if (phoneToVerify == null || phoneToVerify.isEmpty()) {
            log.error("AccountLinkController:/start - 인증할 휴대폰 번호를 세션에서 찾을 수 없습니다. ErrorCode: {}", errorCode);
            redirectAttributes.addFlashAttribute("globalError", "계정 연결 과정에 오류가 발생했습니다. (인증 대상 번호 없음)");
            return "redirect:/login";
        }

        model.addAttribute("displayContextMessage", displayContextMessage);
        model.addAttribute("phoneToVerifyDisplay", MaskingUtils.maskPhoneNumber(phoneToVerify)); // 마스킹된 번호 표시
        // 실제 인증에 사용할 번호는 세션에 이미 있음 (LINK_VERIFICATION_PHONE_SESSION_KEY 또는 CONFLICTING_ACCOUNT_PHONE_SESSION_KEY)

        return "login/account-linking";
    }

    /**
     * 계정 연결을 위한 휴대폰 인증번호 발송
     */
    @PostMapping("/send-otp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendOtpForLinking() {
        // 요청 본문은 현재 필요 없음, 세션에서 정보 사용
        log.info("AccountLinkController|sendOtpForLinking|계정 연결용 OTP 발송 요청");

        Map<String, Object> response = new HashMap<>();
        String phoneToVerify = null;
        String errorCode = (String) httpSession.getAttribute("socialLinkErrorCode");

        if (CustomOAuth2UserService.ERROR_CODE_EMAIL_EXISTS_PHONE_MISMATCH_LINK.equals(errorCode)) {
            phoneToVerify = (String) httpSession.getAttribute(CustomOAuth2UserService.LINK_VERIFICATION_PHONE_SESSION_KEY);
        } else if (CustomOAuth2UserService.ERROR_CODE_NEW_EMAIL_PHONE_CONFLICT_LINK.equals(errorCode)) {
            phoneToVerify = (String) httpSession.getAttribute(CustomOAuth2UserService.CONFLICTING_ACCOUNT_PHONE_SESSION_KEY);
        }

        if (phoneToVerify == null || phoneToVerify.isEmpty()) {
            response.put("success", false);
            response.put("message", "인증할 휴대폰 번호 정보를 찾을 수 없습니다.");
            return ResponseEntity.badRequest().body(response);
        }

        try {

            userService.generateAndSendOtp(httpSession, phoneToVerify, UserService.OTP_ACCOUNT_LINKING_PREFIX);

            log.info("계정 연결용 OTP 발송 요청: 번호 [{}] (접두사: {})", phoneToVerify, UserService.OTP_ACCOUNT_LINKING_PREFIX);

            response.put("success", true);
            response.put("message", "인증번호가 [" + MaskingUtils.maskPhoneNumber(phoneToVerify) + "]로 발송되었습니다.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("계정 연결용 OTP 발송 오류 (번호: {}): {}", phoneToVerify, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "인증번호 발송 중 오류가 발생했습니다.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 입력된 인증번호를 검증하고, 성공 시 계정 연결 처리
     */
    @PostMapping("/verify-and-link")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyOtpAndLinkAccount(@RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();

        String otpCode = payload.get("otp");
        String errorCode = (String) httpSession.getAttribute("socialLinkErrorCode");
        OAuthAttributes pendingSocialAttributes = (OAuthAttributes) httpSession.getAttribute(CustomOAuth2UserService.PENDING_SOCIAL_ATTRIBUTES_SESSION_KEY);
        String phoneToVerify = null;

        if (CustomOAuth2UserService.ERROR_CODE_EMAIL_EXISTS_PHONE_MISMATCH_LINK.equals(errorCode)) {
            phoneToVerify = (String) httpSession.getAttribute(CustomOAuth2UserService.LINK_VERIFICATION_PHONE_SESSION_KEY);
        } else if (CustomOAuth2UserService.ERROR_CODE_NEW_EMAIL_PHONE_CONFLICT_LINK.equals(errorCode)) {
            phoneToVerify = (String) httpSession.getAttribute(CustomOAuth2UserService.CONFLICTING_ACCOUNT_PHONE_SESSION_KEY);
        }

        if (otpCode == null || phoneToVerify == null || pendingSocialAttributes == null) {
            response.put("success", false);
            response.put("message", "계정 연결 처리 중 오류가 발생했습니다. (필수 정보 누락)");
            cleanupLinkSessionAttributes(phoneToVerify); // 불완전하더라도 세션 정리 시도
            return ResponseEntity.badRequest().body(response);
        }

        String cleanPhoneToVerify = phoneToVerify.replaceAll("-", "");
        String otpSessionKey = OTP_ACCOUNT_LINKING_PREFIX + cleanPhoneToVerify;

        // UserService에 verifyCode가 세션 키를 직접 받거나, 접두사를 받아 처리하도록 수정 필요
        boolean otpValid = userService.verifyOtp(httpSession, phoneToVerify, otpCode, UserService.OTP_ACCOUNT_LINKING_PREFIX);

        if (otpValid) {
            try {

                User linkedUser = performAccountLink(errorCode, pendingSocialAttributes, phoneToVerify);

                // User 객체가 OAuth2User를 구현하고, getAuthorities() 등을 올바르게 반환해야 함.
                Authentication authentication = new OAuth2AuthenticationToken(
                        linkedUser, // 연결/업데이트된 User 객체 (OAuth2User 타입이어야 함)
                        linkedUser.getAuthorities(), // User 객체에서 권한 정보 가져오기
                        pendingSocialAttributes.getRegistrationId() // 소셜 프로바이더 ID
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("계정 연결 성공 및 사용자 [{}] 로그인 처리 완료.", linkedUser.getEmail());

                cleanupLinkSessionAttributes(phoneToVerify); // 모든 관련 세션 정보 제거

                response.put("success", true);
                response.put("message", "계정이 성공적으로 연결되었습니다. 잠시 후 메인 페이지로 이동합니다.");
                response.put("redirectUrl", "/"); // 성공 시 리디렉션할 URL
                return ResponseEntity.ok(response);

            } catch (Exception e) {
                log.error("계정 연결 처리 중 심각한 오류 발생: {}", e.getMessage(), e);
                response.put("success", false);
                response.put("message", "계정 연결 중 오류가 발생했습니다.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } else {
            response.put("success", false);
            response.put("message", "인증번호가 올바르지 않거나 만료되었습니다.");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 실제 계정 연결 로직 수행 (User 엔티티 업데이트 및 저장)
     */
    @Transactional
    protected User performAccountLink(String errorCode, OAuthAttributes socialAttrs, String verifiedPhone) {

        log.info("AccountLinkController|performAccountLink|계정 연결 처리 시작. ErrorCode: {}, verifiedPhone: {}", errorCode, verifiedPhone);

        User userToLink = null;
        String socialRegistrationId = socialAttrs.getRegistrationId().toUpperCase();

        if (CustomOAuth2UserService.ERROR_CODE_EMAIL_EXISTS_PHONE_MISMATCH_LINK.equals(errorCode)) {

            // 이메일은 같았고, 기존 계정의 폰(verifiedPhone)을 인증했음.
            String targetEmail = (String) httpSession.getAttribute(CustomOAuth2UserService.LINK_TARGET_EMAIL_SESSION_KEY);
            userToLink = userRepository.findByEmail(targetEmail)
                    .orElseThrow(() -> new IllegalStateException("계정 연결 대상 사용자를 찾을 수 없습니다: " + targetEmail));

            // 소셜 로그인 정보 연결 (예: User 엔티티에 naverId, kakaoId 등 필드 추가 또는 별도 테이블 관리)
            userToLink.setLoginM(socialRegistrationId); // 마지막 로그인 수단 업데이트

            // 이름 등 소셜 정보로 업데이트 여부 결정
            if (userToLink.getName() == null || userToLink.getName().isEmpty()) {
                userToLink.setName(socialAttrs.getName());
            }

        } else if (CustomOAuth2UserService.ERROR_CODE_NEW_EMAIL_PHONE_CONFLICT_LINK.equals(errorCode)) {

            userToLink = userRepository.findByPhoneNo(verifiedPhone)
                    .orElseThrow(() -> new IllegalStateException("계정 연결 대상 사용자를 찾을 수 없습니다 (폰번호: " + verifiedPhone + ")"));

            userToLink.setLoginM(socialRegistrationId);

        } else {
            throw new IllegalStateException("알 수 없는 계정 연결 컨텍스트입니다: " + errorCode);
        }

        userToLink.setPhoneNo(verifiedPhone); // 소셜로그인 정보 상 휴대번호로 업데이트

        return userRepository.save(userToLink);
    }


    private void cleanupLinkSessionAttributes(String verifiedPhone) {

        httpSession.removeAttribute(CustomOAuth2UserService.PENDING_SOCIAL_ATTRIBUTES_SESSION_KEY);
        httpSession.removeAttribute(CustomOAuth2UserService.LINK_TARGET_EMAIL_SESSION_KEY);
        httpSession.removeAttribute(CustomOAuth2UserService.LINK_VERIFICATION_PHONE_SESSION_KEY);
        httpSession.removeAttribute(CustomOAuth2UserService.CONFLICTING_ACCOUNT_EMAIL_SESSION_KEY);
        httpSession.removeAttribute(CustomOAuth2UserService.CONFLICTING_ACCOUNT_PHONE_SESSION_KEY);
        httpSession.removeAttribute(CustomOAuth2UserService.LINK_CONTEXT_SESSION_KEY);
        httpSession.removeAttribute("socialLinkUserMessage");
        httpSession.removeAttribute("socialLinkErrorCode");

        if (verifiedPhone != null && !verifiedPhone.isEmpty()) {
            String cleanPhone = verifiedPhone.replaceAll("-", "");
            String otpSessionKey = OTP_ACCOUNT_LINKING_PREFIX + cleanPhone;
            httpSession.removeAttribute(otpSessionKey);
            httpSession.removeAttribute(otpSessionKey + "_expiry");
        }
        log.info("계정 연결 관련 세션 정보 정리 완료.");
    }

}
