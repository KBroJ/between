package com.wb.between.menu.controller;

import com.wb.between.common.cache.MenuCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MenuTestController {

    private final MenuCache menuCache;

    @GetMapping("/menu-test")
    public String menuTest(Model model) {
        menuCache.reload();
        return "menu reloaded";
    }
}
