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

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public Optional<User> updateUser(String id, User updatedUser) {
        return userRepository.findById(id).map(existing -> {
            existing.setDisplayName(updatedUser.getDisplayName());
            existing.setEmail(updatedUser.getEmail());
            existing.setAvatarUrl(updatedUser.getAvatarUrl());
            existing.setBio(updatedUser.getBio());
            return userRepository.save(existing);
        });
    }

    public boolean deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            return false;
        }
        userRepository.deleteById(id);
        return true;
    }
}
