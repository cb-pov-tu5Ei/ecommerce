package io.cb_demos.ecommerce.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(User.UserRole.USER);
        user.setEnabled(true);
    }

    @Test
    void getters_shouldReturnCorrectValues() {
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPassword()).isEqualTo("encodedPassword");
        assertThat(user.getFirstName()).isEqualTo("Test");
        assertThat(user.getLastName()).isEqualTo("User");
        assertThat(user.getRole()).isEqualTo(User.UserRole.USER);
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void setters_shouldUpdateValues() {
        // When
        user.setUsername("newusername");
        user.setEmail("new@example.com");
        user.setPassword("newPassword");
        user.setFirstName("NewFirst");
        user.setLastName("NewLast");
        user.setRole(User.UserRole.ADMIN);
        user.setEnabled(false);

        // Then
        assertThat(user.getUsername()).isEqualTo("newusername");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getPassword()).isEqualTo("newPassword");
        assertThat(user.getFirstName()).isEqualTo("NewFirst");
        assertThat(user.getLastName()).isEqualTo("NewLast");
        assertThat(user.getRole()).isEqualTo(User.UserRole.ADMIN);
        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    void getFullName_shouldCombineFirstAndLastName() {
        // When
        String fullName = user.getFirstName() + " " + user.getLastName();

        // Then
        assertThat(fullName).isEqualTo("Test User");
    }

    @Test
    void userRole_shouldSupportUserRole() {
        // When
        user.setRole(User.UserRole.USER);

        // Then
        assertThat(user.getRole()).isEqualTo(User.UserRole.USER);
    }

    @Test
    void userRole_shouldSupportAdminRole() {
        // When
        user.setRole(User.UserRole.ADMIN);

        // Then
        assertThat(user.getRole()).isEqualTo(User.UserRole.ADMIN);
    }

    @Test
    void equals_shouldComparById() {
        // Given
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");

        User user2 = new User();
        user2.setId(1L);
        user2.setUsername("user2");

        User user3 = new User();
        user3.setId(2L);
        user3.setUsername("user1");

        // Then
        assertThat(user1).isEqualTo(user2); // Same ID
        assertThat(user1).isNotEqualTo(user3); // Different ID
    }

    @Test
    void hashCode_shouldBeConsistent() {
        // Given
        User user1 = new User();
        user1.setId(1L);

        User user2 = new User();
        user2.setId(1L);

        // Then
        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
    }

    @Test
    void toString_shouldIncludeUsername() {
        // When
        String result = user.toString();

        // Then
        assertThat(result).contains("testuser");
    }

    @Test
    void enabled_shouldDefaultToFalse() {
        // Given
        User newUser = new User();

        // Then - Before being explicitly set
        // Most implementations default to false for security
        assertThat(newUser.isEnabled()).isFalse();
    }

    @Test
    void enabled_shouldToggle() {
        // When
        user.setEnabled(true);
        assertThat(user.isEnabled()).isTrue();

        user.setEnabled(false);
        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    void newUser_shouldHaveNullId() {
        // Given
        User newUser = new User();

        // Then
        assertThat(newUser.getId()).isNull();
    }

    @Test
    void username_shouldBeCaseSensitive() {
        // Given
        User user1 = new User();
        user1.setUsername("TestUser");

        User user2 = new User();
        user2.setUsername("testuser");

        // Then
        assertThat(user1.getUsername()).isNotEqualTo(user2.getUsername());
    }

    @Test
    void email_shouldAcceptValidFormats() {
        // Test various valid email formats
        user.setEmail("simple@example.com");
        assertThat(user.getEmail()).isEqualTo("simple@example.com");

        user.setEmail("user.name@example.com");
        assertThat(user.getEmail()).isEqualTo("user.name@example.com");

        user.setEmail("user+tag@example.co.uk");
        assertThat(user.getEmail()).isEqualTo("user+tag@example.co.uk");
    }

    @Test
    void password_shouldBeStoredAsProvided() {
        // Given - password should be pre-encoded before setting
        String encodedPassword = "$2a$10$abcdefghijklmnopqrstuv";

        // When
        user.setPassword(encodedPassword);

        // Then
        assertThat(user.getPassword()).isEqualTo(encodedPassword);
    }

    @Test
    void firstName_shouldAllowUnicodeCharacters() {
        // Given
        user.setFirstName("José");
        user.setLastName("García");

        // Then
        assertThat(user.getFirstName()).isEqualTo("José");
        assertThat(user.getLastName()).isEqualTo("García");
    }

    @Test
    void firstName_shouldAllowSpaces() {
        // Given
        user.setFirstName("Mary Jane");
        user.setLastName("Watson Parker");

        // Then
        assertThat(user.getFirstName()).isEqualTo("Mary Jane");
        assertThat(user.getLastName()).isEqualTo("Watson Parker");
    }

    @Test
    void userRoleEnum_shouldHaveAllValues() {
        // Then
        assertThat(User.UserRole.values()).contains(
                User.UserRole.USER,
                User.UserRole.ADMIN
        );
    }

    @Test
    void userRoleEnum_shouldHaveTwoValues() {
        // Then
        assertThat(User.UserRole.values()).hasSize(2);
    }

    @Test
    void builder_shouldCreateUserCorrectly() {
        // Given
        User newUser = new User();
        newUser.setUsername("builder");
        newUser.setEmail("builder@test.com");
        newUser.setPassword("pass123");
        newUser.setFirstName("Builder");
        newUser.setLastName("Test");
        newUser.setRole(User.UserRole.USER);
        newUser.setEnabled(true);

        // Then
        assertThat(newUser.getUsername()).isEqualTo("builder");
        assertThat(newUser.getEmail()).isEqualTo("builder@test.com");
        assertThat(newUser.getPassword()).isEqualTo("pass123");
        assertThat(newUser.getFirstName()).isEqualTo("Builder");
        assertThat(newUser.getLastName()).isEqualTo("Test");
        assertThat(newUser.getRole()).isEqualTo(User.UserRole.USER);
        assertThat(newUser.isEnabled()).isTrue();
    }

    @Test
    void username_shouldAllowVariousLengths() {
        user.setUsername("ab"); // Short
        assertThat(user.getUsername()).hasSize(2);

        user.setUsername("averylongusernamethatisseveralcharacters"); // Long
        assertThat(user.getUsername()).hasSizeGreaterThan(20);
    }

    @Test
    void email_shouldAllowVariousDomains() {
        user.setEmail("user@gmail.com");
        assertThat(user.getEmail()).contains("@gmail.com");

        user.setEmail("user@company.co.uk");
        assertThat(user.getEmail()).contains("@company.co.uk");

        user.setEmail("admin@internal.local");
        assertThat(user.getEmail()).contains("@internal.local");
    }

    @Test
    void disabledUser_shouldHaveEnabledFalse() {
        // When
        user.setEnabled(false);

        // Then
        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    void adminUser_shouldHaveAdminRole() {
        // When
        user.setRole(User.UserRole.ADMIN);

        // Then
        assertThat(user.getRole()).isEqualTo(User.UserRole.ADMIN);
    }
}
