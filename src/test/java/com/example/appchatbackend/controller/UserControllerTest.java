package com.example.appchatbackend.controller;

import com.example.appchatbackend.feature.user.User;
import com.example.appchatbackend.feature.user.UserRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test cho POST /users.
 *
 * Database: Flapdoodle embedded MongoDB (auto-configured khi trên test
 * classpath).
 *
 * Lưu ý về @Transactional:
 * MongoDB standalone mode (mặc định của Flapdoodle) KHÔNG hỗ trợ transactions —
 * cần replica set. Do đó, @Transactional sẽ không tự rollback dữ liệu sau mỗi
 * test.
 * Thay vào đó, @BeforeEach xóa sạch collection trước mỗi test case để đảm bảo
 * tính độc lập giữa các test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser // POST /users yêu cầu xác thực; MockUser đóng vai authenticated user
@DisplayName("UserController - Integration Test cho POST /users")
class UserControllerTest {

    private static final String URL = "/users";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // Reset data trước mỗi test (thay thế @Transactional vì MongoDB standalone
    // không hỗ trợ transaction rollback)
    // -------------------------------------------------------------------------
    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * Tạo một User hợp lệ với username và email tùy chỉnh.
     * password mặc định đủ dài (≥ 6 ký tự) để pass validation.
     */
    private User buildValidUser(String username, String email) {
        return User.builder()
                .username(username)
                .email(email)
                .passwordHash("password123")
                .displayName("Test User")
                .build();
    }

    // =========================================================================
    @Nested
    @DisplayName("Happy Path - Tạo user thành công")
    class HappyPath {

        @Test
        @DisplayName("Trả về 201 Created khi dữ liệu hợp lệ")
        void createUser_withValidData_returns201() throws Exception {
            // Arrange
            User user = buildValidUser("testuser", "test@example.com");

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(user)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Response body chứa đủ các fields: id, username, email")
        void createUser_withValidData_responseBodyHasRequiredFields() throws Exception {
            // Arrange
            User user = buildValidUser("testuser", "test@example.com");

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(user)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.username").value("testuser"))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"));
        }

        @Test
        @DisplayName("Password không được trả về trong response (bảo mật)")
        void createUser_withValidData_passwordNotInResponse() throws Exception {
            // Arrange
            User user = buildValidUser("testuser", "test@example.com");

            // Act & Assert
            // Nếu test này FAIL → User model cần thêm @JsonIgnore trên passwordHash
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(user)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        }

        @Test
        @DisplayName("Data được lưu vào database sau khi tạo thành công")
        void createUser_withValidData_savedToDatabase() throws Exception {
            // Arrange
            User user = buildValidUser("testuser", "test@example.com");

            // Act
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(user)))
                    .andExpect(status().isCreated());

            // Assert
            assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
            User saved = userRepository.findByEmail("test@example.com").orElseThrow();
            assertThat(saved.getUsername()).isEqualTo("testuser");
            assertThat(saved.getEmail()).isEqualTo("test@example.com");
            assertThat(saved.getId()).isNotBlank();
        }

        @Test
        @DisplayName("Password trong database đã được encode, khác với plain text gốc")
        void createUser_withValidData_passwordEncodedInDatabase() throws Exception {
            // Arrange
            String rawPassword = "password123";
            User user = buildValidUser("testuser", "test@example.com");
            user.setPasswordHash(rawPassword);

            // Act
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(user)))
                    .andExpect(status().isCreated());

            // Assert
            User saved = userRepository.findByEmail("test@example.com").orElseThrow();
            assertThat(saved.getPasswordHash())
                    .as("Password trong DB không được là plain text")
                    .isNotEqualTo(rawPassword);
            assertThat(passwordEncoder.matches(rawPassword, saved.getPasswordHash()))
                    .as("BCrypt phải verify được raw password với hash trong DB")
                    .isTrue();
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("Validation Errors - 400 Bad Request")
    class ValidationErrors {

        @Test
        @DisplayName("Trả về 400 khi username để trống")
        void createUser_withBlankUsername_returns400() throws Exception {
            // Arrange
            User user = buildValidUser("", "test@example.com");

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(user)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Trả về 400 khi email để trống")
        void createUser_withBlankEmail_returns400() throws Exception {
            // Arrange
            User user = buildValidUser("testuser", "");

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(user)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Trả về 400 khi email không đúng định dạng")
        void createUser_withInvalidEmailFormat_returns400() throws Exception {
            // Arrange
            User user = buildValidUser("testuser", "not-an-email");

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(user)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Trả về 400 khi password ít hơn 6 ký tự")
        void createUser_withShortPassword_returns400() throws Exception {
            // Arrange
            User user = buildValidUser("testuser", "test@example.com");
            user.setPasswordHash("12345"); // 5 ký tự — vi phạm @Size(min = 6)

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(user)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Không lưu data vào database khi validation fail")
        void createUser_whenValidationFails_noDataSavedToDatabase() throws Exception {
            // Arrange — email sai định dạng sẽ bị reject ở tầng @Valid
            User user = buildValidUser("testuser", "invalid-email");

            // Act
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(user)))
                    .andExpect(status().isBadRequest());

            // Assert — DB phải rỗng, không có gì được lưu
            assertThat(userRepository.findAll()).isEmpty();
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("Business Errors - 409 Conflict")
    class BusinessErrors {

        @Test
        @DisplayName("Trả về 409 Conflict khi email đã tồn tại trong database")
        void createUser_whenEmailAlreadyExists_returns409() throws Exception {
            // Arrange — lưu user đầu tiên trực tiếp qua repository
            User existing = buildValidUser("user1", "duplicate@example.com");
            existing.setPasswordHash(passwordEncoder.encode("password123"));
            userRepository.save(existing);

            // Act — thử tạo user thứ hai với cùng email
            User duplicate = buildValidUser("user2", "duplicate@example.com");

            // Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(duplicate)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Không tạo user mới khi email đã tồn tại")
        void createUser_whenEmailAlreadyExists_noNewUserCreated() throws Exception {
            // Arrange — lưu user đầu tiên trực tiếp qua repository
            User existing = buildValidUser("user1", "duplicate@example.com");
            existing.setPasswordHash(passwordEncoder.encode("password123"));
            userRepository.save(existing);

            long countBefore = userRepository.count(); // = 1

            // Act — thử tạo user thứ hai với cùng email
            User duplicate = buildValidUser("user2", "duplicate@example.com");
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(duplicate)))
                    .andExpect(status().isConflict());

            // Assert — tổng số user trong DB không đổi
            assertThat(userRepository.count())
                    .as("Số lượng user trong DB phải giữ nguyên khi email bị trùng")
                    .isEqualTo(countBefore);
        }
    }
}
