package com.studychain.services;

import com.studychain.models.Flashcard;
import com.studychain.models.FlashcardCollection;
import com.studychain.models.User;
import com.studychain.repositories.FlashcardCollectionRepository;
import com.studychain.repositories.FlashcardRepository;
import com.studychain.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class FlashcardService {

	private final UserRepository userRepository;
	private final FlashcardCollectionRepository collectionRepository;
	private final FlashcardRepository flashcardRepository;

	public FlashcardService(UserRepository userRepository,
	                        FlashcardCollectionRepository collectionRepository,
	                        FlashcardRepository flashcardRepository) {
		this.userRepository = userRepository;
		this.collectionRepository = collectionRepository;
		this.flashcardRepository = flashcardRepository;
	}

	public Optional<User> getUser(Long userId) {
		return userRepository.findById(userId);
	}

	public List<FlashcardCollection> listCollections(User user) {
		return collectionRepository.findAllByUserOrderByUpdatedAtDesc(user);
	}

	@Transactional
	public Optional<FlashcardCollection> createCollection(Long userId, String name, String description) {
		if (name == null || name.isBlank()) return Optional.empty();
		return userRepository.findById(userId).map(user -> {
			FlashcardCollection c = new FlashcardCollection();
			c.setUser(user);
			c.setName(name.trim());
			c.setDescription(description == null ? null : description.trim());
			return collectionRepository.save(c);
		});
	}

	public Optional<FlashcardCollection> getUserCollection(Long userId, Long collectionId) {
		return userRepository.findById(userId).flatMap(user -> collectionRepository.findByIdAndUser(collectionId, user));
	}

	public List<Flashcard> listCards(FlashcardCollection collection) {
		return flashcardRepository.findAllByCollectionOrderByPositionAscIdAsc(collection);
	}

	public Optional<FlashcardCollection> getCollectionById(Long id) {
		return collectionRepository.findById(id);
	}

	@Transactional
	public Optional<Flashcard> addCard(Long userId, Long collectionId, String frontText, String backText) {
		if (frontText == null || frontText.isBlank() || backText == null || backText.isBlank()) {
			return Optional.empty();
		}
		return getUserCollection(userId, collectionId).map(collection -> {
			Flashcard card = new Flashcard();
			card.setCollection(collection);
			card.setFrontText(frontText.trim());
			card.setBackText(backText.trim());
			card.setPosition(null);
			return flashcardRepository.save(card);
		});
	}

	@Transactional
	public boolean deleteCollection(Long userId, Long collectionId) {
		Optional<User> u = userRepository.findById(userId);
		if (u.isEmpty()) return false;
		User user = u.get();
		boolean isAdmin = user.getRole() == com.studychain.models.UserRole.ADMIN;
		Optional<FlashcardCollection> col = isAdmin ? collectionRepository.findById(collectionId)
			: collectionRepository.findByIdAndUser(collectionId, user);
		if (col.isEmpty()) return false;
		collectionRepository.delete(col.get());
		return true;
	}
}


