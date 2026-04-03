package io.cb_demos.ecommerce.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class RegisterPage {

    private final WebDriver driver;
    private final String baseUrl;

    public RegisterPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
    }

    public void navigateTo() {
        driver.get(baseUrl + "/register");
    }

    public void register(String username, String email, String firstName, String lastName,
                         String password, String confirmPassword) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.findElement(By.id("username")).clear();
        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("email")).clear();
        driver.findElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("firstName")).clear();
        driver.findElement(By.id("firstName")).sendKeys(firstName);
        driver.findElement(By.id("lastName")).clear();
        driver.findElement(By.id("lastName")).sendKeys(lastName);
        driver.findElement(By.id("password")).clear();
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.id("confirmPassword")).clear();
        driver.findElement(By.id("confirmPassword")).sendKeys(confirmPassword);
        WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(
            By.cssSelector("button[type=submit]")));
        submitButton.click();
        // Wait for page to reload (stale element works for both redirect and validation errors)
        wait.until(ExpectedConditions.stalenessOf(submitButton));
    }

    public boolean hasGlobalErrors() {
        return !driver.findElements(By.cssSelector(".alert-danger")).isEmpty();
    }

    public boolean hasFieldError(String fieldId) {
        List<org.openqa.selenium.WebElement> inputs = driver.findElements(By.id(fieldId));
        if (inputs.isEmpty()) return false;
        String classes = inputs.get(0).getAttribute("class");
        return classes != null && classes.contains("is-invalid");
    }

    public String getFieldErrorText(String fieldId) {
        // invalid-feedback appears after the input
        List<org.openqa.selenium.WebElement> feedbacks = driver.findElements(
            By.cssSelector("#" + fieldId + " ~ .invalid-feedback"));
        return feedbacks.isEmpty() ? "" : feedbacks.get(0).getText();
    }

    public boolean isOnRegisterPage() {
        return driver.getCurrentUrl().contains("/register");
    }

    public boolean isSuccessRedirect() {
        // after successful registration redirects to /login
        return driver.getCurrentUrl().contains("/login");
    }
}
