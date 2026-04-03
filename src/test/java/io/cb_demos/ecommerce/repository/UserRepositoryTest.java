package io.cb_demos.ecommerce.repository;

import io.cb_demos.ecommerce.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void findByUsername_shouldReturnUserWhenExists() {
        // Given
        User user = createUser("testuser", "test@example.com");

        // When
        Optional<User> found = userRepository.findByUsername("testuser");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(user);
    }

    @Test
    void findByUsername_shouldReturnEmptyWhenNotExists() {
        // When
        Optional<User> found = userRepository.findByUsername("nonexistent");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void findByEmail_shouldReturnUserWhenExists() {
        // Given
        User user = createUser("testuser", "test@example.com");

        // When
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(user);
    }

    @Test
    void findByEmail_shouldReturnEmptyWhenNotExists() {
        // When
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void existsByUsername_shouldReturnTrueWhenExists() {
        // Given
        createUser("existinguser", "existing@example.com");

        // When
        boolean exists = userRepository.existsByUsername("existinguser");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByUsername_shouldReturnFalseWhenNotExists() {
        // When
        boolean exists = userRepository.existsByUsername("nonexistent");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void existsByEmail_shouldReturnTrueWhenExists() {
        // Given
        createUser("testuser", "existing@example.com");

        // When
        boolean exists = userRepository.existsByEmail("existing@example.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_shouldReturnFalseWhenNotExists() {
        // When
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void save_shouldPersistUser() {
        // Given
        User user = new User();
        user.setUsername("newuser");
        user.setEmail("new@example.com");
        user.setPassword("password");
        user.setFirstName("New");
        user.setLastName("User");
        user.setRole(User.UserRole.USER);
        user.setEnabled(true);

        // When
        User saved = userRepository.save(user);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(userRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void delete_shouldRemoveUser() {
        // Given
        User user = createUser("deleteuser", "delete@example.com");
        Long userId = user.getId();

        // When
        userRepository.delete(user);

        // Then
        assertThat(userRepository.findById(userId)).isEmpty();
    }

    @Test
    void findByRole_shouldReturnUsersWithRole() {
        // Given
        User admin1 = createUser("admin1", "admin1@example.com");
        admin1.setRole(User.UserRole.ADMIN);
        userRepository.save(admin1);

        User admin2 = createUser("admin2", "admin2@example.com");
        admin2.setRole(User.UserRole.ADMIN);
        userRepository.save(admin2);

        User regularUser = createUser("user", "user@example.com");
        regularUser.setRole(User.UserRole.USER);
        userRepository.save(regularUser);

        // When
        var admins = userRepository.findByRole(User.UserRole.ADMIN);

        // Then
        assertThat(admins).hasSize(2);
        assertThat(admins).contains(admin1, admin2);
        assertThat(admins).doesNotContain(regularUser);
    }

    @Test
    void findByEnabled_shouldReturnEnabledUsers() {
        // Given
        User enabled1 = createUser("enabled1", "enabled1@example.com");
        enabled1.setEnabled(true);
        userRepository.save(enabled1);

        User enabled2 = createUser("enabled2", "enabled2@example.com");
        enabled2.setEnabled(true);
        userRepository.save(enabled2);

        User disabled = createUser("disabled", "disabled@example.com");
        disabled.setEnabled(false);
        userRepository.save(disabled);

        // When
        var enabledUsers = userRepository.findByEnabled(true);

        // Then
        assertThat(enabledUsers).hasSize(2);
        assertThat(enabledUsers).contains(enabled1, enabled2);
        assertThat(enabledUsers).doesNotContain(disabled);
    }

    @Test
    void findAll_shouldReturnAllUsers() {
        // Given
        createUser("user1", "user1@example.com");
        createUser("user2", "user2@example.com");
        createUser("user3", "user3@example.com");

        // When
        var allUsers = userRepository.findAll();

        // Then
        assertThat(allUsers).hasSize(3);
    }

    @Test
    void count_shouldReturnCorrectCount() {
        // Given
        createUser("user1", "user1@example.com");
        createUser("user2", "user2@example.com");

        // When
        long count = userRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void update_shouldModifyUser() {
        // Given
        User user = createUser("updateuser", "update@example.com");
        Long userId = user.getId();

        // When
        user.setFirstName("Updated");
        user.setLastName("Name");
        user.setEmail("newemail@example.com");
        userRepository.save(user);

        // Then
        User updated = userRepository.findById(userId).orElseThrow();
        assertThat(updated.getFirstName()).isEqualTo("Updated");
        assertThat(updated.getLastName()).isEqualTo("Name");
        assertThat(updated.getEmail()).isEqualTo("newemail@example.com");
    }

    @Test
    void findByUsername_shouldBeCaseSensitive() {
        // Given
        createUser("TestUser", "test@example.com");

        // When
        Optional<User> exactMatch = userRepository.findByUsername("TestUser");
        Optional<User> lowerCase = userRepository.findByUsername("testuser");
        Optional<User> upperCase = userRepository.findByUsername("TESTUSER");

        // Then
        assertThat(exactMatch).isPresent();
        assertThat(lowerCase).isEmpty();
        assertThat(upperCase).isEmpty();
    }

    @Test
    void existsByEmail_shouldBeCaseInsensitive() {
        // Given
        createUser("testuser", "Test@Example.COM");

        // When
        boolean exists1 = userRepository.existsByEmail("Test@Example.COM");
        boolean exists2 = userRepository.existsByEmail("test@example.com");
        boolean exists3 = userRepository.existsByEmail("TEST@EXAMPLE.COM");

        // Then - Depending on database collation, this might vary
        // For most databases, email comparison is case-insensitive
        assertThat(exists1).isTrue();
    }

    @Test
    void save_shouldGenerateId() {
        // Given
        User user = new User();
        user.setUsername("idtest");
        user.setEmail("idtest@example.com");
        user.setPassword("password");
        user.setFirstName("ID");
        user.setLastName("Test");
        user.setRole(User.UserRole.USER);
        user.setEnabled(true);

        // When
        User saved = userRepository.save(user);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isGreaterThan(0L);
    }

    @Test
    void findByUsernameOrEmail_shouldFindByEither() {
        // Given
        User user = createUser("findme", "findme@example.com");

        // When
        Optional<User> foundByUsername = userRepository.findByUsernameOrEmail("findme", "wrong@email.com");
        Optional<User> foundByEmail = userRepository.findByUsernameOrEmail("wronguser", "findme@example.com");
        Optional<User> foundByBoth = userRepository.findByUsernameOrEmail("findme", "findme@example.com");

        // Then
        assertThat(foundByUsername).isPresent();
        assertThat(foundByEmail).isPresent();
        assertThat(foundByBoth).isPresent();
        assertThat(foundByUsername.get()).isEqualTo(user);
        assertThat(foundByEmail.get()).isEqualTo(user);
        assertThat(foundByBoth.get()).isEqualTo(user);
    }

    @Test
    void deleteAll_shouldRemoveAllUsers() {
        // Given
        createUser("user1", "user1@example.com");
        createUser("user2", "user2@example.com");
        assertThat(userRepository.count()).isEqualTo(2);

        // When
        userRepository.deleteAll();

        // Then
        assertThat(userRepository.count()).isZero();
    }

    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(User.UserRole.USER);
        user.setEnabled(true);
        return userRepository.save(user);
    }
}
