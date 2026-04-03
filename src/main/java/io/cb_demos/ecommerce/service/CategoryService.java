package io.cb_demos.ecommerce.service;

import io.cb_demos.ecommerce.domain.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryService {

    List<Category> findAllCategories();

    Category findById(Long id);

    Optional<Category> findByName(String name);

    Category saveCategory(Category category);

    Category updateCategory(Long id, Category category);

    void deleteCategory(Long id);

    long getCategoryProductCount(Long categoryId);

    List<Category> findCategoriesWithProducts();
}
