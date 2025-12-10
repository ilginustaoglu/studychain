package com.studychain.services;

import com.studychain.models.Request;
import com.studychain.models.User;
import com.studychain.repositories.RequestRepository;
import com.studychain.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RequestService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;

    public RequestService(RequestRepository requestRepository, UserRepository userRepository) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Request create(Long userId, String content) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Request req = new Request();
        req.setUser(user);
        req.setContent(content);
        return requestRepository.save(req);
    }

    @Transactional(readOnly = true)
    public List<Request> findAllForAdmin() {
        return requestRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Request> findAllForUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return requestRepository.findByUserOrderByCreatedAtDesc(user);
    }
}


