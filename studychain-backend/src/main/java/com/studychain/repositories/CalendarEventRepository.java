package com.studychain.repositories;

import com.studychain.models.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {
	List<CalendarEvent> findAllByUser_IdOrderByStartTimeAsc(Long userId);
	List<CalendarEvent> findAllByUser_IdAndStartTimeBetweenOrderByStartTimeAsc(
		Long userId, LocalDateTime start, LocalDateTime end
	);
}

