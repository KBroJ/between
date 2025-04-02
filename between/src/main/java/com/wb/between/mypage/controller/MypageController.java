package com.wb.between.mypage.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mypage")
public class MypageController {

    @GetMapping("/")
    public String mypage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null){

        }
        return "mypage/mypage";
    }

    @GetMapping("/edit")
    public String editProfile(Model model) {
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
