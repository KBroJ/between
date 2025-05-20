package com.wb.between.main.controller;

import com.wb.between.banner.dto.BannerListResponseDto;
import com.wb.between.banner.service.BannerService;
import com.wb.between.common.exception.CustomException;
import com.wb.between.common.exception.ErrorCode;
import com.wb.between.popup.dto.PopupResDto;
import com.wb.between.popup.service.PopupService;
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

    //배너
    private final BannerService bannerService;

    //팝업
    private final PopupService popupService;

    @GetMapping("/")
    public String main(Model model) {
        List<BannerListResponseDto> bannerList = bannerService.findBannerList();
        List<PopupResDto> popupList = popupService.popups();

        model.addAttribute("bannerList", bannerList);
        model.addAttribute("popupList", popupList);
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
