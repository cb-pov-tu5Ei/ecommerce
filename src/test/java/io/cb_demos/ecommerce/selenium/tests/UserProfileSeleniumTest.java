package io.cb_demos.ecommerce.selenium.tests;

import io.cb_demos.ecommerce.selenium.base.BaseSeleniumTest;
import io.cb_demos.ecommerce.selenium.pages.LoginPage;
import io.cb_demos.ecommerce.selenium.pages.UserProfilePage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileSeleniumTest extends BaseSeleniumTest {

    @Test
    void profilePageShowsUserInfo() {
        loginAs("testuser", "test123");

        UserProfilePage profilePage = new UserProfilePage(driver, baseUrl);
        profilePage.navigateTo();

        assertThat(profilePage.isOnProfilePage()).isTrue();
        assertThat(profilePage.getUsername()).isEqualTo("testuser");
        assertThat(profilePage.getEmail()).isNotBlank();
        assertThat(profilePage.getDisplayName()).isNotBlank();
    }

    @Test
    void profilePageRequiresAuthentication() {
        UserProfilePage profilePage = new UserProfilePage(driver, baseUrl);
        profilePage.navigateTo();

        assertThat(profilePage.isRedirectedToLogin()).isTrue();
    }

    @Test
    void editProfileNavigatesToEditPage() {
        loginAs("testuser", "test123");

        UserProfilePage profilePage = new UserProfilePage(driver, baseUrl);
        profilePage.navigateTo();
        profilePage.clickEditProfile();

        assertThat(driver.getCurrentUrl()).contains("/user/edit");
    }

    @Test
    void editProfileSavesChanges() {
        loginAs("testuser", "test123");

        UserProfilePage profilePage = new UserProfilePage(driver, baseUrl);
        profilePage.updateProfile("testuser_updated@example.com", "Test", "User");

        // Should redirect back to profile or show success
        assertThat(driver.getCurrentUrl()).doesNotContain("/user/edit");
    }

    @Test
    void changePasswordNavigatesToChangePage() {
        loginAs("testuser", "test123");

        UserProfilePage profilePage = new UserProfilePage(driver, baseUrl);
        profilePage.navigateTo();
        profilePage.clickChangePassword();

        assertThat(driver.getCurrentUrl()).contains("/user/change-password");
    }

    @Test
    void changePasswordWithWrongCurrentPasswordShowsError() {
        loginAs("testuser", "test123");

        // Get user ID — go to profile page to read it, but the form needs userId
        // We'll use a known bad value to trigger the error
        UserProfilePage profilePage = new UserProfilePage(driver, baseUrl);
        profilePage.changePassword("1", "wrongpassword", "newpassword123", "newpassword123");

        assertThat(profilePage.getChangePasswordError()).isNotBlank();
    }

    @Test
    void changePasswordWithMismatchedPasswordsShowsError() {
        loginAs("testuser", "test123");

        UserProfilePage profilePage = new UserProfilePage(driver, baseUrl);
        profilePage.changePassword("1", "test123", "newpassword123", "differentpassword");

        assertThat(profilePage.getChangePasswordError()).isNotBlank();
    }
}
