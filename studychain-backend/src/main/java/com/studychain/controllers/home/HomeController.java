package com.studychain.controllers.home;

import com.studychain.models.User;
import com.studychain.models.UserRole;
import com.studychain.repositories.UserRepository;
import com.studychain.services.MusicService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
public class HomeController {

    private final MusicService musicService;
    private final UserRepository userRepository;

    public HomeController(MusicService musicService, UserRepository userRepository) {
        this.musicService = musicService;
        this.userRepository = userRepository;
    }

    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        Object username = session.getAttribute("username");
        if (username == null) {
            return "redirect:/auth/login";
        }
        model.addAttribute("username", username);

        Object uidObj = session.getAttribute("userId");
        boolean isAdmin = false;
        if (uidObj instanceof Long uid) {
            Optional<User> user = userRepository.findById(uid);
            isAdmin = user.map(u -> u.getRole() == UserRole.ADMIN).orElse(false);
        }
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("musicList", musicService.listAll());
        return "home/index";
    }
}


