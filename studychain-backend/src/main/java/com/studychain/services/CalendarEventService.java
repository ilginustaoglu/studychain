package com.studychain.services;

import com.studychain.models.CalendarEvent;
import com.studychain.models.User;
import com.studychain.repositories.CalendarEventRepository;
import com.studychain.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CalendarEventService {

	private static final Logger log = LoggerFactory.getLogger(CalendarEventService.class);

	private final CalendarEventRepository eventRepository;
	private final UserRepository userRepository;
	private final GoogleCalendarService googleCalendarService;

	public CalendarEventService(CalendarEventRepository eventRepository, UserRepository userRepository,
	                            GoogleCalendarService googleCalendarService) {
		this.eventRepository = eventRepository;
		this.userRepository = userRepository;
		this.googleCalendarService = googleCalendarService;
	}

	public List<CalendarEvent> findAllByUserId(Long userId) {
		return eventRepository.findAllByUser_IdOrderByStartTimeAsc(userId);
	}

	public List<CalendarEvent> findEventsInRange(Long userId, LocalDateTime start, LocalDateTime end) {
		try {
			googleCalendarService.syncRange(userId, start, end);
		} catch (Exception ex) {
			log.warn("Google Calendar sync failed for user {}: {}", userId, ex.getMessage());
		}
		return eventRepository.findAllByUser_IdAndStartTimeBetweenOrderByStartTimeAsc(userId, start, end);
	}

	public Optional<CalendarEvent> findById(Long id) {
		return eventRepository.findById(id);
	}

	public Optional<CalendarEvent> findByIdAndUserId(Long id, Long userId) {
		return eventRepository.findById(id).filter(e -> e.getUser().getId().equals(userId));
	}

	@Transactional
	public Optional<CalendarEvent> create(Long userId, String title, String description,
	                                      LocalDateTime startTime, LocalDateTime endTime,
	                                      boolean allDay, String color) {
		return userRepository.findById(userId).map(user -> {
			CalendarEvent event = new CalendarEvent();
			event.setUser(user);
			event.setTitle(title);
			event.setDescription(description);
			event.setStartTime(startTime);
			event.setEndTime(endTime);
			event.setAllDay(allDay);
			event.setColor(color != null ? color : "#6366f1");
			CalendarEvent saved = eventRepository.save(event);
			googleCalendarService.pushCreated(saved);
			return saved;
		});
	}

	@Transactional
	public Optional<CalendarEvent> update(Long userId, Long eventId, String title, String description,
	                                     LocalDateTime startTime, LocalDateTime endTime,
	                                     boolean allDay, String color) {
		return findByIdAndUserId(eventId, userId).map(event -> {
			event.setTitle(title);
			event.setDescription(description);
			event.setStartTime(startTime);
			event.setEndTime(endTime);
			event.setAllDay(allDay);
			if (color != null) {
				event.setColor(color);
			}
			CalendarEvent saved = eventRepository.save(event);
			googleCalendarService.pushUpdated(saved);
			return saved;
		});
	}

	@Transactional
	public boolean delete(Long userId, Long eventId) {
		return findByIdAndUserId(eventId, userId).map(event -> {
			googleCalendarService.deleteRemote(userId, event.getGoogleEventId(), event.getGoogleCalendarId());
			eventRepository.delete(event);
			return true;
		}).orElse(false);
	}
}

