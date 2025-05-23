package com.wb.between.pay.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaymentViewController {

    @GetMapping("/payment-success")
    public String payment(Model model){
        return "/reservation/payment-success";
    }
}
