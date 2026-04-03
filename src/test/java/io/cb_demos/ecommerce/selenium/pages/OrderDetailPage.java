package io.cb_demos.ecommerce.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class OrderDetailPage {

    private final WebDriver driver;

    public OrderDetailPage(WebDriver driver) {
        this.driver = driver;
    }

    public String getOrderNumber() {
        return getFieldValue("Order Number:");
    }

    public String getStatus() {
        return getFieldValue("Status:");
    }

    public String getTotalAmount() {
        return getFieldValue("Total Amount:");
    }

    public String getShippingAddress() {
        return getFieldValue("Shipping Address:");
    }

    public int getItemCount() {
        return driver.findElements(By.cssSelector("table tbody tr")).size();
    }

    public boolean isCancelButtonPresent() {
        return !driver.findElements(By.cssSelector("button.btn-danger")).isEmpty();
    }

    public void cancelOrder() {
        driver.findElement(By.cssSelector("button.btn-danger")).click();
        // Handle the confirm dialog
        driver.switchTo().alert().accept();
    }

    public boolean isOnOrderDetailPage() {
        return driver.getCurrentUrl().matches(".*\\/orders\\/\\d+$");
    }

    public void clickBackToOrders() {
        driver.findElement(By.cssSelector("a[href*='/orders']")).click();
    }

    private String getFieldValue(String label) {
        List<WebElement> paragraphs = driver.findElements(By.cssSelector(".card-body p"));
        for (WebElement p : paragraphs) {
            if (p.getText().startsWith(label)) {
                List<WebElement> spans = p.findElements(By.tagName("span"));
                return spans.isEmpty() ? "" : spans.get(0).getText();
            }
        }
        return "";
    }
}
