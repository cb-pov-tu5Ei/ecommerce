package io.cb_demos.ecommerce.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class OrderListPage {

    private final WebDriver driver;
    private final String baseUrl;

    public OrderListPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
    }

    public void navigateTo() {
        driver.get(baseUrl + "/orders");
    }

    public void navigateToHistory() {
        driver.get(baseUrl + "/orders/history");
    }

    public boolean isEmpty() {
        return !driver.findElements(By.cssSelector(".alert-info")).isEmpty();
    }

    public int getOrderCount() {
        return driver.findElements(By.cssSelector("table tbody tr")).size();
    }

    public void clickOrder(int index) {
        List<WebElement> viewButtons = driver.findElements(
            By.cssSelector("table tbody .btn-outline-primary"));
        viewButtons.get(index).click();
    }

    public String getOrderNumberAt(int index) {
        List<WebElement> rows = driver.findElements(By.cssSelector("table tbody tr"));
        if (index >= rows.size()) return "";
        List<WebElement> cells = rows.get(index).findElements(By.tagName("td"));
        return cells.isEmpty() ? "" : cells.get(0).getText();
    }

    public boolean isOnOrderListPage() {
        return driver.getCurrentUrl().contains("/orders");
    }

    public boolean isRedirectedToLogin() {
        return driver.getCurrentUrl().contains("/login");
    }
}
