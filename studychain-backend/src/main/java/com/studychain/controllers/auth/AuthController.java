package com.studychain.controllers.auth;

import com.studychain.models.User;
import com.studychain.services.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import com.studychain.dto.SignupForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/auth/login")
    public String loginPage(Model model) {
        model.addAttribute("email", "");
        return "auth/login";
    }

    @PostMapping("/auth/login")
    public String loginSubmit(@RequestParam String identifier,
                              @RequestParam String password,
                              HttpSession session,
                              Model model) {
        if (userService.authenticateByIdentifier(identifier, password)) {
            userService.findByIdentifier(identifier).ifPresent(user -> {
                session.setAttribute("username", user.getUsername()); // compatibility key; stores username
                session.setAttribute("userId", user.getId());
            });
            return "redirect:/home";
        }
        model.addAttribute("error", "Invalid email/username or password");
        return "auth/login";
    }

    @GetMapping("/auth/signup")
    public String signupPage(Model model) {
        model.addAttribute("form", new SignupForm());
        return "auth/signup";
    }

    @PostMapping("/auth/signup")
    public String signupSubmit(@Valid @ModelAttribute("form") SignupForm form,
                               BindingResult bindingResult,
                               Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }
        try {
            if (!form.getPassword().equals(form.getConfirmPassword())) {
                model.addAttribute("error", "Passwords do not match");
                return "auth/signup";
            }
            userService.register(
                form.getUsername(),
                form.getEmail(),
                form.getFirstName(),
                form.getLastName(),
                form.getBirthDate(),
                form.getPassword()
            );
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/signup";
        }
        return "redirect:/auth/login";
    }

    @PostMapping("/auth/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}


