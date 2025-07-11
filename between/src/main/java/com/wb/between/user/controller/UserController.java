package com.wb.between.user.controller;

import com.wb.between.coupon.service.CouponService;
import com.wb.between.user.domain.User;
import com.wb.between.user.dto.SignupReqDto;
import com.wb.between.user.dto.VerificationResult;
import com.wb.between.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
public class UserController {

    private UserService userService;
    private static final String OTP_PREFIX = "OTP_";

    private CouponService couponService;

    @Autowired
    public UserController(UserService userService, CouponService couponService) {
        this.userService = userService;
        this.couponService = couponService;
    }

    // 회원가입 페이지 호출
    @GetMapping("/signup")
    public String singupForm(Model model) {

        model.addAttribute("user", new User());

        return "login/signup";
    }

    // 회원정보(email/pwd) 확인 페이지 호출
    @GetMapping("/findUserInfo")
    public String findUserInfoForm(Model model) {

        model.addAttribute("user", new User());

        return "login/findUserInfo";
    }

    // 회원가입
    @PostMapping("/signup")
    public String registerUser(
            // @Valid : SignupRequest 객체에서 설정한 유효성 검사 실행
            // signupRequest : 회원가입 정보를 담는 객체
            // BindingResult : 유효성 검사 결과를 담는 객체
            @Valid @ModelAttribute("user") SignupReqDto signupReqDto,
            BindingResult result, Model model,
            HttpSession session, // HttpSession 파라미터 추가
            RedirectAttributes redirectAttributes
    ) {

        log.info("UserController|registerUser|signupRequest = {}", signupReqDto);

        // SignupRequest 유효성 검사 실패 시 로그 출력
        log.info("유효성 검사|result.getAllErrors() = {}", result.getAllErrors());
        log.info("유효성 검사|result.hasErrors() = {}", result.hasErrors());
        if (result.hasErrors()) {

            result.getAllErrors().forEach(error -> {

                if (error instanceof FieldError) {

                    FieldError fieldError = (FieldError) error;
                    String fieldName = fieldError.getField();
                    String errorMessage = fieldError.getDefaultMessage();
                    String rejectedValue = String.valueOf(fieldError.getRejectedValue());

                    System.out.println("유효성 검사 실패: 필드=" + fieldName +
                            ", 값=" + rejectedValue +
                            ", 메시지=" + errorMessage);
                } else {
                    log.info("유효성 검사 실패: " + error.getDefaultMessage());
                }
            });

            return "redirect:/";
        }

        try {

            // 회원가입 진행
            User user = userService.registerUser(signupReqDto);
            log.info("UserController|registerUser|회원가입 진행 후 | user = " + user);

            //쿠폰발급 진행, 쿠폰발급은 회원가입 흐름에 영향을 주지 않아야함
            if(user != null) {
                try {
                    couponService.signUpCoupon(user);
                } catch (Exception e) {
                    log.error("회원가입 성공 후 쿠폰 발급 실패. 사용자: {}, 에러: {}", user.getUserNo(), e.getMessage(), e);
                }

                // 회원가입 성공 시 세션에 사용자 정보 저장(비정상적 접근 제한용)
                session.setAttribute("signupFlowCompleted", true);
                session.setAttribute("signupSuccessUserName", user.getName());
            }

//            return "redirect:/";
            return "redirect:/signup-success";

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "/signup";
        }
    }

    /**
     * 회원가입 완료 페이지
     */
    @GetMapping("/signup-success")
    public String signupSuccessPage(Model model, HttpSession session, RedirectAttributes redirectAttributes) {

        // 세션에서 "signupFlowCompleted" 플래그 확인
        Boolean signupFlowCompleted = (Boolean) session.getAttribute("signupFlowCompleted");

        if (Boolean.TRUE.equals(signupFlowCompleted)) {
            // 정상적인 접근: 플래그 사용 후 세션에서 제거
            log.info("회원가입 완료 페이지 정상 접근");
            String userName = (String) session.getAttribute("signupSuccessUserName");
            if (userName != null) {
                model.addAttribute("userName", userName);
                session.removeAttribute("signupSuccessUserName"); // 사용자 이름 정보도 제거
            }
            session.removeAttribute("signupFlowCompleted"); // 플래그 제거
            return "login/signup-success";
        } else {
            // 비정상적인 접근: 메인 페이지로 리디렉션
            log.warn("회원가입 완료 페이지 비정상 접근 시도. 메인 페이지로 리디렉션합니다.");
//            redirectAttributes.addFlashAttribute("alertMessage", "잘못된 접근입니다."); // 메인 페이지에서 보여줄 메시지

            return "redirect:/";
        }

    }

/*
    getMapping으로 이메일 중복 체크

    @GetMapping("/checkEmail")
    @ResponseBody
    public Map<String, Boolean> checkEmail(@RequestParam("email") String email) {

        System.out.println("UserController|checkEmail|inputValue|email = " + email);
        boolean isAvailable = userService.checkEmail(email);
        System.out.println("UserController|checkEmail|isAvailable = " + isAvailable);

        Map<String, Boolean> response = new HashMap<>();
        response.put("available", isAvailable);

        return response;

    }
*/
/*
    PostMapping으로 이메일 중복 체크
 */
    @PostMapping("/checkEmail")
    @ResponseBody
    public Map<String, Boolean> checkEmail(@RequestBody User user) {

        System.out.println("UserController|checkEmail|inputValue|user = " + user);
        System.out.println("UserController|checkEmail|inputValue|user.getEmail() = " + user.getEmail());
        boolean isAvailable = userService.checkEmail(user.getEmail());
        System.out.println("UserController|checkEmail|isAvailable = " + isAvailable);

        Map<String, Boolean> response = new HashMap<>();
        response.put("available", isAvailable);

        return response;

    }

    // 휴대폰번호 인증번호 전송(회원가입, 회원정보 이메일 찾기 시에 사용)
    @PostMapping("/send-verification")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendVerificationCode(
            @RequestBody Map<String, String> request,
            HttpSession session
    ) {
        log.info("UserController|sendVerificationCode|시작 ==========> request : " + request);

        String phoneNo = request.get("phoneNo");
        String context = request.get("context"); // "signup" 또는 "findEmail"
        Map<String, Object> response = new HashMap<>();

        if (phoneNo == null || context == null) {
            response.put("success", false);
            response.put("message", "잘못된 요청입니다. (필수 정보 누락)");
            return ResponseEntity.badRequest().body(response); // 400 Bad Request
        }

        try {
            // 1. 휴대폰 번호 유효성 검사
            boolean phoneExists = userService.isPhoneNumberDuplicated(phoneNo);

            // 회원가입 - 인증번호 발송
            if("signup".equalsIgnoreCase(context)) {
                if (phoneExists) {
                    // 1. 휴대폰 번호 중복인 경우
                    log.warn("UserController|sendVerificationCode|중복된 휴대폰 번호: {}", phoneNo);

                    response.put("success", false);
                    response.put("message", "이미 사용 중인 휴대폰 번호입니다.");

                    return ResponseEntity.ok(response); // 클라이언트가 처리하기 쉽도록 200 OK와 함께 응답
                } else {

                    // 2. 중복되지 않은 경우, 인증번호 생성 및 발송
                    userService.generateAndSendVerificationCode(session, phoneNo);

                    response.put("success", true);
                    response.put("message", "인증번호가 발송되었습니다.");

                    return ResponseEntity.ok(response);
                }
            }
            // 회원 정보 찾기(이메일) - 인증번호 발송
            else if ("findEmail".equalsIgnoreCase(context)) {
                if (phoneExists) {
                    // 휴대폰 번호가 DB에 존재해야 함
                    userService.generateAndSendVerificationCode(session, phoneNo);
                    response.put("success", true);
                    response.put("message", "인증번호가 발송되었습니다. 휴대폰을 확인해주세요.");
                    return ResponseEntity.ok(response);
                } else {
                    // 휴대폰 번호가 DB에 존재하지 않음
                    log.warn("UserController|sendVerificationCode [findEmail]|등록되지 않은 휴대폰 번호: {}", phoneNo);
                    response.put("success", false);
                    response.put("message", "가입되지 않은 휴대폰 번호입니다.");
                    return ResponseEntity.ok(response);
                }
            } else {
                log.warn("UserController|sendVerificationCode|알 수 없는 context: {}", context);
                response.put("success", false);
                response.put("message", "잘못된 요청 유형입니다.");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) { // SMS 발송 실패 또는 기타 예외 처리
            log.error("UserController|sendVerificationCode|인증번호 처리 중 알 수 없는 오류 발생: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("message", "인증번호 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");

            return  ResponseEntity.ok(response);
        }
    }

    // 회원가입 > 휴대폰 인증번호 확인
    @PostMapping("/signup/verify-code")
    @ResponseBody
    public Map<String, Boolean> verifyCode(@RequestBody Map<String, String> request, HttpSession session) {
        System.out.println("UserController|verifyCode|시작 ==========> request : " + request);

        String phoneNo = request.get("phoneNo");
        String code = request.get("code");

        boolean isValid = userService.verifyCode(session, phoneNo, code);
        System.out.println("UserController|verifyCode|isValid : " + isValid);

        Map<String, Boolean> response = new HashMap<>();
        response.put("valid", isValid);

        // 검증 성공 시 컨트롤러에서 세션 OTP 제거
        if (isValid) {
            session.removeAttribute(OTP_PREFIX + phoneNo);              // 인증번호 세션 제거
            session.removeAttribute(OTP_PREFIX + phoneNo + "_expiry");  // 인증번호 만료 시간 세션 제거

                System.out.println("========== 세션에 저장된 인증번호 삭제 확인 ==========");
                Enumeration<String> attributeNames = session.getAttributeNames();
                while (attributeNames.hasMoreElements()) {
                    String attributeName = attributeNames.nextElement();
                    Object attributeValue = session.getAttribute(attributeName);
                    System.out.println("키: " + attributeName + ", 값: " + attributeValue);
                }
                System.out.println("=================================================");
        }

        return response;
    }

// 회원정보찾기 > 이메일 찾기 : 휴대번호로 인증 후 이메일 조회
    @PostMapping("/findUserInfo/verify-code")
    @ResponseBody
    public Map<String, Object> findEmailByVerifiedPhone(@RequestBody Map<String, String> request, HttpSession session) {
        System.out.println("UserController|findEmailByVerifiedPhone|시작 ==========> request : " + request);

        String phoneNo = request.get("phoneNo");
        String code = request.get("code");

        // 인증번호 검증 및 이메일 정보 조회
        VerificationResult result = userService.verifyAndGetUserByPhone(session, phoneNo, code);

        Map<String, Object> response = new HashMap<>();

        // 결과 상태(VerificationStatus)에 따라 분기
        switch (result.status()) {
            case SUCCESS:
                String email = result.email();
                System.out.println("UserController|findEmailByVerifiedPhone| 최종 성공: " + email);
                response.put("success", true);
                response.put("email", email);
                break;

            case USER_NOT_FOUND:
                System.out.println("UserController|findEmailByVerifiedPhone| 실패: 사용자 없음");
                response.put("success", false);
                response.put("message", "인증번호는 확인되었으나, 해당 번호로 가입된 사용자를 찾을 수 없습니다.");
                break;

            case OTP_INVALID_OR_EXPIRED:
                System.out.println("UserController|findEmailByVerifiedPhone| 실패: OTP 오류");
                response.put("success", false);
                response.put("message", "인증번호가 올바르지 않거나 만료되었습니다.");
                break;

            default:
                System.out.println("UserController|findEmailByVerifiedPhone| 실패: 알 수 없는 오류");
                response.put("success", false);
                response.put("message", "알 수 없는 오류가 발생했습니다.");
                break;
        }

        return response;
    }




    /**
     * [API] 비밀번호 찾기 - 1단계: 이메일 확인 및 OTP 발송 요청
     */
    @PostMapping("/findUserInfo/reqSendEmail")
    @ResponseBody
    public Map<String, Object> reqSendEmail(@RequestBody Map<String, String> request, HttpSession session) {
        System.out.println("UserController|requestPasswordOtp| 시작 ==========> email: " + request.get("email"));

        String email = request.get("email");

        Map<String, Object> response = new HashMap<>();

        try {
            // 회원여부 확인 > 인증번호 생성 > 메일 발송(세션저장)
            boolean requested = userService.requestPasswordOtp(email, session);
            response.put("success", requested);

            if (!requested) {
                response.put("message", "가입되지 않은 이메일이 입니다.");
            }
        } catch (Exception e) {
            System.err.println("UserController|requestPasswordOtp| 오류 발생: " + e.getMessage());
            e.printStackTrace(); // 로그 추가
            response.put("success", false);
            response.put("message", "처리 중 오류가 발생했습니다.");
        }
        return response;
    }

    /**
     * [API] 비밀번호 찾기 - 2단계: 이메일 인증번호 검증
     */
    @PostMapping("/findUserInfo/verifyPwdCode")
    @ResponseBody
    public Map<String, Object> verifyPwdCode(@RequestBody Map<String, String> request, HttpSession session) {
        System.out.println("UserController|verifyPasswordOtp| 시작 ==========> email: " + request.get("email") + ", code: " + request.get("code"));

        String email = request.get("email");
        String code = request.get("code");

        Map<String, Object> response = new HashMap<>();

        try {

            // 인증번호 및 유효시간 검증 > 유효 시 세션 제거
            boolean isValid = userService.verifyPasswordOtp(email, code, session);
            response.put("success", isValid);

            if (!isValid) {
                response.put("message", "인증번호가 올바르지 않거나 만료되었습니다.");
            }

        } catch (Exception e) {
            System.err.println("UserController|verifyPasswordOtp| 오류 발생: " + e.getMessage());
            e.printStackTrace(); // 로그 추가
            response.put("success", false);
            response.put("message", "처리 중 오류가 발생했습니다.");
        }
        return response;
    }

    /**
     * [API] 비밀번호 찾기 - 3단계: 새 비밀번호 설정
     * @Valid 추가하여 DTO 유효성 검사 가능
     */
    @PostMapping("/api/resetPwd")
    @ResponseBody
    public Map<String, Object> resetPwd(@Valid @RequestBody User user, BindingResult bindingResult) {
        System.out.println("UserController|resetPassword| 시작 ==========> email: " + user.getEmail());
        System.out.println("UserController|resetPassword| 시작 ==========> password: " + user.getPassword());
        Map<String, Object> response = new HashMap<>();

        // User Param(email) 유효성 검사 결과 확인
        if (bindingResult.hasErrors()) {
            System.out.println("UserController|resetPassword| 유효성 검사 실패");
            // 첫번째 에러 메시지만 전달하거나, 모든 에러를 조합할 수 있음
            response.put("success", false);
            response.put("message", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return response;
        }

        try {
            boolean resetSuccess = userService.resetPassword(user.getEmail(), user.getPassword());
            response.put("success", resetSuccess);

            if (!resetSuccess) {
                response.put("message", "비밀번호 변경 중 오류가 발생했습니다.");
            }

        } catch (Exception e) {
            System.err.println("UserController|resetPassword| 오류 발생: " + e.getMessage());
            e.printStackTrace(); // 로그 추가
            response.put("success", false);
            response.put("message", "처리 중 오류가 발생했습니다.");
        }
        return response;
    }


}
