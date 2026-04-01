package com.example.appchatbackend.service;

import com.example.appchatbackend.exception.DuplicateResourceException;
import com.example.appchatbackend.feature.user.User;
import com.example.appchatbackend.feature.user.UserRepository;
import com.example.appchatbackend.feature.user.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    // -------------------------------------------------------------------------
    // Dữ liệu dùng chung
    // -------------------------------------------------------------------------
    private static final String RAW_PASSWORD = "secret123";
    private static final String ENCODED_PASSWORD = "encoded_secret123";
    private static final String EMAIL = "test@example.com";
    private static final String USERNAME = "testuser";

    private User buildInputUser() {
        return User.builder()
                .username(USERNAME)
                .email(EMAIL)
                .passwordHash(RAW_PASSWORD)
                .build();
    }

    // =========================================================================
    @Nested
    @DisplayName("Tạo user thành công")
    class CreateUserSuccess {

        @Test
        @DisplayName("Trả về user đã lưu khi email và username chưa tồn tại")
        void createUser_whenEmailAndUsernameNotExist_returnsSavedUser() {
            // Arrange
            User inputUser = buildInputUser();
            User savedUser = User.builder()
                    .id("abc123")
                    .username(USERNAME)
                    .email(EMAIL)
                    .passwordHash(ENCODED_PASSWORD)
                    .build();

            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // Act
            User result = userService.create(inputUser);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("abc123");
            assertThat(result.getEmail()).isEqualTo(EMAIL);
            assertThat(result.getPasswordHash()).isEqualTo(ENCODED_PASSWORD);
        }

        @Test
        @DisplayName("Password phải được encode trước khi lưu vào database")
        void createUser_passwordIsEncodedBeforeSave() {
            // Arrange
            User inputUser = buildInputUser();

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            // trả lại chính đối tượng được truyền vào để kiểm tra trạng thái
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            userService.create(inputUser);

            // Assert – dùng argThat để xác nhận user được save đã có password encoded,
            // không còn là raw password ban đầu
            verify(userRepository).save(argThat(user -> !RAW_PASSWORD.equals(user.getPasswordHash())
                    && ENCODED_PASSWORD.equals(user.getPasswordHash())));
        }

        @Test
        @DisplayName("existsByEmail() phải được gọi trước save()")
        void createUser_existsByEmailCalledBeforeSave() {
            // Arrange
            User inputUser = buildInputUser();

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            userService.create(inputUser);

            // Assert – InOrder đảm bảo thứ tự: existsByEmail → save
            InOrder inOrder = inOrder(userRepository);
            inOrder.verify(userRepository).existsByEmail(EMAIL);
            inOrder.verify(userRepository).save(any(User.class));
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("Tạo user thất bại - Email đã tồn tại")
    class CreateUserDuplicateEmail {

        @Test
        @DisplayName("Ném DuplicateResourceException khi email đã tồn tại")
        void createUser_whenEmailAlreadyExists_throwsDuplicateResourceException() {
            // Arrange
            User inputUser = buildInputUser();
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.create(inputUser))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining(EMAIL);
        }

        @Test
        @DisplayName("save() không được gọi khi email đã tồn tại")
        void createUser_whenEmailAlreadyExists_saveNeverCalled() {
            // Arrange
            User inputUser = buildInputUser();
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            // Act
            assertThatThrownBy(() -> userService.create(inputUser))
                    .isInstanceOf(DuplicateResourceException.class);

            // Assert
            verify(userRepository, never()).save(any(User.class));
        }
    }
}
