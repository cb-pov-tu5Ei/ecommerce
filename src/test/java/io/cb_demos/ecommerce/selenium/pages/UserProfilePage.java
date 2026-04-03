package io.cb_demos.ecommerce.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class UserProfilePage {

    private final WebDriver driver;
    private final String baseUrl;

    public UserProfilePage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
    }

    public void navigateTo() {
        driver.get(baseUrl + "/user/profile");
    }

    public String getDisplayName() {
        List<WebElement> paragraphs = driver.findElements(By.cssSelector(".container p"));
        for (WebElement p : paragraphs) {
            if (p.getText().startsWith("Name:")) {
                return p.findElement(By.tagName("span")).getText();
            }
        }
        return "";
    }

    public String getEmail() {
        List<WebElement> paragraphs = driver.findElements(By.cssSelector(".container p"));
        for (WebElement p : paragraphs) {
            if (p.getText().startsWith("Email:")) {
                return p.findElement(By.tagName("span")).getText();
            }
        }
        return "";
    }

    public String getUsername() {
        List<WebElement> paragraphs = driver.findElements(By.cssSelector(".container p"));
        for (WebElement p : paragraphs) {
            if (p.getText().startsWith("Username:")) {
                return p.findElement(By.tagName("span")).getText();
            }
        }
        return "";
    }

    public void clickEditProfile() {
        driver.findElement(By.cssSelector("a.btn-primary[href*='/user/edit']")).click();
    }

    public void clickChangePassword() {
        driver.findElement(By.cssSelector("a.btn-secondary[href*='/user/change-password']")).click();
    }

    public boolean isOnProfilePage() {
        return driver.getCurrentUrl().contains("/user/profile");
    }

    public boolean isRedirectedToLogin() {
        return driver.getCurrentUrl().contains("/login");
    }

    // Edit Profile page actions
    public void updateProfile(String email, String firstName, String lastName) {
        driver.get(baseUrl + "/user/edit");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement form = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("form")));
        WebElement emailField = form.findElement(By.id("email"));
        emailField.clear();
        emailField.sendKeys(email);
        WebElement firstNameField = form.findElement(By.id("firstName"));
        firstNameField.clear();
        firstNameField.sendKeys(firstName);
        WebElement lastNameField = form.findElement(By.id("lastName"));
        lastNameField.clear();
        lastNameField.sendKeys(lastName);
        String urlBefore = driver.getCurrentUrl();
        // Submit form via JavaScript to ensure it works in headless mode
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].submit()", form);
        wait.until(ExpectedConditions.not(ExpectedConditions.urlToBe(urlBefore)));
    }

    // Change Password page actions
    public void changePassword(String userId, String oldPassword, String newPassword,
                               String confirmPassword) {
        driver.get(baseUrl + "/user/change-password");
        driver.findElement(By.id("userId")).sendKeys(userId);
        driver.findElement(By.id("oldPassword")).sendKeys(oldPassword);
        driver.findElement(By.id("newPassword")).sendKeys(newPassword);
        driver.findElement(By.id("confirmPassword")).sendKeys(confirmPassword);
        driver.findElement(By.cssSelector("button[type=submit]")).click();
    }

    public String getChangePasswordError() {
        List<WebElement> errors = driver.findElements(By.cssSelector(".alert-danger"));
        return errors.isEmpty() ? "" : errors.get(0).getText();
    }
}
