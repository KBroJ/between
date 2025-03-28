package com.wb.between.main;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class testMainController {

    @GetMapping("/") // 처음 호출 시 열리는 페이지 맵핑(localhost:8080/)
    public String home() {
        return "main/test";
    }

}
