package com.studychain.controllers.agenda;

import com.studychain.models.CalendarEvent;
import com.studychain.services.CalendarEventService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
public class AgendaController {

	private final CalendarEventService eventService;

	public AgendaController(CalendarEventService eventService) {
		this.eventService = eventService;
	}

	@GetMapping("/agenda")
	public String agenda(HttpSession session, Model model) {
		Object username = session.getAttribute("username");
		if (username == null) {
			return "redirect:/auth/login";
		}
		model.addAttribute("username", username);
		return "agenda/index";
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
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
			HttpSession session) {
			Long userId = (Long) session.getAttribute("userId");
			if (userId == null) {
				return ResponseEntity.status(401).build();
			}
			if (start != null && end != null) {
				return ResponseEntity.ok(eventService.findEventsInRange(userId, start, end));
			}
			return ResponseEntity.ok(eventService.findAllByUserId(userId));
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

