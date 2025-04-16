package com.wb.between.reservation.reserve.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ReservationController {

    @GetMapping("/reservation")
    public String reservation(Model model) {

        return "reservation/reservation";
    }


}
