package com.twitter.social.service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/social")
    public String socialHome() {
        return "Social Service is running";
    }
}