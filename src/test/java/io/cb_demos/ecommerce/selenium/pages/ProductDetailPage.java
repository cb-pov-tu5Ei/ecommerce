package io.cb_demos.ecommerce.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class ProductDetailPage {

    private final WebDriver driver;
    private final String baseUrl;

    public ProductDetailPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
    }

    public void navigateTo(long productId) {
        driver.get(baseUrl + "/products/" + productId);
    }

    public String getProductName() {
        return driver.findElement(By.cssSelector("main h2")).getText();
    }

    public String getPrice() {
        return driver.findElement(By.cssSelector("h3.text-primary")).getText();
    }

    public boolean isInStock() {
        return !driver.findElements(By.cssSelector(".text-success")).isEmpty();
    }

    public boolean isOutOfStock() {
        return !driver.findElements(By.cssSelector(".text-danger")).isEmpty();
    }

    public String getStockInfo() {
        List<WebElement> inStock = driver.findElements(By.cssSelector(".text-success"));
        if (!inStock.isEmpty()) return inStock.get(0).getText();
        List<WebElement> outOfStock = driver.findElements(By.cssSelector(".text-danger"));
        if (!outOfStock.isEmpty()) return outOfStock.get(0).getText();
        return "";
    }

    public void addToCart(int quantity) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement form = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("form[action*='/cart/add']")));
        WebElement qtyInput = form.findElement(By.id("quantity"));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1]", qtyInput, String.valueOf(quantity));
        String urlBefore = driver.getCurrentUrl();
        // Submit form via JavaScript to ensure it works in headless mode
        ((JavascriptExecutor) driver).executeScript("arguments[0].submit()", form);
        // Wait for redirect to /cart
        wait.until(ExpectedConditions.not(ExpectedConditions.urlToBe(urlBefore)));
    }

    public void addToCart() {
        addToCart(1);
    }

    public boolean isAddToCartFormPresent() {
        return !driver.findElements(By.cssSelector("form[action*='/cart/add']")).isEmpty();
    }

    public void clickContinueShopping() {
        driver.findElement(By.cssSelector("a.btn-outline-secondary")).click();
    }

    public boolean isOnProductDetailPage() {
        return driver.getCurrentUrl().matches(".*\\/products\\/\\d+$");
    }
}
