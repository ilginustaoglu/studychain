package com.studychain.repositories;

import com.studychain.models.Flashcard;
import com.studychain.models.FlashcardCollection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {
	List<Flashcard> findAllByCollectionOrderByPositionAscIdAsc(FlashcardCollection collection);
	long countByCollection(FlashcardCollection collection);
}


