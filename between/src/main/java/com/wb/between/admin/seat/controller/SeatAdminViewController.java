package com.wb.between.admin.seat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@RequiredArgsConstructor
@Controller
public class SeatAdminViewController {

    @GetMapping("/admin/adminSeat")
    public String viewSeatAdmin(){
        return "/admin/seat/adminSeat";
    }

    @GetMapping("/admin/adminSeatForm")
    public String formSeatAdmin(){
        return "/admin/seat/adminSeatForm";
    }


}
