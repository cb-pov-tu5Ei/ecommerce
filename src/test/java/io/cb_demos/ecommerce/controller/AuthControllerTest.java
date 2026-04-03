package io.cb_demos.ecommerce.controller;

import io.cb_demos.ecommerce.domain.User;
import io.cb_demos.ecommerce.dto.UserRegistrationDto;
import io.cb_demos.ecommerce.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private UserRegistrationDto registrationDto;
    private User testUser;

    @BeforeEach
    void setUp() {
        registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("newuser");
        registrationDto.setEmail("newuser@example.com");
        registrationDto.setPassword("SecurePass123!");
        registrationDto.setFirstName("New");
        registrationDto.setLastName("User");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("newuser");
        testUser.setEmail("newuser@example.com");
    }

    @Test
    void showLoginForm_shouldReturnLoginPage() throws Exception {
        // When & Then
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void showRegistrationForm_shouldReturnRegistrationPage() throws Exception {
        // When & Then
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    void registerUser_shouldCreateUserAndRedirect_whenValidData() throws Exception {
        // Given
        when(userService.registerUser(any(UserRegistrationDto.class))).thenReturn(testUser);

        // When & Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("email", "newuser@example.com")
                        .param("password", "SecurePass123!")
                        .param("firstName", "New")
                        .param("lastName", "User"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        verify(userService).registerUser(any(UserRegistrationDto.class));
    }

    @Test
    void registerUser_shouldRejectEmptyUsername() throws Exception {
        // When & Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "")
                        .param("email", "newuser@example.com")
                        .param("password", "SecurePass123!")
                        .param("firstName", "New")
                        .param("lastName", "User"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("user", "username"));

        verify(userService, never()).registerUser(any());
    }

    @Test
    void registerUser_shouldRejectInvalidEmail() throws Exception {
        // When & Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("email", "invalid-email")
                        .param("password", "SecurePass123!")
                        .param("firstName", "New")
                        .param("lastName", "User"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("user", "email"));

        verify(userService, never()).registerUser(any());
    }

    @Test
    void registerUser_shouldRejectShortPassword() throws Exception {
        // When & Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("email", "newuser@example.com")
                        .param("password", "short")
                        .param("firstName", "New")
                        .param("lastName", "User"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("user", "password"));

        verify(userService, never()).registerUser(any());
    }

    @Test
    void registerUser_shouldHandleDuplicateUsername() throws Exception {
        // Given
        when(userService.registerUser(any(UserRegistrationDto.class)))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        // When & Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "existinguser")
                        .param("email", "newuser@example.com")
                        .param("password", "SecurePass123!")
                        .param("firstName", "New")
                        .param("lastName", "User"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void registerUser_shouldHandleDuplicateEmail() throws Exception {
        // Given
        when(userService.registerUser(any(UserRegistrationDto.class)))
                .thenThrow(new IllegalArgumentException("Email already exists"));

        // When & Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("email", "existing@example.com")
                        .param("password", "SecurePass123!")
                        .param("firstName", "New")
                        .param("lastName", "User"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void registerUser_shouldRejectMissingFirstName() throws Exception {
        // When & Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("email", "newuser@example.com")
                        .param("password", "SecurePass123!")
                        .param("firstName", "")
                        .param("lastName", "User"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("user", "firstName"));

        verify(userService, never()).registerUser(any());
    }

    @Test
    void registerUser_shouldRejectMissingLastName() throws Exception {
        // When & Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("email", "newuser@example.com")
                        .param("password", "SecurePass123!")
                        .param("firstName", "New")
                        .param("lastName", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("user", "lastName"));

        verify(userService, never()).registerUser(any());
    }

    @Test
    void registerUser_shouldTrimWhitespace() throws Exception {
        // Given
        when(userService.registerUser(any(UserRegistrationDto.class))).thenReturn(testUser);

        // When & Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "  newuser  ")
                        .param("email", "  newuser@example.com  ")
                        .param("password", "SecurePass123!")
                        .param("firstName", "  New  ")
                        .param("lastName", "  User  "))
                .andExpect(status().is3xxRedirection());

        verify(userService).registerUser(any(UserRegistrationDto.class));
    }

    @Test
    void registerUser_shouldRejectUsernameTooLong() throws Exception {
        // Given
        String longUsername = "a".repeat(100);

        // When & Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", longUsername)
                        .param("email", "newuser@example.com")
                        .param("password", "SecurePass123!")
                        .param("firstName", "New")
                        .param("lastName", "User"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("user", "username"));

        verify(userService, never()).registerUser(any());
    }

    @Test
    void registerUser_shouldAcceptValidInternationalNames() throws Exception {
        // Given
        when(userService.registerUser(any(UserRegistrationDto.class))).thenReturn(testUser);

        // When & Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("email", "newuser@example.com")
                        .param("password", "SecurePass123!")
                        .param("firstName", "José")
                        .param("lastName", "García"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        verify(userService).registerUser(any(UserRegistrationDto.class));
    }
}
