package io.cb_demos.ecommerce.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class ProductListPage {

    private final WebDriver driver;
    private final String baseUrl;

    public ProductListPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
    }

    public void navigateTo() {
        driver.get(baseUrl + "/products");
    }

    public void search(String query) {
        WebElement searchInput = driver.findElement(By.name("query"));
        searchInput.clear();
        searchInput.sendKeys(query);
        String urlBefore = driver.getCurrentUrl();
        driver.findElement(By.cssSelector("form button[type=submit]")).click();
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.not(ExpectedConditions.urlToBe(urlBefore)));
    }

    public void filterByCategory(long categoryId) {
        driver.get(baseUrl + "/products/category/" + categoryId);
    }

    public int getProductCount() {
        return driver.findElements(By.cssSelector(".col-md-8 .card")).size();
    }

    public void clickProduct(int index) {
        List<WebElement> viewButtons = driver.findElements(
            By.cssSelector(".col-md-8 .card-footer a.btn-primary"));
        viewButtons.get(index).click();
    }

    public boolean isSearchResultsMessagePresent() {
        return !driver.findElements(By.cssSelector(".text-muted")).isEmpty();
    }

    public boolean isOnProductListPage() {
        return driver.getCurrentUrl().contains("/products");
    }

    public boolean isPaginationPresent() {
        return !driver.findElements(By.cssSelector(".pagination")).isEmpty();
    }

    public void clickNextPage() {
        WebElement nextBtn = driver.findElement(By.cssSelector(".page-item:last-child a.page-link"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true)", nextBtn);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click()", nextBtn);
    }

    public List<String> getCategoryNames() {
        List<WebElement> links = driver.findElements(By.cssSelector(".list-group-item"));
        return links.stream()
            .map(WebElement::getText)
            .toList();
    }
}
