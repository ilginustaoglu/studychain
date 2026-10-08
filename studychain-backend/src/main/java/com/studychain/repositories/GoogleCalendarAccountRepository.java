package com.studychain.repositories;

import com.studychain.models.GoogleCalendarAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoogleCalendarAccountRepository extends JpaRepository<GoogleCalendarAccount, Long> {
	Optional<GoogleCalendarAccount> findByUserId(Long userId);

	void deleteByUserId(Long userId);
}
