package com.wb.between.main.controller;

import com.wb.between.banner.dto.BannerListResponseDto;
import com.wb.between.banner.service.BannerService;
import com.wb.between.common.exception.CustomException;
import com.wb.between.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MainController {

    private final BannerService bannerService;

    @GetMapping("/")
    public String main(Model model) {
        List<BannerListResponseDto> bannerList = bannerService.findBannerList();
        model.addAttribute("bannerList", bannerList);
        return "main/main";
    }

    @GetMapping("/user/login")
    public String login(Model model) {

        return "user/login";
    }


    @GetMapping("/user/join")
    public String join(Model model) {

        return "user/join";
    }


    @GetMapping("/custom-error")
    public String error(Model model) {
        throw new CustomException(ErrorCode.INTERNAL_ERROR);
    }


}
