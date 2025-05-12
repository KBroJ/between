package com.wb.between.admin.faq.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class adminFaQViewController {

    @GetMapping("/admin/faq_admin")
    public String viewFaQ(){

        return "/admin/faq/faq_admin";
    }

    @GetMapping("/admin/faq_form")
    public String faqForm(){
        return "/admin/faq/faq_form";
    }
}
