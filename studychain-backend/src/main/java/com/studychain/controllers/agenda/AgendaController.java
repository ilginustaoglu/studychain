package com.studychain.controllers.agenda;

import com.studychain.models.CalendarEvent;
import com.studychain.models.GoogleCalendarAccount;
import com.studychain.services.CalendarEventService;
import com.studychain.services.GoogleCalendarException;
import com.studychain.services.GoogleCalendarService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
public class AgendaController {

	private static final Logger log = LoggerFactory.getLogger(AgendaController.class);

	private final GoogleCalendarService googleCalendarService;

	public AgendaController(GoogleCalendarService googleCalendarService) {
		this.googleCalendarService = googleCalendarService;
	}

	@GetMapping("/agenda")
	public String agenda(@RequestParam(name = "google", required = false) String googleStatus,
	                     HttpSession session, Model model) {
		Object username = session.getAttribute("username");
		if (username == null) {
			return "redirect:/auth/login";
		}
		Long userId = (Long) session.getAttribute("userId");
		Optional<GoogleCalendarAccount> account = userId == null
			? Optional.empty()
			: googleCalendarService.findAccount(userId);
		model.addAttribute("username", username);
		model.addAttribute("googleConfigured", googleCalendarService.isConfigured());
		GoogleCalendarService.ScopeStatus scopes = account.map(googleCalendarService::scopeStatus)
			.orElse(new GoogleCalendarService.ScopeStatus(false, false));
		model.addAttribute("googleConnected", account.isPresent());
		model.addAttribute("googleScopeMissing", account.isPresent() && !scopes.calendar());
		model.addAttribute("googleColorScopeMissing", account.isPresent() && scopes.calendar() && !scopes.calendarList());
		model.addAttribute("googleEmail", account.map(GoogleCalendarAccount::getGoogleEmail).orElse(null));
		model.addAttribute("googleMessage", googleStatusMessage(googleStatus));
		return "agenda/index";
	}

	@GetMapping("/agenda/google/connect")
	public void connectGoogle(HttpSession session, HttpServletResponse response) throws IOException {
		if (session.getAttribute("userId") == null) {
			response.sendRedirect("/auth/login");
			return;
		}
		if (!googleCalendarService.isConfigured()) {
			response.sendRedirect("/agenda?google=not-configured");
			return;
		}
		String state = UUID.randomUUID().toString();
		session.setAttribute("googleOauthState", state);
		response.setStatus(HttpServletResponse.SC_FOUND);
		response.setHeader("Location", googleCalendarService.authorizationUrl(state));
	}

	@GetMapping("/agenda/google/callback")
	public String googleCallback(@RequestParam(required = false) String code,
	                             @RequestParam(required = false) String state,
	                             @RequestParam(required = false) String error,
	                             HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return "redirect:/auth/login";
		}
		String expectedState = (String) session.getAttribute("googleOauthState");
		session.removeAttribute("googleOauthState");
		if (error != null || code == null || expectedState == null || !expectedState.equals(state)) {
			return "redirect:/agenda?google=" + (error != null ? "denied" : "error");
		}
		try {
			googleCalendarService.connect(userId, code);
			return "redirect:/agenda?google=connected";
		} catch (GoogleCalendarException ex) {
			log.warn("Google Calendar connect failed: {}", ex.getMessage());
			String status = GoogleCalendarService.MISSING_CALENDAR_SCOPE.equals(ex.getMessage())
				? "missing-scope"
				: "error";
			return "redirect:/agenda?google=" + status;
		}
	}

	@PostMapping("/agenda/google/disconnect")
	public String disconnectGoogle(HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return "redirect:/auth/login";
		}
		googleCalendarService.disconnect(userId);
		return "redirect:/agenda?google=disconnected";
	}

	private static String googleStatusMessage(String status) {
		if (status == null) {
			return null;
		}
		return switch (status) {
			case "connected" -> "Google Calendar connected. Events sync both ways.";
			case "disconnected" -> "Google Calendar disconnected. Events already here stay in StudyChain.";
			case "denied" -> "Google permission was not granted.";
			case "not-configured" -> "Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET, then restart the app.";
			case "missing-scope" -> "Google did not grant Calendar access. Add the Calendar scope, then connect again.";
			case "error" -> "Could not connect Google Calendar. Check the OAuth client and try again.";
			default -> null;
		};
	}

	@RestController
	@RequestMapping("/api/calendar/events")
	public static class CalendarEventApiController {
		private final CalendarEventService eventService;

		public CalendarEventApiController(CalendarEventService eventService) {
			this.eventService = eventService;
		}

		@GetMapping
		public ResponseEntity<List<CalendarEvent>> list(
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			HttpSession session) {
			Long userId = (Long) session.getAttribute("userId");
			if (userId == null) {
				return ResponseEntity.status(401).build();
			}
			try {
				LocalDateTime startTime = parseQueryDate(start);
				LocalDateTime endTime = parseQueryDate(end);
				if (startTime != null && endTime != null) {
					return ResponseEntity.ok(eventService.findEventsInRange(userId, startTime, endTime));
				}
			} catch (DateTimeParseException ex) {
				return ResponseEntity.badRequest().build();
			}
			return ResponseEntity.ok(eventService.findAllByUserId(userId));
		}

		private static LocalDateTime parseQueryDate(String value) {
			if (value == null || value.isBlank()) {
				return null;
			}
			try {
				return OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
			} catch (DateTimeParseException ignored) {
				// Fall through to local date-time.
			}
			try {
				return Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDateTime();
			} catch (DateTimeParseException ignored) {
				// Fall through to a value without a zone.
			}
			return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
		}

		@PostMapping
		public ResponseEntity<CalendarEvent> create(@RequestBody CalendarEventRequest request, HttpSession session) {
			Long userId = (Long) session.getAttribute("userId");
			if (userId == null) {
				return ResponseEntity.status(401).build();
			}
			Optional<CalendarEvent> created = eventService.create(
				userId, request.title, request.description,
				request.startTime, request.endTime, request.allDay, request.color
			);
			return created.map(e -> ResponseEntity.ok(e))
				.orElseGet(() -> ResponseEntity.badRequest().build());
		}

		@PutMapping("/{id}")
		public ResponseEntity<CalendarEvent> update(@PathVariable Long id, @RequestBody CalendarEventRequest request, HttpSession session) {
			Long userId = (Long) session.getAttribute("userId");
			if (userId == null) {
				return ResponseEntity.status(401).build();
			}
			Optional<CalendarEvent> updated = eventService.update(
				userId, id, request.title, request.description,
				request.startTime, request.endTime, request.allDay, request.color
			);
			return updated.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
		}

		@DeleteMapping("/{id}")
		public ResponseEntity<Void> delete(@PathVariable Long id, HttpSession session) {
			Long userId = (Long) session.getAttribute("userId");
			if (userId == null) {
				return ResponseEntity.status(401).build();
			}
			boolean removed = eventService.delete(userId, id);
			return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
		}
	}

	public static class CalendarEventRequest {
		public String title;
		public String description;
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		public LocalDateTime startTime;
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		public LocalDateTime endTime;
		public boolean allDay;
		public String color;
	}
}

