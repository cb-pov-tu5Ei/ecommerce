package io.cb_demos.ecommerce.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.List;

public class HomePage {

    private final WebDriver driver;
    private final String baseUrl;

    public HomePage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
    }

    public void navigateTo() {
        driver.get(baseUrl + "/");
    }

    public String getTitle() {
        return driver.getTitle();
    }

    public void clickProducts() {
        driver.findElement(By.cssSelector("a.nav-link[href*='/products']")).click();
    }

    public void clickLogin() {
        driver.findElement(By.cssSelector("a.nav-link[href*='/login']")).click();
    }

    public void clickRegister() {
        driver.findElement(By.cssSelector("a.nav-link[href*='/register']")).click();
    }

    public void clickCart() {
        driver.findElement(By.cssSelector("a.nav-link[href*='/cart']")).click();
    }

    public int getFeaturedProductCount() {
        return driver.findElements(By.cssSelector("h2 + .row .card")).size();
    }

    public boolean isShopNowButtonPresent() {
        return !driver.findElements(By.cssSelector("a.btn-primary.btn-lg")).isEmpty();
    }

    public boolean isLoggedIn() {
        return !driver.findElements(By.cssSelector("button.btn-link.nav-link")).isEmpty();
    }

    public String getWelcomeHeading() {
        List<org.openqa.selenium.WebElement> elements = driver.findElements(By.cssSelector("h1.display-4"));
        return elements.isEmpty() ? "" : elements.get(0).getText();
    }
}
