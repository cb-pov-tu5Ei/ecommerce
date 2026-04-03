package io.cb_demos.ecommerce.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class CartPage {

    private final WebDriver driver;
    private final String baseUrl;

    public CartPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
    }

    public void navigateTo() {
        driver.get(baseUrl + "/cart");
    }

    public boolean isEmpty() {
        return !driver.findElements(By.cssSelector(".alert-info")).isEmpty();
    }

    public int getItemCount() {
        return driver.findElements(By.cssSelector("table tbody tr")).size();
    }

    public String getTotal() {
        return driver.findElement(By.cssSelector("tfoot strong")).getText();
    }

    public void updateQuantity(int rowIndex, int quantity) {
        List<WebElement> forms = driver.findElements(By.cssSelector("form[action*='/cart/update']"));
        WebElement input = forms.get(rowIndex).findElement(By.name("quantity"));
        input.clear();
        input.sendKeys(String.valueOf(quantity));
        forms.get(rowIndex).findElement(By.cssSelector("button[type=submit]")).click();
    }

    public void removeItem(int rowIndex) {
        List<WebElement> forms = driver.findElements(By.cssSelector("form[action*='/cart/remove']"));
        WebElement form = forms.get(rowIndex);
        form.findElement(By.cssSelector("button[type=submit]")).click();
        // Wait for page reload (URL stays /cart, so wait for stale element)
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.stalenessOf(form));
    }

    public void clearCart() {
        WebElement clearButton = driver.findElement(By.cssSelector("form[action*='/cart/clear'] button[type=submit]"));
        clearButton.click();
        // Wait for page reload (URL stays /cart, so wait for stale element)
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.stalenessOf(clearButton));
    }

    public void proceedToCheckout() {
        // Navigate directly to checkout instead of clicking link
        // This is more reliable in headless mode
        driver.get(baseUrl + "/orders/checkout");
    }

    public boolean isCheckoutButtonPresent() {
        return !driver.findElements(By.cssSelector("a[href*='/orders/checkout']")).isEmpty();
    }

    public boolean isLoginPromptPresent() {
        return !driver.findElements(By.cssSelector("a[href*='/login']")).isEmpty();
    }

    public boolean isOnCartPage() {
        return driver.getCurrentUrl().contains("/cart");
    }
}
