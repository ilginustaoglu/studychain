package com.studychain.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class GlobalModelAttributes {

    @ModelAttribute
    public void addSessionUserAttributes(Model model, HttpSession session) {
        Object username = session.getAttribute("username");
        boolean isLoggedIn = username != null;
        if (isLoggedIn) {
            model.addAttribute("username", username);
        }
        model.addAttribute("isLoggedIn", isLoggedIn);
    }
}


