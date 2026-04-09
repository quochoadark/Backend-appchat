package com.example.appchatbackend.config;

import com.example.appchatbackend.feature.user.User;
import com.example.appchatbackend.feature.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * CustomUserDetailsService — triển khai UserDetailsService của Spring Security.
 *
 * Spring Security dùng interface UserDetailsService để biết cách tải thông tin
 * người dùng khi xác thực (login). Class này override lại logic đó:
 * thay vì tìm theo username, ta tìm theo email trong MongoDB.
 *
 * Được Spring Security tự động phát hiện qua @Service và dùng trong quá trình
 * xác thực (AuthenticationManager).
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Tải thông tin người dùng từ database dựa trên email.
     *
     * - Spring Security gọi hàm này khi cần xác thực người dùng (vd: lúc login).
     * - Trả về UserDetails (gồm username, passwordHash, roles) để Spring Security
     *   tự so sánh mật khẩu và cấp quyền.
     * - Nếu không tìm thấy email → ném UsernameNotFoundException → trả về 401.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .roles("USER")
                .build();
    }
}
