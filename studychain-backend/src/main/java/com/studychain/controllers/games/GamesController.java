package com.studychain.controllers.games;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GamesController {

	@GetMapping("/games")
	public String games(HttpSession session, Model model) {
		Object username = session.getAttribute("username");
		if (username == null) {
			return "redirect:/auth/login";
		}
		model.addAttribute("username", username);
		return "games/index";
	}
}


