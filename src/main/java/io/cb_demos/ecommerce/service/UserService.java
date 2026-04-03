package io.cb_demos.ecommerce.service;

import io.cb_demos.ecommerce.domain.User;
import io.cb_demos.ecommerce.dto.UserRegistrationDto;

import java.util.Optional;

public interface UserService {

    User registerUser(UserRegistrationDto registrationDto);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    User getCurrentUser();

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    User updateUser(User user);

    void disableUser(Long userId);

    void changePassword(Long userId, String oldPassword, String newPassword);
}
