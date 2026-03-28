package com.mivestreaming.website;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WelcomeController {

    @GetMapping("/api/welcome")
    public String welcome() {
        return "Welcome to my web taitri";
    }

}
