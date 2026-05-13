package com.mkoba.mkoba_system.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
    
    @GetMapping("/")
    public String index() {
        return "welcome";
    }
    
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
