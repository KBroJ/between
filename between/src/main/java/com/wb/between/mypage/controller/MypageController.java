package com.wb.between.mypage.controller;

import com.wb.between.mypage.dto.MypageResponseDto;
import com.wb.between.mypage.service.MypageService;
import com.wb.between.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final MypageService mypageService;

    /**
     * 마이페이지 조회
     */
    @GetMapping
    public String mypage(@AuthenticationPrincipal User user, Model model) {

        log.debug("user = {}", user);
        MypageResponseDto mypageResponseDto = mypageService.findUserbyId(user.getUserNo());

        model.addAttribute("userInfo", mypageResponseDto);

        return "mypage/dashboard";
    }

    /**
     * 마이페이지 > 정보 수정 조회
     */
    @GetMapping("/edit")
    public String editProfile(@AuthenticationPrincipal User user, Model model) {

        log.debug("user = {}", user);
        MypageResponseDto mypageResponseDto = mypageService.findUserbyId(user.getUserNo());
        log.debug("mypageResponseDto = {}", mypageResponseDto);
        log.debug("mypageResponseDto.getEmail = {}", mypageResponseDto.getEmail());

        model.addAttribute("userInfo", mypageResponseDto);

        return "mypage/edit-profile";
    }

    /**
     * 마이페이지 > 비밀번호 수정 조회
     */
    @GetMapping("/changePassword")
    public String changePasswordPage(@AuthenticationPrincipal User user,

                                 Model model) {
        MypageResponseDto mypageResponseDto = mypageService.findUserbyId(user.getUserNo());
        model.addAttribute("userInfo", mypageResponseDto);
        return "mypage/change-password";
    }

    /**
     * 비밀번호 수정 처리
     */
    @PostMapping("/changePassword")
    public String changePassword(@AuthenticationPrincipal User user,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 Model model) {
        log.debug("currentPassword = {}", currentPassword);
        log.debug("newPassword = {}", newPassword);
        MypageResponseDto mypageResponseDto = mypageService.findUserbyId(user.getUserNo());
        model.addAttribute("userInfo", mypageResponseDto);
        return "redirect:/mypage";
    }

    /**
     * 마이페이지 > 탈퇴 비밀번호 확인 화면 이동
     * @param model
     * @return
     */
    @GetMapping("/confirmResign")
    public String confirmResign(Model model) {
        return "mypage/confirmResign";
    }

    /**
     * 탈퇴 비밀번호 확인 처리
     * @param currentPassword
     * @param model
     * @return
     */
    @PostMapping("/resignCheckPassword")
    public String resignCheckPassword(@RequestParam String currentPassword,Model model) {
        return "confirmResign";
    }

    /**
     * 탈퇴 처리
     * @param currentPassword
     * @param model
     * @return
     */
    @GetMapping("/resign")
    public String resign(@RequestParam String currentPassword,Model model) {
        return "/";
    }

}
