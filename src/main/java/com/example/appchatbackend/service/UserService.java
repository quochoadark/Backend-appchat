package com.example.appchatbackend.service;

import com.example.appchatbackend.exception.DuplicateResourceException;
import com.example.appchatbackend.exception.ResourceNotFoundException;
import com.example.appchatbackend.model.User;
import com.example.appchatbackend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", id));
    }

    public User create(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("Người dùng", "email", user.getEmail());
        }
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new DuplicateResourceException("Người dùng", "username", user.getUsername());
        }
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        return userRepository.save(user);
    }

    public User update(String id, User updatedUser) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", id));

        existing.setDisplayName(updatedUser.getDisplayName());
        existing.setEmail(updatedUser.getEmail());
        existing.setAvatarUrl(updatedUser.getAvatarUrl());
        existing.setBio(updatedUser.getBio());
        return userRepository.save(existing);
    }

    public void deleteById(String id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Người dùng", "id", id);
        }
        userRepository.deleteById(id);
    }
}
