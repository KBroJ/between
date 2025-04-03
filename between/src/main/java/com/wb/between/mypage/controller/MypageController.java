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
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final MypageService mypageService;

    @GetMapping
    public String mypage(@AuthenticationPrincipal User user, Model model) {

        log.debug("user = {}", user);
        MypageResponseDto mypageResponseDto = mypageService.findUserbyId(user.getUserNo());

        model.addAttribute("userInfo", mypageResponseDto);

        return "mypage/dashboard";
    }

    @GetMapping("/edit")
    public String editProfile(@AuthenticationPrincipal User user, Model model) {

        log.debug("user = {}", user);
        MypageResponseDto mypageResponseDto = mypageService.findUserbyId(user.getUserNo());
        log.debug("mypageResponseDto = {}", mypageResponseDto);
        log.debug("mypageResponseDto.getEmail = {}", mypageResponseDto.getEmail());

        model.addAttribute("userInfo", mypageResponseDto);

        return "mypage/edit-profile";
    }

    @GetMapping("/chagePassword")
    public String chagePassword(Model model) {
        return "mypage/change-password";
    }

    @GetMapping("/resign")
    public String resign(Model model) {
        return "mypage/resign";
    }

}
