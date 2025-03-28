package com.wb.between.user.controller;

import com.wb.between.user.domain.User;
import com.wb.between.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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

}
