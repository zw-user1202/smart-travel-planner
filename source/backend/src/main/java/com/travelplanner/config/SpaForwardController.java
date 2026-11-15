package com.travelplanner.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping({
            "/",
            "/login",
            "/trips",
            "/trips/new",
            "/trips/{id:[0-9]+}",
            "/admin",
            "/routes",
            "/costs",
            "/footprint",
            "/checklist"
    })
    public String spa() {
        return "forward:/index.html";
    }
}
