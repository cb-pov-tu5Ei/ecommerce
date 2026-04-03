package io.cb_demos.ecommerce.repository;

import io.cb_demos.ecommerce.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    long countProductsByCategory(@Param("categoryId") Long categoryId);

    @Query("SELECT DISTINCT c FROM Category c JOIN Product p ON p.category = c WHERE p.active = true")
    List<Category> findCategoriesWithProducts();
}
