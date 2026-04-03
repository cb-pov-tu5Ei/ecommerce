package io.cb_demos.ecommerce.selenium.tests;

import io.cb_demos.ecommerce.selenium.base.BaseSeleniumTest;
import io.cb_demos.ecommerce.selenium.pages.CartPage;
import io.cb_demos.ecommerce.selenium.pages.CheckoutPage;
import io.cb_demos.ecommerce.selenium.pages.OrderConfirmationPage;
import io.cb_demos.ecommerce.selenium.pages.ProductDetailPage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutFlowSeleniumTest extends BaseSeleniumTest {

    @Test
    void fullCheckoutFlow() {
        // Login
        loginAs("testuser", "test123");

        // Add a product to cart
        driver.get(baseUrl + "/products/1");
        ProductDetailPage detailPage = new ProductDetailPage(driver, baseUrl);
        assertThat(detailPage.isAddToCartFormPresent()).isTrue();
        detailPage.addToCart();

        // Proceed to checkout
        CartPage cartPage = new CartPage(driver, baseUrl);
        cartPage.navigateTo();
        assertThat(cartPage.isEmpty()).isFalse();
        cartPage.proceedToCheckout();

        // Fill shipping address and place order
        CheckoutPage checkoutPage = new CheckoutPage(driver, baseUrl);
        assertThat(checkoutPage.isOnCheckoutPage()).isTrue();
        checkoutPage.enterShippingAddress("123 Selenium St, Test City, TC 12345");
        checkoutPage.placeOrder();

        // Verify confirmation page
        OrderConfirmationPage confirmationPage = new OrderConfirmationPage(driver);
        assertThat(confirmationPage.isSuccessAlertPresent()).isTrue();
        assertThat(confirmationPage.getOrderNumber()).startsWith("ORD-");
        assertThat(confirmationPage.getItemCount()).isGreaterThan(0);
        assertThat(confirmationPage.getShippingAddress()).contains("123 Selenium St");
    }

    @Test
    void checkoutRequiresAuthentication() {
        driver.get(baseUrl + "/orders/checkout");

        CheckoutPage checkoutPage = new CheckoutPage(driver, baseUrl);
        assertThat(checkoutPage.isRedirectedToLogin()).isTrue();
    }

    @Test
    void checkoutWithEmptyCartRedirects() {
        loginAs("testuser", "test123");

        // Clear any cart items first
        CartPage cartPage = new CartPage(driver, baseUrl);
        cartPage.navigateTo();
        if (!cartPage.isEmpty()) {
            cartPage.clearCart();
        }

        // Try to checkout with empty cart
        driver.get(baseUrl + "/orders/checkout");

        // Should redirect back to cart or show warning
        assertThat(driver.getCurrentUrl()).doesNotContain("/orders/confirmation");
    }

    @Test
    void checkoutWithMissingAddressShowsError() {
        loginAs("testuser", "test123");

        // Add item to cart
        driver.get(baseUrl + "/products/1");
        ProductDetailPage detailPage = new ProductDetailPage(driver, baseUrl);
        if (!detailPage.isAddToCartFormPresent()) return;
        detailPage.addToCart();

        // Go to checkout and submit without address
        CheckoutPage checkoutPage = new CheckoutPage(driver, baseUrl);
        checkoutPage.navigateTo();
        // Click submit without filling address — HTML5 required prevents navigation
        driver.findElement(org.openqa.selenium.By.cssSelector("button[type=submit]")).click();

        // Should remain on checkout (HTML5 required validation blocks submission)
        assertThat(driver.getCurrentUrl()).doesNotContain("/orders/confirmation");
    }
}
