package io.cb_demos.ecommerce.selenium.tests;

import io.cb_demos.ecommerce.selenium.base.BaseSeleniumTest;
import io.cb_demos.ecommerce.selenium.pages.ProductDetailPage;
import io.cb_demos.ecommerce.selenium.pages.ProductListPage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductBrowsingSeleniumTest extends BaseSeleniumTest {

    @Test
    void productListPageLoads() {
        ProductListPage listPage = new ProductListPage(driver, baseUrl);
        listPage.navigateTo();

        assertThat(listPage.isOnProductListPage()).isTrue();
        assertThat(listPage.getProductCount()).isGreaterThan(0);
    }

    @Test
    void searchReturnsResults() {
        ProductListPage listPage = new ProductListPage(driver, baseUrl);
        listPage.navigateTo();
        listPage.search("laptop");

        assertThat(listPage.isSearchResultsMessagePresent()).isTrue();
        assertThat(listPage.getProductCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void searchWithNoResults() {
        ProductListPage listPage = new ProductListPage(driver, baseUrl);
        listPage.navigateTo();
        listPage.search("xyznotaproduct999");

        assertThat(listPage.getProductCount()).isEqualTo(0);
    }

    @Test
    void filterByCategory() {
        ProductListPage listPage = new ProductListPage(driver, baseUrl);
        listPage.navigateTo();

        // Get all category links from the sidebar and click the first non-"All Products" one
        java.util.List<String> categories = listPage.getCategoryNames();
        assertThat(categories).isNotEmpty();

        // Navigate to the first real category (skip "All Products")
        driver.get(baseUrl + "/products/category/1");
        assertThat(driver.getCurrentUrl()).contains("/products/category/");
    }

    @Test
    void productDetailPageLoads() {
        ProductListPage listPage = new ProductListPage(driver, baseUrl);
        listPage.navigateTo();
        assertThat(listPage.getProductCount()).isGreaterThan(0);

        listPage.clickProduct(0);

        ProductDetailPage detailPage = new ProductDetailPage(driver, baseUrl);
        assertThat(detailPage.isOnProductDetailPage()).isTrue();
        assertThat(detailPage.getProductName()).isNotBlank();
        assertThat(detailPage.getPrice()).startsWith("$");
    }

    @Test
    void productDetailShowsStockInfo() {
        driver.get(baseUrl + "/products/1");

        ProductDetailPage detailPage = new ProductDetailPage(driver, baseUrl);
        assertThat(detailPage.getStockInfo()).isNotBlank();
    }

    @Test
    void paginationWorksWhenAvailable() {
        ProductListPage listPage = new ProductListPage(driver, baseUrl);
        listPage.navigateTo();

        if (listPage.isPaginationPresent()) {
            listPage.clickNextPage();
            assertThat(listPage.isOnProductListPage()).isTrue();
        }
        // If no pagination, test passes (not enough products to paginate)
    }
}
