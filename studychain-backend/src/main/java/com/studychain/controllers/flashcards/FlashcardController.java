package com.studychain.controllers.flashcards;

import com.studychain.models.FlashcardCollection;
import com.studychain.models.User;
import com.studychain.models.UserRole;
import com.studychain.services.FlashcardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class FlashcardController {

	private final FlashcardService flashcardService;

	public FlashcardController(FlashcardService flashcardService) {
		this.flashcardService = flashcardService;
	}

	@GetMapping("/flashcards")
	public String index(HttpSession session, Model model) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return "redirect:/auth/login";
		}
		Optional<User> userOpt = flashcardService.getUser(userId);
		if (userOpt.isEmpty()) {
			return "redirect:/auth/login";
		}
		model.addAttribute("collections", flashcardService.listCollections(userOpt.get()));
		return "flashcards/index";
	}

	@PostMapping("/flashcards/collections")
	public String createCollection(@RequestParam String name,
	                               @RequestParam(required = false) String description,
	                               HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return "redirect:/auth/login";
		}
		Optional<FlashcardCollection> created = flashcardService.createCollection(userId, name, description);
		if (created.isEmpty()) {
			return "redirect:/flashcards?error=invalid";
		}
		return "redirect:/flashcards/collections/" + created.get().getId();
	}

	@GetMapping("/flashcards/collections/{id}")
	public String viewCollection(@PathVariable("id") Long collectionId,
	                             HttpSession session,
	                             Model model) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return "redirect:/auth/login";
		}
		Optional<User> userOpt = flashcardService.getUser(userId);
		if (userOpt.isEmpty()) return "redirect:/auth/login";
		User user = userOpt.get();
		boolean isAdmin = user.getRole() == UserRole.ADMIN;
		Optional<FlashcardCollection> colOpt = isAdmin
			? flashcardService.getCollectionById(collectionId)
			: flashcardService.getUserCollection(userId, collectionId);
		return colOpt.map(col -> {
			model.addAttribute("collection", col);
			model.addAttribute("cards", flashcardService.listCards(col));
			model.addAttribute("isAdmin", isAdmin);
			return "flashcards/collection";
		}).orElse("redirect:/flashcards?error=notfound");
	}

	@PostMapping("/flashcards/collections/{id}/cards")
	public String addCard(@PathVariable("id") Long collectionId,
	                      @RequestParam("frontText") String frontText,
	                      @RequestParam("backText") String backText,
	                      HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return "redirect:/auth/login";
		}
		boolean ok = flashcardService.addCard(userId, collectionId, frontText, backText).isPresent();
		if (!ok) {
			return "redirect:/flashcards/collections/" + collectionId + "?error=invalid";
		}
		return "redirect:/flashcards/collections/" + collectionId;
	}

	@GetMapping("/flashcards/collections/{id}/study")
	public String study(@PathVariable("id") Long collectionId,
	                    HttpSession session,
	                    Model model) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) return "redirect:/auth/login";
		Optional<User> userOpt = flashcardService.getUser(userId);
		if (userOpt.isEmpty()) return "redirect:/auth/login";
		User user = userOpt.get();
		boolean isAdmin = user.getRole() == UserRole.ADMIN;
		Optional<FlashcardCollection> colOpt = isAdmin
			? flashcardService.getCollectionById(collectionId)
			: flashcardService.getUserCollection(userId, collectionId);
		return colOpt.map(col -> {
			model.addAttribute("collection", col);
			model.addAttribute("cards", flashcardService.listCards(col));
			model.addAttribute("isAdmin", isAdmin);
			return "flashcards/study";
		}).orElse("redirect:/flashcards?error=notfound");
	}
}


