package com.wewe.temjaimusic.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";  // ชื่อไฟล์ login.html ใน resources/templates/
    }
}

