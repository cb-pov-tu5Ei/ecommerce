package io.cb_demos.ecommerce.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class OrderConfirmationPage {

    private final WebDriver driver;

    public OrderConfirmationPage(WebDriver driver) {
        this.driver = driver;
    }

    public boolean isOnConfirmationPage() {
        return driver.getCurrentUrl().contains("/orders/confirmation")
            || !driver.findElements(By.cssSelector(".alert-success h2")).isEmpty();
    }

    public String getOrderNumber() {
        // The order number is in a <span> following "Order Number:" text
        List<WebElement> paragraphs = driver.findElements(By.cssSelector(".card-body p"));
        for (WebElement p : paragraphs) {
            if (p.getText().startsWith("Order Number:")) {
                return p.findElement(By.tagName("span")).getText();
            }
        }
        return "";
    }

    public int getItemCount() {
        return driver.findElements(By.cssSelector("table tbody tr")).size();
    }

    public String getTotal() {
        List<WebElement> paragraphs = driver.findElements(By.cssSelector(".card-body p"));
        for (WebElement p : paragraphs) {
            if (p.getText().startsWith("Total Amount:")) {
                return p.findElement(By.tagName("span")).getText();
            }
        }
        return "";
    }

    public String getShippingAddress() {
        List<WebElement> paragraphs = driver.findElements(By.cssSelector(".card-body p"));
        for (WebElement p : paragraphs) {
            if (p.getText().startsWith("Shipping Address:")) {
                return p.findElement(By.tagName("span")).getText();
            }
        }
        return "";
    }

    public boolean isSuccessAlertPresent() {
        return !driver.findElements(By.cssSelector(".alert-success")).isEmpty();
    }

    public void clickViewOrderHistory() {
        driver.findElement(By.cssSelector("a[href*='/orders/history']")).click();
    }

    public void clickContinueShopping() {
        driver.findElement(By.cssSelector("a.btn-primary[href='/']")).click();
    }
}
