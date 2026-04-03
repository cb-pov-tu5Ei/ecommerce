package io.cb_demos.ecommerce.controller;

import io.cb_demos.ecommerce.service.CategoryService;
import io.cb_demos.ecommerce.service.ProductService;
import io.cb_demos.ecommerce.service.ProductReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ProductReviewService reviewService;

    @GetMapping
    public String listProducts(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "12") int size,
                               Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<?> productsPage = productService.findAllProducts(pageable);

        model.addAttribute("products", productsPage.getContent());
        model.addAttribute("currentPage", productsPage.getNumber());
        model.addAttribute("totalPages", productsPage.getTotalPages());
        model.addAttribute("categories", categoryService.findAllCategories());
        return "products/list";
    }

    @GetMapping("/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.findById(id));
        model.addAttribute("reviews", reviewService.getReviewsByProduct(id));
        model.addAttribute("averageRating", reviewService.getAverageRating(id));
        model.addAttribute("reviewCount", reviewService.getReviewCount(id));
        return "products/detail";
    }

    @GetMapping("/category/{categoryId}")
    public String productsByCategory(@PathVariable Long categoryId,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "12") int size,
                                      Model model) {
        // This will throw IllegalArgumentException if category not found, which GlobalExceptionHandler will catch
        model.addAttribute("category", categoryService.findById(categoryId));

        Pageable pageable = PageRequest.of(page, size);
        Page<?> productsPage = productService.findByCategory(categoryId, pageable);

        model.addAttribute("products", productsPage.getContent());
        model.addAttribute("currentPage", productsPage.getNumber());
        model.addAttribute("totalPages", productsPage.getTotalPages());
        model.addAttribute("categories", categoryService.findAllCategories());
        model.addAttribute("selectedCategoryId", categoryId);
        return "products/list";
    }

    @GetMapping("/search")
    public String searchProducts(@RequestParam(required = false) String query,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "12") int size,
                                  Model model) {
        if (query == null || query.trim().isEmpty()) {
            return "redirect:/products";
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<?> productsPage = productService.searchProducts(query, pageable);

        model.addAttribute("products", productsPage.getContent());
        model.addAttribute("currentPage", productsPage.getNumber());
        model.addAttribute("totalPages", productsPage.getTotalPages());
        model.addAttribute("categories", categoryService.findAllCategories());
        model.addAttribute("searchQuery", query);
        return "products/list";
    }
}
