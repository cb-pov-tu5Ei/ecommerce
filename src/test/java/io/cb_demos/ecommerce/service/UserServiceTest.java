package io.cb_demos.ecommerce.service;

import io.cb_demos.ecommerce.domain.User;
import io.cb_demos.ecommerce.dto.UserRegistrationDto;
import io.cb_demos.ecommerce.repository.UserRepository;
import io.cb_demos.ecommerce.service.impl.UserServiceImpl;
import io.cb_demos.ecommerce.util.TestDelayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRegistrationDto registrationDto;
    private User testUser;

    @BeforeEach
    void setUp() {
        registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("newuser");
        registrationDto.setEmail("newuser@example.com");
        registrationDto.setPassword("SecurePass123!");
        registrationDto.setConfirmPassword("SecurePass123!");
        registrationDto.setFirstName("New");
        registrationDto.setLastName("User");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("existinguser");
        testUser.setEmail("existing@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFirstName("Existing");
        testUser.setLastName("User");
        testUser.setRole(User.UserRole.USER);
        testUser.setEnabled(true);
    }

    @Test
    void registerUser_shouldCreateNewUser_whenValidData() {
        TestDelayUtil.mediumDelay();
        // Given
        when(userRepository.existsByUsername(registrationDto.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registrationDto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registrationDto.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        // When
        User result = userService.registerUser(registrationDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getEmail()).isEqualTo("newuser@example.com");
        assertThat(result.getFirstName()).isEqualTo("New");
        assertThat(result.getLastName()).isEqualTo("User");
        assertThat(result.getRole()).isEqualTo(User.UserRole.USER);
        assertThat(result.isEnabled()).isTrue();

        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("newuser@example.com");
        verify(passwordEncoder).encode("SecurePass123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_shouldThrowException_whenUsernameExists() {
        TestDelayUtil.mediumDelay();
        // Given
        when(userRepository.existsByUsername(registrationDto.getUsername())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(registrationDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository).existsByUsername("newuser");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_shouldThrowException_whenEmailExists() {
        // Given
        when(userRepository.existsByUsername(registrationDto.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registrationDto.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(registrationDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository).existsByEmail("newuser@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void findByUsername_shouldReturnUser_whenUserExists() {
        // Given
        when(userRepository.findByUsername("existinguser")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findByUsername("existinguser");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("existinguser");
        verify(userRepository).findByUsername("existinguser");
    }

    @Test
    void findByUsername_shouldReturnEmpty_whenUserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findByUsername("nonexistent");

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void findByEmail_shouldReturnUser_whenUserExists() {
        // Given
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findByEmail("existing@example.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("existing@example.com");
        verify(userRepository).findByEmail("existing@example.com");
    }

    @Test
    void existsByUsername_shouldReturnTrue_whenUserExists() {
        // Given
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // When
        boolean result = userService.existsByUsername("existinguser");

        // Then
        assertThat(result).isTrue();
        verify(userRepository).existsByUsername("existinguser");
    }

    @Test
    void existsByUsername_shouldReturnFalse_whenUserDoesNotExist() {
        // Given
        when(userRepository.existsByUsername("newuser")).thenReturn(false);

        // When
        boolean result = userService.existsByUsername("newuser");

        // Then
        assertThat(result).isFalse();
        verify(userRepository).existsByUsername("newuser");
    }

    @Test
    void existsByEmail_shouldReturnTrue_whenEmailExists() {
        // Given
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When
        boolean result = userService.existsByEmail("existing@example.com");

        // Then
        assertThat(result).isTrue();
        verify(userRepository).existsByEmail("existing@example.com");
    }

    @Test
    void updateUser_shouldUpdateUserDetails() {
        // Given
        testUser.setFirstName("Updated");
        testUser.setLastName("Name");
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        User result = userService.updateUser(testUser);

        // Then
        assertThat(result.getFirstName()).isEqualTo("Updated");
        assertThat(result.getLastName()).isEqualTo("Name");
        verify(userRepository).save(testUser);
    }

    @Test
    void disableUser_shouldSetEnabledToFalse() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.disableUser(1L);

        // Then
        assertThat(testUser.isEnabled()).isFalse();
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    void changePassword_shouldEncodeAndSaveNewPassword() {
        // Given
        String oldPassword = "OldPass123!";
        String newPassword = "NewPass456!";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(oldPassword, testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.changePassword(1L, oldPassword, newPassword);

        // Then
        verify(passwordEncoder).matches(oldPassword, "encodedPassword");
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(testUser);
    }

    @Test
    void changePassword_shouldThrowException_whenOldPasswordIncorrect() {
        // Given
        String oldPassword = "WrongPass123!";
        String newPassword = "NewPass456!";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(oldPassword, testUser.getPassword())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(1L, oldPassword, newPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password");

        verify(passwordEncoder).matches(oldPassword, testUser.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }
}
