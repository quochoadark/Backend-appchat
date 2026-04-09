package com.example.appchatbackend.feature.user;

import com.example.appchatbackend.exception.DuplicateResourceException;
import com.example.appchatbackend.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * UserServiceImpl — implementation của UserService.
 *
 * Lưu ý quan trọng ở hàm create():
 * - Kiểm tra trùng email/username trước khi lưu → ném DuplicateResourceException (409)
 * - Hash mật khẩu bằng BCrypt trước khi lưu → không lưu plaintext vào DB
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", id));
    }

    @Override
    public Page<User> search(String keyword, Pageable pageable) {
        return userRepository.searchByKeyword(keyword, pageable);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "email", email));
    }

    @Override
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

    @Override
    public User update(String id, User updatedUser) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", id));

        existing.setDisplayName(updatedUser.getDisplayName());
        existing.setEmail(updatedUser.getEmail());
        existing.setAvatarUrl(updatedUser.getAvatarUrl());
        existing.setBio(updatedUser.getBio());
        return userRepository.save(existing);
    }

    @Override
    public void deleteById(String id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Người dùng", "id", id);
        }
        userRepository.deleteById(id);
    }
}
