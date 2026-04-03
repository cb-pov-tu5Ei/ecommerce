package io.cb_demos.ecommerce.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class LoginPage {

    private final WebDriver driver;
    private final String baseUrl;

    public LoginPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
    }

    public void navigateTo() {
        driver.get(baseUrl + "/login");
    }

    public void login(String username, String password) {
        driver.findElement(By.name("username")).clear();
        driver.findElement(By.name("username")).sendKeys(username);
        driver.findElement(By.name("password")).clear();
        driver.findElement(By.name("password")).sendKeys(password);
        String urlBefore = driver.getCurrentUrl();
        driver.findElement(By.cssSelector("button[type=submit]")).click();
        // Wait until the URL changes (either success redirect or ?error redirect)
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.not(ExpectedConditions.urlToBe(urlBefore)));
    }

    public String getErrorMessage() {
        List<org.openqa.selenium.WebElement> errors = driver.findElements(By.cssSelector(".alert-danger"));
        return errors.isEmpty() ? "" : errors.get(0).getText();
    }

    public boolean isOnLoginPage() {
        return driver.getCurrentUrl().contains("/login");
    }

    public boolean isLogoutMessagePresent() {
        return !driver.findElements(By.cssSelector(".alert-success")).isEmpty();
    }

    public void clickRegisterLink() {
        driver.findElement(By.cssSelector("a[href*='/register']")).click();
    }
}
