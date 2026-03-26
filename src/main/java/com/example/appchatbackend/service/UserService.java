package com.example.appchatbackend.service;

import com.example.appchatbackend.model.User;
import com.example.appchatbackend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    public User create(User user) {
        return userRepository.save(user);
    }

    public Optional<User> update(String id, User updatedUser) {
        Optional<User> optUser = userRepository.findById(id);
        if (optUser.isPresent()) {
            User existing = optUser.get();
            existing.setDisplayName(updatedUser.getDisplayName());
            existing.setEmail(updatedUser.getEmail());
            existing.setAvatarUrl(updatedUser.getAvatarUrl());
            existing.setBio(updatedUser.getBio());
            return Optional.of(userRepository.save(existing));
        }
        return Optional.empty();
    }

    public boolean existsById(String id) {
        return userRepository.existsById(id);
    }

    public void deleteById(String id) {
        userRepository.deleteById(id);
    }
}
