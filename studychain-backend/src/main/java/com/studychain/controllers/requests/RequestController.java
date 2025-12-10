package com.studychain.controllers.requests;

import com.studychain.models.Request;
import com.studychain.models.UpdatePost;
import com.studychain.models.User;
import com.studychain.models.UserRole;
import com.studychain.repositories.UserRepository;
import com.studychain.services.MailService;
import com.studychain.services.RequestService;
import com.studychain.services.UpdateService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
public class RequestController {

    private final RequestService requestService;
    private final UpdateService updateService;
    private final UserRepository userRepository;
    private final MailService mailService;

    public RequestController(RequestService requestService,
                             UpdateService updateService,
                             UserRepository userRepository,
                             MailService mailService) {
        this.requestService = requestService;
        this.updateService = updateService;
        this.userRepository = userRepository;
        this.mailService = mailService;
    }

    @GetMapping("/requests")
    public String page(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        Long userId = (Long) session.getAttribute("userId");
        model.addAttribute("username", username);

        Optional<User> maybeUser = userId != null ? userRepository.findById(userId) : Optional.empty();
        boolean isAdmin = maybeUser.map(u -> u.getRole() == UserRole.ADMIN).orElse(false);
        model.addAttribute("isAdmin", isAdmin);

        List<UpdatePost> updates = updateService.listAll();
        model.addAttribute("updates", updates);

        List<Request> requests = Collections.emptyList();
        if (isAdmin) {
            requests = requestService.findAllForAdmin();
        } else if (userId != null) {
            requests = requestService.findAllForUser(userId);
        }
        model.addAttribute("requests", requests);
        return "requests/index";
    }

    @PostMapping("/requests")
    public String submit(@RequestParam("content") String content, HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        if (!StringUtils.hasText(content)) {
            return "redirect:/requests?error=empty";
        }
        Request created = requestService.create(userId, content.trim());

        // Notify admin via email
        Optional<User> user = userRepository.findById(userId);
        String subject = "[StudyChain] New Request from " + user.map(User::getUsername).orElse("unknown");
        String body = "User: " + user.map(User::getUsername).orElse("unknown")
            + " (" + user.map(User::getEmail).orElse("-") + ")\n"
            + "Request ID: " + created.getId() + "\n\n"
            + created.getContent();
        mailService.sendToAdmin(subject, body, user.map(User::getEmail).orElse(null));

        return "redirect:/requests?ok=1";
    }

    @PostMapping("/requests/updates")
    public String createUpdate(@RequestParam("title") String title,
                               @RequestParam("content") String content,
                               HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRole() != UserRole.ADMIN) {
            return "redirect:/requests?error=forbidden";
        }
        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            return "redirect:/requests?error=empty";
        }
        updateService.create(userId, title.trim(), content.trim());
        return "redirect:/requests?ok=1";
    }
}


