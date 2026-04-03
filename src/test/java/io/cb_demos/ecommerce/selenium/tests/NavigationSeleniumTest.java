package io.cb_demos.ecommerce.selenium.tests;

import io.cb_demos.ecommerce.selenium.base.BaseSeleniumTest;
import io.cb_demos.ecommerce.selenium.pages.HomePage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NavigationSeleniumTest extends BaseSeleniumTest {

    @Test
    void homePageLoads() {
        HomePage homePage = new HomePage(driver, baseUrl);
        homePage.navigateTo();

        assertThat(driver.getTitle()).containsIgnoringCase("home");
        assertThat(homePage.getWelcomeHeading()).isNotBlank();
        assertThat(homePage.isShopNowButtonPresent()).isTrue();
    }

    @Test
    void featuredProductsVisibleOnHomePage() {
        HomePage homePage = new HomePage(driver, baseUrl);
        homePage.navigateTo();

        assertThat(homePage.getFeaturedProductCount()).isGreaterThan(0);
    }

    @Test
    void aboutPageLoads() {
        driver.get(baseUrl + "/about");

        assertThat(driver.getTitle()).containsIgnoringCase("about");
        assertThat(driver.getPageSource()).containsIgnoringCase("about");
    }

    @Test
    void contactPageLoads() {
        driver.get(baseUrl + "/contact");

        assertThat(driver.getTitle()).containsIgnoringCase("contact");
        assertThat(driver.getPageSource()).containsIgnoringCase("contact");
    }

    @Test
    void navLinksWork() {
        HomePage homePage = new HomePage(driver, baseUrl);
        homePage.navigateTo();

        homePage.clickProducts();
        assertThat(driver.getCurrentUrl()).contains("/products");

        driver.navigate().back();
        homePage.clickCart();
        assertThat(driver.getCurrentUrl()).contains("/cart");
    }

    @Test
    void brandLogoNavigatesToHome() {
        driver.get(baseUrl + "/products");
        driver.findElement(org.openqa.selenium.By.cssSelector("a.navbar-brand")).click();

        assertThat(driver.getCurrentUrl()).doesNotContain("/products");
    }
}
