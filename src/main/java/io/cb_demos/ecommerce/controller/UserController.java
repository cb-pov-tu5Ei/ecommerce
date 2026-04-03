package io.cb_demos.ecommerce.controller;

import io.cb_demos.ecommerce.domain.User;
import io.cb_demos.ecommerce.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private String getCurrentUsername(Principal principal) {
        if (principal != null) {
            return principal.getName();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        throw new IllegalStateException("No authenticated user found");
    }

    @GetMapping("/profile")
    public String showProfile(Principal principal, Model model) {
        String username = getCurrentUsername(principal);
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        model.addAttribute("user", user);
        return "user/profile";
    }

    @GetMapping("/edit")
    public String editProfile(Principal principal, Model model) {
        String username = getCurrentUsername(principal);
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        model.addAttribute("user", user);
        return "user/edit";
    }

    @PostMapping("/update")
    public String updateProfile(@Valid @ModelAttribute("user") User user,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "user/edit";
        }

        // Validate manually since @Valid doesn't catch empty strings that were trimmed
        if (user.getFirstName() == null || user.getFirstName().trim().isEmpty()) {
            result.rejectValue("firstName", "error.user", "First name is required");
            return "user/edit";
        }
        if (user.getLastName() == null || user.getLastName().trim().isEmpty()) {
            result.rejectValue("lastName", "error.user", "Last name is required");
            return "user/edit";
        }
        if (user.getEmail() == null || !user.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            result.rejectValue("email", "error.user", "Valid email is required");
            return "user/edit";
        }

        try {
            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
            return "redirect:/user/profile?updated";
        } catch (IllegalArgumentException e) {
            result.reject("error", e.getMessage());
            return "user/edit";
        }
    }

    @GetMapping("/change-password")
    public String showChangePasswordForm() {
        return "user/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam(required = false) String currentPassword,
                                @RequestParam(required = false) String newPassword,
                                @RequestParam(required = false) String confirmPassword,
                                Principal principal,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        // Get current user
        String username = getCurrentUsername(principal);
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "New passwords do not match");
            return "user/change-password";
        }

        if (newPassword.length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters");
            return "user/change-password";
        }

        if (currentPassword.equals(newPassword)) {
            model.addAttribute("error", "New password must be different from old password");
            return "user/change-password";
        }

        try {
            userService.changePassword(user.getId(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("success", "Password changed successfully!");
            return "redirect:/user/profile?passwordChanged";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "user/change-password";
        }
    }
}
