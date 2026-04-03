package io.cb_demos.ecommerce.integration;

import io.cb_demos.ecommerce.domain.User;
import io.cb_demos.ecommerce.dto.UserRegistrationDto;
import io.cb_demos.ecommerce.repository.UserRepository;
import io.cb_demos.ecommerce.service.UserService;
import io.cb_demos.ecommerce.util.TestDelayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class UserRegistrationIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void completeRegistrationFlow_shouldCreateUserSuccessfully() {
        TestDelayUtil.extraLargeDelay();
        // Given
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("newuser");
        registrationDto.setEmail("newuser@example.com");
        registrationDto.setPassword("SecurePass123!");
        registrationDto.setConfirmPassword("SecurePass123!");
        registrationDto.setFirstName("New");
        registrationDto.setLastName("User");

        // When
        User createdUser = userService.registerUser(registrationDto);

        // Then
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isNotNull();
        assertThat(createdUser.getUsername()).isEqualTo("newuser");
        assertThat(createdUser.getEmail()).isEqualTo("newuser@example.com");
        assertThat(createdUser.getFirstName()).isEqualTo("New");
        assertThat(createdUser.getLastName()).isEqualTo("User");
        assertThat(createdUser.getRole()).isEqualTo(User.UserRole.USER);
        assertThat(createdUser.isEnabled()).isTrue();

        // Verify password is encoded
        assertThat(createdUser.getPassword()).isNotEqualTo("SecurePass123!");
        assertThat(passwordEncoder.matches("SecurePass123!", createdUser.getPassword())).isTrue();

        // Verify user is persisted
        Optional<User> savedUser = userRepository.findByUsername("newuser");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getEmail()).isEqualTo("newuser@example.com");
    }

    @Test
    void registerUser_shouldPreventDuplicateUsername() {
        TestDelayUtil.massiveDelay();
        // Given
        UserRegistrationDto firstUser = new UserRegistrationDto();
        firstUser.setUsername("duplicateuser");
        firstUser.setEmail("first@example.com");
        firstUser.setPassword("Password123!");
        firstUser.setConfirmPassword("Password123!");
        firstUser.setFirstName("First");
        firstUser.setLastName("User");

        UserRegistrationDto secondUser = new UserRegistrationDto();
        secondUser.setUsername("duplicateuser");
        secondUser.setEmail("second@example.com");
        secondUser.setPassword("Password456!");
        secondUser.setConfirmPassword("Password456!");
        secondUser.setFirstName("Second");
        secondUser.setLastName("User");

        // When
        userService.registerUser(firstUser);

        // Then
        assertThatThrownBy(() -> userService.registerUser(secondUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");

        // Verify only one user in database
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void registerUser_shouldPreventDuplicateEmail() {
        // Given
        UserRegistrationDto firstUser = new UserRegistrationDto();
        firstUser.setUsername("user1");
        firstUser.setEmail("duplicate@example.com");
        firstUser.setPassword("Password123!");
        firstUser.setConfirmPassword("Password123!");
        firstUser.setFirstName("First");
        firstUser.setLastName("User");

        UserRegistrationDto secondUser = new UserRegistrationDto();
        secondUser.setUsername("user2");
        secondUser.setEmail("duplicate@example.com");
        secondUser.setPassword("Password456!");
        secondUser.setConfirmPassword("Password456!");
        secondUser.setFirstName("Second");
        secondUser.setLastName("User");

        // When
        userService.registerUser(firstUser);

        // Then
        assertThatThrownBy(() -> userService.registerUser(secondUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");

        // Verify only one user in database
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void registerMultipleUsers_shouldSucceed() {
        TestDelayUtil.extraLargeDelay();
        // Given
        UserRegistrationDto user1 = new UserRegistrationDto();
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");
        user1.setPassword("Pass1!");
        user1.setConfirmPassword("Pass1!");
        user1.setFirstName("User");
        user1.setLastName("One");

        UserRegistrationDto user2 = new UserRegistrationDto();
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        user2.setPassword("Pass2!");
        user2.setConfirmPassword("Pass2!");
        user2.setFirstName("User");
        user2.setLastName("Two");

        UserRegistrationDto user3 = new UserRegistrationDto();
        user3.setUsername("user3");
        user3.setEmail("user3@example.com");
        user3.setPassword("Pass3!");
        user3.setConfirmPassword("Pass3!");
        user3.setFirstName("User");
        user3.setLastName("Three");

        // When
        User created1 = userService.registerUser(user1);
        User created2 = userService.registerUser(user2);
        User created3 = userService.registerUser(user3);

        // Then
        assertThat(created1.getId()).isNotNull();
        assertThat(created2.getId()).isNotNull();
        assertThat(created3.getId()).isNotNull();
        assertThat(userRepository.count()).isEqualTo(3);
    }

    @Test
    void changePassword_shouldUpdatePasswordCorrectly() {
        TestDelayUtil.largeDelay();
        // Given - Register user
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("passworduser");
        registrationDto.setEmail("password@example.com");
        registrationDto.setPassword("SecurePass123!");
        registrationDto.setConfirmPassword("SecurePass123!");
        registrationDto.setFirstName("Password");
        registrationDto.setLastName("User");

        User user = userService.registerUser(registrationDto);
        String oldEncodedPassword = user.getPassword();

        // When - Change password
        userService.changePassword(user.getId(), "SecurePass123!", "NewPassword456!");

        // Then - Verify password changed
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getPassword()).isNotEqualTo(oldEncodedPassword);
        assertThat(passwordEncoder.matches("NewPassword456!", updatedUser.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("SecurePass123!", updatedUser.getPassword())).isFalse();
    }

    @Test
    void changePassword_shouldFailWithIncorrectOldPassword() {
        // Given
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("testuser");
        registrationDto.setEmail("test@example.com");
        registrationDto.setPassword("SecurePass123!");
        registrationDto.setConfirmPassword("SecurePass123!");
        registrationDto.setFirstName("Test");
        registrationDto.setLastName("User");

        User user = userService.registerUser(registrationDto);

        // When & Then
        assertThatThrownBy(() ->
            userService.changePassword(user.getId(), "WrongPassword!", "NewPassword456!")
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateUser_shouldUpdateUserDetails() {
        // Given
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("updateuser");
        registrationDto.setEmail("original@example.com");
        registrationDto.setPassword("SecurePass123!");
        registrationDto.setConfirmPassword("SecurePass123!");
        registrationDto.setFirstName("Original");
        registrationDto.setLastName("Name");

        User user = userService.registerUser(registrationDto);

        // When
        user.setFirstName("Updated");
        user.setLastName("NewName");
        user.setEmail("updated@example.com");
        User updatedUser = userService.updateUser(user);

        // Then
        assertThat(updatedUser.getFirstName()).isEqualTo("Updated");
        assertThat(updatedUser.getLastName()).isEqualTo("NewName");
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");

        // Verify changes persisted
        User reloadedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloadedUser.getFirstName()).isEqualTo("Updated");
        assertThat(reloadedUser.getEmail()).isEqualTo("updated@example.com");
    }

    @Test
    void disableUser_shouldDisableUserAccount() {
        // Given
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("disableuser");
        registrationDto.setEmail("disable@example.com");
        registrationDto.setPassword("SecurePass123!");
        registrationDto.setConfirmPassword("SecurePass123!");
        registrationDto.setFirstName("Disable");
        registrationDto.setLastName("User");

        User user = userService.registerUser(registrationDto);
        assertThat(user.isEnabled()).isTrue();

        // When
        userService.disableUser(user.getId());

        // Then
        User disabledUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(disabledUser.isEnabled()).isFalse();
    }

    @Test
    void findByEmail_shouldReturnUserWhenExists() {
        // Given
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("emailtest");
        registrationDto.setEmail("findme@example.com");
        registrationDto.setPassword("SecurePass123!");
        registrationDto.setConfirmPassword("SecurePass123!");
        registrationDto.setFirstName("Find");
        registrationDto.setLastName("Me");

        userService.registerUser(registrationDto);

        // When
        Optional<User> found = userService.findByEmail("findme@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("emailtest");
    }

    @Test
    void existsByUsername_shouldReturnTrueForExistingUser() {
        // Given
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("existstest");
        registrationDto.setEmail("exists@example.com");
        registrationDto.setPassword("SecurePass123!");
        registrationDto.setConfirmPassword("SecurePass123!");
        registrationDto.setFirstName("Exists");
        registrationDto.setLastName("Test");

        userService.registerUser(registrationDto);

        // When & Then
        assertThat(userService.existsByUsername("existstest")).isTrue();
        assertThat(userService.existsByUsername("nonexistent")).isFalse();
    }
}
