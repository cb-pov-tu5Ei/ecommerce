package io.cb_demos.ecommerce.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class CheckoutPage {

    private final WebDriver driver;
    private final String baseUrl;

    public CheckoutPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
    }

    public void navigateTo() {
        driver.get(baseUrl + "/orders/checkout");
    }

    public void enterShippingAddress(String address) {
        org.openqa.selenium.WebElement textarea = driver.findElement(By.id("shippingAddress"));
        textarea.clear();
        textarea.sendKeys(address);
    }

    public void placeOrder() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        WebElement form = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("form")));
        String urlBefore = driver.getCurrentUrl();
        // Submit form via JavaScript to ensure it works in headless mode
        ((JavascriptExecutor) driver).executeScript("arguments[0].submit()", form);
        // Wait for redirect away from checkout (to confirmation or back to checkout on error)
        wait.until(ExpectedConditions.not(ExpectedConditions.urlToBe(urlBefore)));
    }

    public boolean isOnCheckoutPage() {
        return driver.getCurrentUrl().contains("/orders/checkout");
    }

    public boolean isRedirectedToLogin() {
        return driver.getCurrentUrl().contains("/login");
    }

    public String getCartTotal() {
        List<org.openqa.selenium.WebElement> totals = driver.findElements(
            By.cssSelector(".card-body strong"));
        return totals.isEmpty() ? "" : totals.get(totals.size() - 1).getText();
    }
}
