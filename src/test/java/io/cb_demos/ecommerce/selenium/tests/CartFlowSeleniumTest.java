package io.cb_demos.ecommerce.selenium.tests;

import io.cb_demos.ecommerce.selenium.base.BaseSeleniumTest;
import io.cb_demos.ecommerce.selenium.pages.CartPage;
import io.cb_demos.ecommerce.selenium.pages.ProductDetailPage;
import io.cb_demos.ecommerce.selenium.pages.ProductListPage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CartFlowSeleniumTest extends BaseSeleniumTest {

    @Test
    void anonymousUserCanUseCart() {
        CartPage cartPage = new CartPage(driver, baseUrl);
        cartPage.navigateTo();

        // Cart page loads without login
        assertThat(cartPage.isOnCartPage()).isTrue();
    }

    @Test
    void addItemToCartFromProductList() {
        // Navigate to a product detail and add to cart
        driver.get(baseUrl + "/products/1");
        ProductDetailPage detailPage = new ProductDetailPage(driver, baseUrl);

        if (detailPage.isAddToCartFormPresent()) {
            detailPage.addToCart();

            CartPage cartPage = new CartPage(driver, baseUrl);
            cartPage.navigateTo();
            assertThat(cartPage.isEmpty()).isFalse();
            assertThat(cartPage.getItemCount()).isGreaterThan(0);
        }
    }

    @Test
    void addItemFromProductDetail() {
        ProductDetailPage detailPage = new ProductDetailPage(driver, baseUrl);
        detailPage.navigateTo(1);

        if (detailPage.isAddToCartFormPresent()) {
            detailPage.addToCart(2);

            CartPage cartPage = new CartPage(driver, baseUrl);
            cartPage.navigateTo();
            assertThat(cartPage.isEmpty()).isFalse();
        }
    }

    @Test
    void cartPersistsOnPageNavigation() {
        // Add item to cart
        driver.get(baseUrl + "/products/1");
        ProductDetailPage detailPage = new ProductDetailPage(driver, baseUrl);

        if (detailPage.isAddToCartFormPresent()) {
            detailPage.addToCart();

            // Navigate away
            driver.get(baseUrl + "/");
            driver.get(baseUrl + "/products");

            // Cart should still have items
            CartPage cartPage = new CartPage(driver, baseUrl);
            cartPage.navigateTo();
            assertThat(cartPage.isEmpty()).isFalse();
        }
    }

    @Test
    void removeItemFromCart() {
        // First add an item
        driver.get(baseUrl + "/products/1");
        ProductDetailPage detailPage = new ProductDetailPage(driver, baseUrl);

        if (!detailPage.isAddToCartFormPresent()) return;
        detailPage.addToCart();

        CartPage cartPage = new CartPage(driver, baseUrl);
        cartPage.navigateTo();
        int initialCount = cartPage.getItemCount();
        assertThat(initialCount).isGreaterThan(0);

        cartPage.removeItem(0);

        cartPage.navigateTo();
        assertThat(cartPage.getItemCount()).isLessThan(initialCount);
    }

    @Test
    void clearCart() {
        // Add items first
        driver.get(baseUrl + "/products/1");
        ProductDetailPage detailPage = new ProductDetailPage(driver, baseUrl);

        if (!detailPage.isAddToCartFormPresent()) return;
        detailPage.addToCart();

        CartPage cartPage = new CartPage(driver, baseUrl);
        cartPage.navigateTo();
        assertThat(cartPage.isEmpty()).isFalse();

        cartPage.clearCart();

        cartPage.navigateTo();
        assertThat(cartPage.isEmpty()).isTrue();
    }

    @Test
    void checkoutButtonRequiresLogin() {
        // Add an item as anonymous user
        driver.get(baseUrl + "/products/1");
        ProductDetailPage detailPage = new ProductDetailPage(driver, baseUrl);

        if (!detailPage.isAddToCartFormPresent()) return;
        detailPage.addToCart();

        CartPage cartPage = new CartPage(driver, baseUrl);
        cartPage.navigateTo();

        // Anonymous user should not see the checkout button
        assertThat(cartPage.isCheckoutButtonPresent()).isFalse();
        assertThat(cartPage.isLoginPromptPresent()).isTrue();
    }
}
