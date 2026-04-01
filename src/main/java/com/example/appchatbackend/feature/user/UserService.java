package com.example.appchatbackend.feature.user;

import java.util.List;

public interface UserService {

    List<User> findAll();

    User findById(String id);

    User create(User user);

    User update(String id, User user);

    void deleteById(String id);
}
