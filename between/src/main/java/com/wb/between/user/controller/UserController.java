package com.wb.between.user.controller;

import com.wb.between.user.domain.User;
import com.wb.between.user.dto.SignupRequest;
import com.wb.between.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
public class UserController {

    private UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 회원가입 페이지 호출
    @GetMapping("/signup")
    public String singupForm(Model model) {

        model.addAttribute("user", new User());

//        return "login/signup";
        return "login/testSignup";
    }

// 회원가입
    @PostMapping("/signup")
    public String registerUser(
            // @Valid : SignupRequest 객체에 대한 유효성 검사
            @Valid @ModelAttribute("user") SignupRequest signupRequest,
            BindingResult result, Model model
    ) {

        System.out.println("UserController|registerUser|signupRequest = " + signupRequest);

        // 유효성 검사 실패 시 로그 출력
        System.out.println("유효성 검사|result.hasErrors() = " + result.hasErrors());
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
                    System.out.println("유효성 검사 실패: " + error.getDefaultMessage());
                }
            });

            return "redirect:/";
        }

        try {

            System.out.println("UserController|registerUser|회원가입 진행 전");
            User user = userService.registerUser(signupRequest);
            System.out.println("UserController|registerUser|회원가입 진행 후 | user = " + user);

            return "redirect:/";

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "/signup";
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

    // 휴대폰번호 인증번호 전송
    @PostMapping("/send-verification")
    @ResponseBody
    public Map<String, String> sendVerificationCode(@RequestBody Map<String, String> request) {
        String phoneNumber = request.get("phoneNumber");
        String code = userService.generateAndSendVerificationCode(phoneNumber);

        Map<String, String> response = new HashMap<>();
        response.put("success", "true");
        // 실제 구현에서는 코드를 클라이언트에 반환하지 않음
        // 여기서는 테스트를 위해 반환
        response.put("code", code);
        return response;
    }

}
