package com.wb.between.faq.controller;

import com.wb.between.faq.dto.FaQListViewResponse;
import com.wb.between.faq.service.FaQService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@RequiredArgsConstructor
@Controller
public class FaQViewController {

    private final FaQService faQService;

    @GetMapping("/faqList")
    public String getFaq(Model model){
        List<FaQListViewResponse> faqList = faQService.findAll().stream()
                .map(FaQListViewResponse::new)
                .toList();

        model.addAttribute("faqList", faqList);

        return "/FaQ/faQ";

    }
}
