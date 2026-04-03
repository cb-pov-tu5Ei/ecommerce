package io.cb_demos.ecommerce.selenium.tests;

import io.cb_demos.ecommerce.selenium.base.BaseSeleniumTest;
import io.cb_demos.ecommerce.selenium.pages.HomePage;
import io.cb_demos.ecommerce.selenium.pages.LoginPage;
import io.cb_demos.ecommerce.selenium.pages.RegisterPage;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthFlowSeleniumTest extends BaseSeleniumTest {

    @Test
    void loginWithValidCredentials() {
        LoginPage loginPage = new LoginPage(driver, baseUrl);
        loginPage.navigateTo();
        loginPage.login("testuser", "test123");

        assertThat(driver.getCurrentUrl()).doesNotContain("/login");
        HomePage homePage = new HomePage(driver, baseUrl);
        assertThat(homePage.isLoggedIn()).isTrue();
    }

    @Test
    void loginWithInvalidCredentials() {
        LoginPage loginPage = new LoginPage(driver, baseUrl);
        loginPage.navigateTo();
        loginPage.login("testuser", "wrongpassword");

        assertThat(loginPage.isOnLoginPage()).isTrue();
        assertThat(loginPage.getErrorMessage()).isNotBlank();
    }

    @Test
    void logout() {
        loginAs("testuser", "test123");

        // Click logout button — logoutSuccessUrl is "/" (home page, same URL)
        driver.findElement(org.openqa.selenium.By.cssSelector("button.btn-link.nav-link")).click();
        // Wait for the logout button to disappear (page reloads with unauthenticated view)
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.invisibilityOfElementLocated(
                org.openqa.selenium.By.cssSelector("button.btn-link.nav-link")));

        // After logout, still on home page (not /login)
        assertThat(driver.getCurrentUrl()).doesNotContain("/login");
        HomePage homePage = new HomePage(driver, baseUrl);
        assertThat(homePage.isLoggedIn()).isFalse();
    }

    @Test
    void registerNewUser() {
        String uniqueUsername = "selenium_" + UUID.randomUUID().toString().substring(0, 8);
        RegisterPage registerPage = new RegisterPage(driver, baseUrl);
        registerPage.navigateTo();
        registerPage.register(uniqueUsername, uniqueUsername + "@test.com",
            "Selenium", "Test", "password123", "password123");

        assertThat(registerPage.isSuccessRedirect()).isTrue();
    }

    @Test
    void registerWithDuplicateUsername() {
        RegisterPage registerPage = new RegisterPage(driver, baseUrl);
        registerPage.navigateTo();
        registerPage.register("testuser", "newemail@test.com",
            "First", "Last", "password123", "password123");

        assertThat(registerPage.isOnRegisterPage()).isTrue();
        assertThat(registerPage.hasGlobalErrors()).isTrue();
    }

    @Test
    void registerWithInvalidEmail() {
        RegisterPage registerPage = new RegisterPage(driver, baseUrl);
        registerPage.navigateTo();
        // Bypass Chrome's HTML5 email validation so the invalid value reaches the server
        ((JavascriptExecutor) driver).executeScript(
            "document.getElementById('email').setAttribute('type', 'text')");
        registerPage.register("newuser_x1", "not-an-email",
            "First", "Last", "password123", "password123");

        assertThat(registerPage.isOnRegisterPage()).isTrue();
        assertThat(registerPage.hasFieldError("email")).isTrue();
    }

    @Test
    void registerWithShortPassword() {
        RegisterPage registerPage = new RegisterPage(driver, baseUrl);
        registerPage.navigateTo();
        registerPage.register("newuser_x2", "valid@test.com",
            "First", "Last", "short", "short");

        assertThat(registerPage.isOnRegisterPage()).isTrue();
        assertThat(registerPage.hasFieldError("password")).isTrue();
    }

    @Test
    void unauthenticatedUserRedirectedToLogin() {
        driver.get(baseUrl + "/orders/checkout");

        assertThat(driver.getCurrentUrl()).contains("/login");
    }
}
