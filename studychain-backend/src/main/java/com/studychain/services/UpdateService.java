package com.studychain.services;

import com.studychain.models.UpdatePost;
import com.studychain.models.User;
import com.studychain.repositories.UpdatePostRepository;
import com.studychain.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UpdateService {

    private final UpdatePostRepository updatePostRepository;
    private final UserRepository userRepository;

    public UpdateService(UpdatePostRepository updatePostRepository, UserRepository userRepository) {
        this.updatePostRepository = updatePostRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public UpdatePost create(Long adminUserId, String title, String content) {
        User admin = userRepository.findById(adminUserId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        UpdatePost post = new UpdatePost();
        post.setTitle(title);
        post.setContent(content);
        post.setCreatedBy(admin);
        return updatePostRepository.save(post);
    }

    @Transactional(readOnly = true)
    public List<UpdatePost> listAll() {
        return updatePostRepository.findAllByOrderByCreatedAtDesc();
    }
}


