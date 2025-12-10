package com.studychain.controllers.landing;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LandingController {

    @GetMapping({"/", "/landing"})
	public String index(HttpSession session) {
		Object username = session.getAttribute("username");
		if (username != null) {
			return "redirect:/home";
		}
		return "landing/index";
    }
}


