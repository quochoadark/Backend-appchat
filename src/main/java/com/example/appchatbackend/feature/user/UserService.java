package com.example.appchatbackend.feature.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    List<User> findAll();

    Page<User> search(String keyword, Pageable pageable);

    User findById(String id);

    User findByEmail(String email);

    User create(User user);

    User update(String id, User user);

    void deleteById(String id);
}
