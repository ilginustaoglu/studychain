package com.studychain.services;

import com.studychain.models.User;
import com.studychain.models.UserRole;
import com.studychain.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.time.LocalDate;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
	    public User register(String username, String email, String firstName, String lastName, LocalDate birthDate, String password) {
	        if (userRepository.existsByUsername(username)) {
	            throw new IllegalArgumentException("Username is already taken");
	        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }
        User user = new User();
	        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setBirthDate(birthDate);
        user.setPassword(password); // Not: Demo amaçlı düz metin. İleride hash'e geçilecek.
        user.setRole(UserRole.USER);
        return userRepository.save(user);
    }

	    public boolean authenticateByIdentifier(String identifier, String password) {
	        Optional<User> user = userRepository.findByEmail(identifier);
	        if (user.isEmpty()) {
	            user = userRepository.findByUsername(identifier);
	        }
        return user.filter(value -> value.getPassword().equals(password)).isPresent();
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

	    public Optional<User> findByIdentifier(String identifier) {
	        Optional<User> user = userRepository.findByEmail(identifier);
	        return user.isPresent() ? user : userRepository.findByUsername(identifier);
	    }
}


