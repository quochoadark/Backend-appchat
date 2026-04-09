package com.example.appchatbackend.feature.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository — truy cập MongoDB collection "users".
 * Spring Data tự sinh implementation tại runtime từ tên method và @Query.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    /** Kiểm tra email đã tồn tại chưa (dùng khi đăng ký) — nhanh hơn findByEmail vì không fetch document */
    boolean existsByEmail(String email);

    /** Kiểm tra username đã tồn tại chưa (dùng khi đăng ký) */
    boolean existsByUsername(String username);

    /** Lấy nhiều user theo danh sách ID — dùng khi lấy danh sách bạn bè */
    List<User> findByIdIn(List<String> ids);

    /**
     * Tìm kiếm user theo keyword, không phân biệt hoa thường ($options: 'i').
     * Tìm trên cả username và display_name.
     * Hỗ trợ phân trang (Pageable).
     */
    @Query("{ '$or': [ { 'username': { '$regex': ?0, '$options': 'i' } }, { 'display_name': { '$regex': ?0, '$options': 'i' } } ] }")
    Page<User> searchByKeyword(String keyword, Pageable pageable);
}
