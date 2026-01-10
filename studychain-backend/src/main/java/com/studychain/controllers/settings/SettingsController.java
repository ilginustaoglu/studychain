package com.studychain.controllers.settings;

import com.studychain.models.User;
import com.studychain.repositories.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class SettingsController {

	private final UserRepository userRepository;

	public SettingsController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@GetMapping("/settings")
	public String settings(@RequestParam(value = "success", required = false) String success,
	                       @RequestParam(value = "error", required = false) String error,
	                       HttpSession session,
	                       Model model) {
		Object username = session.getAttribute("username");
		Long userId = (Long) session.getAttribute("userId");
		if (username == null || userId == null) {
			return "redirect:/auth/login";
		}
		Optional<User> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) {
			session.invalidate();
			return "redirect:/auth/login";
		}
		User user = userOpt.get();
		model.addAttribute("username", username);
		model.addAttribute("user", user);
		model.addAttribute("themePreference", user.getThemePreference() == null ? "dark" : user.getThemePreference());
		model.addAttribute("displayNameMode", user.getDisplayNameMode() == null ? "username" : user.getDisplayNameMode());
		model.addAttribute("showBirthDatePublic", user.getShowBirthDatePublic() == null ? true : user.getShowBirthDatePublic());
		model.addAttribute("showJoinedDatePublic", user.getShowJoinedDatePublic() == null ? true : user.getShowJoinedDatePublic());
		model.addAttribute("about", user.getAbout() == null ? "" : user.getAbout());
		if (success != null) model.addAttribute("success", success);
		if (error != null) model.addAttribute("error", error);
		return "settings/index";
	}

	@PostMapping("/settings/application/theme")
	public String updateTheme(@RequestParam("theme") String theme, HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) return "redirect:/auth/login";
		if (!"dark".equals(theme) && !"light".equals(theme)) {
			return "redirect:/settings?error=Invalid+theme";
		}
		return userRepository.findById(userId).map(user -> {
			user.setThemePreference(theme);
			userRepository.save(user);
			return "redirect:/settings?success=Theme+updated";
		}).orElse("redirect:/auth/login");
	}

	@PostMapping("/settings/account/email")
	public String updateEmail(@RequestParam("email") String email, HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) return "redirect:/auth/login";
		return userRepository.findById(userId).map(user -> {
			if (email == null || email.isBlank()) {
				return "redirect:/settings?error=Email+is+required";
			}
			if (email.equals(user.getEmail())) {
				return "redirect:/settings?success=Email+unchanged";
			}
			if (userRepository.existsByEmail(email)) {
				return "redirect:/settings?error=Email+is+already+in+use";
			}
			user.setEmail(email);
			userRepository.save(user);
			return "redirect:/settings?success=Email+updated";
		}).orElse("redirect:/auth/login");
	}

	@PostMapping("/settings/account/password")
	public String updatePassword(@RequestParam("currentPassword") String currentPassword,
	                             @RequestParam("newPassword") String newPassword,
	                             @RequestParam("confirmPassword") String confirmPassword,
	                             HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) return "redirect:/auth/login";
		return userRepository.findById(userId).map(user -> {
			if (currentPassword == null || !currentPassword.equals(user.getPassword())) {
				return "redirect:/settings?error=Current+password+is+incorrect";
			}
			if (newPassword == null || newPassword.length() < 4) {
				return "redirect:/settings?error=New+password+must+be+at+least+4+chars";
			}
			if (!newPassword.equals(confirmPassword)) {
				return "redirect:/settings?error=Passwords+do+not+match";
			}
			if (newPassword.equals(currentPassword)) {
				return "redirect:/settings?error=New+password+must+be+different";
			}
			user.setPassword(newPassword); // Note: plain text for demo parity
			userRepository.save(user);
			return "redirect:/settings?success=Password+updated";
		}).orElse("redirect:/auth/login");
	}

	@PostMapping("/settings/account/profile")
	public String updateProfile(@RequestParam("username") String username,
	                            @RequestParam("firstName") String firstName,
	                            @RequestParam("lastName") String lastName,
	                            HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) return "redirect:/auth/login";
		if (username == null || username.isBlank() || firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
			return "redirect:/settings?error=All+fields+are+required";
		}
		if (username.length() > 50 || firstName.length() > 80 || lastName.length() > 80) {
			return "redirect:/settings?error=Field+length+exceeded";
		}
		return userRepository.findById(userId).map(user -> {
			String currentUsername = user.getUsername();
			if (!username.equals(currentUsername) && userRepository.existsByUsername(username)) {
				return "redirect:/settings?error=Username+is+already+taken";
			}
			user.setUsername(username);
			user.setFirstName(firstName);
			user.setLastName(lastName);
			userRepository.save(user);
			// keep session username in sync
			session.setAttribute("username", username);
			return "redirect:/settings?success=Profile+updated";
		}).orElse("redirect:/auth/login");
	}

	@PostMapping("/settings/account/visibility")
	public String updateVisibility(@RequestParam(value = "showFullName", required = false) String showFullName,
	                               @RequestParam(value = "showBirthDate", required = false) String showBirthDate,
	                               @RequestParam(value = "showJoinedDate", required = false) String showJoinedDate,
	                               HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) return "redirect:/auth/login";
		return userRepository.findById(userId).map(user -> {
			user.setDisplayNameMode("on".equalsIgnoreCase(showFullName) ? "full_name" : "username");
			user.setShowBirthDatePublic("on".equalsIgnoreCase(showBirthDate));
			user.setShowJoinedDatePublic("on".equalsIgnoreCase(showJoinedDate));
			userRepository.save(user);
			return "redirect:/settings?success=Visibility+settings+updated";
		}).orElse("redirect:/auth/login");
	}

	@PostMapping("/settings/account/about")
	public String updateAbout(@RequestParam("about") String about, HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) return "redirect:/auth/login";
		return userRepository.findById(userId).map(user -> {
			if (about != null && about.length() > 2000) {
				return "redirect:/settings?error=About+is+too+long";
			}
			user.setAbout(about == null ? "" : about);
			userRepository.save(user);
			return "redirect:/settings?success=About+updated";
		}).orElse("redirect:/auth/login");
	}
}

