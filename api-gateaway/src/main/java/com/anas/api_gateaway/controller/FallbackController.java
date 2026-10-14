package com.anas.api_gateaway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallbackController {

    @GetMapping("/fallback/users")
    public String usersFallback() {
        return "User Service is currently unavailable. Please try again later.";
    }
}