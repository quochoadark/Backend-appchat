package com.example.appchatbackend.feature.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findByIdIn(List<String> ids);

    @Query("{ '$or': [ { 'username': { '$regex': ?0, '$options': 'i' } }, { 'display_name': { '$regex': ?0, '$options': 'i' } } ] }")
    Page<User> searchByKeyword(String keyword, Pageable pageable);
}
