package io.cb_demos.ecommerce.controller;

import io.cb_demos.ecommerce.service.CategoryService;
import io.cb_demos.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping({"/", "/home", "/index"})
    public String home(Model model) {
        List<?> featuredProducts = productService.findFeaturedProducts();
        if (featuredProducts == null) {
            throw new RuntimeException("Featured products cannot be null");
        }

        model.addAttribute("featuredProducts", featuredProducts);
        model.addAttribute("categories", categoryService.findAllCategories());
        return "home";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }
}
