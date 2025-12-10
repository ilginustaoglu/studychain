package com.studychain.repositories;

import com.studychain.models.FlashcardCollection;
import com.studychain.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FlashcardCollectionRepository extends JpaRepository<FlashcardCollection, Long> {
	List<FlashcardCollection> findAllByUserOrderByUpdatedAtDesc(User user);
	Optional<FlashcardCollection> findByIdAndUser(Long id, User user);
}


