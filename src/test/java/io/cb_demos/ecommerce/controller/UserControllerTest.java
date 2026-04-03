package io.cb_demos.ecommerce.controller;
import io.cb_demos.ecommerce.config.TestSecurityConfig;
import org.springframework.context.annotation.Import;

import io.cb_demos.ecommerce.domain.User;
import io.cb_demos.ecommerce.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestSecurityConfig.class)
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEnabled(true);
        testUser.setRole(User.UserRole.USER);
    }

    @Test
    @WithMockUser(username = "testuser")
    void viewProfile_shouldDisplayUserProfile() throws Exception {
        // Given
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(get("/user/profile").with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(view().name("user/profile"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", testUser));

        verify(userService).findByUsername("testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void editProfile_shouldShowEditForm() throws Exception {
        // Given
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(get("/user/edit").with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(view().name("user/edit"))
                .andExpect(model().attributeExists("user"));

        verify(userService).findByUsername("testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateProfile_shouldUpdateUserAndRedirect() throws Exception {
        // Given
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userService.updateUser(any(User.class))).thenReturn(testUser);

        // When & Then
        mockMvc.perform(post("/user/update")
                        .with(csrf()).with(user("testuser"))
                        .param("firstName", "Updated")
                        .param("lastName", "Name")
                        .param("email", "updated@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/profile?updated"));

        verify(userService).updateUser(any(User.class));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateProfile_shouldRejectInvalidEmail() throws Exception {
        // Given
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(post("/user/update")
                        .with(csrf()).with(user("testuser"))
                        .param("firstName", "Updated")
                        .param("lastName", "Name")
                        .param("email", "invalid-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/edit"))
                .andExpect(model().attributeHasFieldErrors("user", "email"));

        verify(userService, never()).updateUser(any());
    }

    @Test
    @WithMockUser(username = "testuser")
    void changePassword_shouldShowPasswordForm() throws Exception {
        // When & Then
        mockMvc.perform(get("/user/change-password").with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(view().name("user/change-password"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void changePassword_shouldUpdatePassword_whenValid() throws Exception {
        // Given
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        doNothing().when(userService).changePassword(eq(1L), eq("OldPass123!"), eq("NewPass456!"));

        // When & Then
        mockMvc.perform(post("/user/change-password")
                        .with(csrf()).with(user("testuser"))
                        .param("currentPassword", "OldPass123!")
                        .param("newPassword", "NewPass456!")
                        .param("confirmPassword", "NewPass456!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/profile?passwordChanged"));

        verify(userService).changePassword(1L, "OldPass123!", "NewPass456!");
    }

    @Test
    @WithMockUser(username = "testuser")
    void changePassword_shouldReject_whenPasswordsDontMatch() throws Exception {
        // Given
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(post("/user/change-password")
                        .with(csrf()).with(user("testuser"))
                        .param("currentPassword", "OldPass123!")
                        .param("newPassword", "NewPass456!")
                        .param("confirmPassword", "DifferentPass789!"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/change-password"))
                .andExpect(model().attributeExists("error"));

        verify(userService, never()).changePassword(anyLong(), anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void changePassword_shouldReject_whenNewPasswordTooShort() throws Exception {
        // Given
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(post("/user/change-password")
                        .with(csrf()).with(user("testuser"))
                        .param("currentPassword", "OldPass123!")
                        .param("newPassword", "short")
                        .param("confirmPassword", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/change-password"))
                .andExpect(model().attributeExists("error"));

        verify(userService, never()).changePassword(anyLong(), anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void changePassword_shouldHandleIncorrectCurrentPassword() throws Exception {
        // Given
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        doThrow(new IllegalArgumentException("Current password is incorrect"))
                .when(userService).changePassword(eq(1L), eq("WrongPass!"), eq("NewPass456!"));

        // When & Then
        mockMvc.perform(post("/user/change-password")
                        .with(csrf()).with(user("testuser"))
                        .param("currentPassword", "WrongPass!")
                        .param("newPassword", "NewPass456!")
                        .param("confirmPassword", "NewPass456!"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/change-password"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateProfile_shouldRejectEmptyFirstName() throws Exception {
        // Given
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(post("/user/update")
                        .with(csrf()).with(user("testuser"))
                        .param("firstName", "")
                        .param("lastName", "Name")
                        .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/edit"))
                .andExpect(model().attributeHasFieldErrors("user", "firstName"));

        verify(userService, never()).updateUser(any());
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateProfile_shouldRejectEmptyLastName() throws Exception {
        // Given
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(post("/user/update")
                        .with(csrf()).with(user("testuser"))
                        .param("firstName", "Test")
                        .param("lastName", "")
                        .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/edit"))
                .andExpect(model().attributeHasFieldErrors("user", "lastName"));

        verify(userService, never()).updateUser(any());
    }

    @Test
    void viewProfile_shouldRequireAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/user/profile"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username = "testuser")
    void viewProfile_shouldHandle_whenUserNotFound() throws Exception {
        // Given
        when(userService.findByUsername("testuser")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/user/profile"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username = "testuser")
    void changePassword_shouldReject_whenSameAsOldPassword() throws Exception {
        // Given
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(post("/user/change-password")
                        .with(csrf()).with(user("testuser"))
                        .param("currentPassword", "SamePass123!")
                        .param("newPassword", "SamePass123!")
                        .param("confirmPassword", "SamePass123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/change-password"))
                .andExpect(model().attributeExists("error"));

        verify(userService, never()).changePassword(anyLong(), anyString(), anyString());
    }
}
