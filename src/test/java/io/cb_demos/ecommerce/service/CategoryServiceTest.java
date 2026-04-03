package io.cb_demos.ecommerce.service;

import io.cb_demos.ecommerce.domain.Category;
import io.cb_demos.ecommerce.repository.CategoryRepository;
import io.cb_demos.ecommerce.service.impl.CategoryServiceImpl;
import io.cb_demos.ecommerce.util.TestDelayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category electronicsCategory;
    private Category clothingCategory;
    private Category booksCategory;

    @BeforeEach
    void setUp() {
        electronicsCategory = new Category();
        electronicsCategory.setId(1L);
        electronicsCategory.setName("Electronics");
        electronicsCategory.setDescription("Electronic devices and accessories");

        clothingCategory = new Category();
        clothingCategory.setId(2L);
        clothingCategory.setName("Clothing");
        clothingCategory.setDescription("Apparel and fashion items");

        booksCategory = new Category();
        booksCategory.setId(3L);
        booksCategory.setName("Books");
        booksCategory.setDescription("Physical and digital books");
    }

    @Test
    void findAllCategories_shouldReturnAllCategories() {
        TestDelayUtil.mediumDelay();
        // Given
        List<Category> categories = Arrays.asList(electronicsCategory, clothingCategory, booksCategory);
        when(categoryRepository.findAll()).thenReturn(categories);

        // When
        List<Category> result = categoryService.findAllCategories();

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(Category::getName)
                .containsExactly("Electronics", "Clothing", "Books");
        verify(categoryRepository).findAll();
    }

    @Test
    void findAllCategories_shouldReturnEmptyList_whenNoCategories() {
        // Given
        when(categoryRepository.findAll()).thenReturn(Arrays.asList());

        // When
        List<Category> result = categoryService.findAllCategories();

        // Then
        assertThat(result).isEmpty();
        verify(categoryRepository).findAll();
    }

    @Test
    void findById_shouldReturnCategory_whenCategoryExists() {
        TestDelayUtil.mediumDelay();
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronicsCategory));

        // When
        Category result = categoryService.findById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Electronics");
        verify(categoryRepository).findById(1L);
    }

    @Test
    void findById_shouldThrowException_whenCategoryNotFound() {
        // Given
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> categoryService.findById(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found")
                .hasMessageContaining("999");
        verify(categoryRepository).findById(999L);
    }

    @Test
    void findByName_shouldReturnCategory_whenNameExists() {
        // Given
        when(categoryRepository.findByName("Electronics")).thenReturn(Optional.of(electronicsCategory));

        // When
        Optional<Category> result = categoryService.findByName("Electronics");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Electronics");
        verify(categoryRepository).findByName("Electronics");
    }

    @Test
    void findByName_shouldReturnEmpty_whenNameNotFound() {
        // Given
        when(categoryRepository.findByName("NonExistent")).thenReturn(Optional.empty());

        // When
        Optional<Category> result = categoryService.findByName("NonExistent");

        // Then
        assertThat(result).isEmpty();
        verify(categoryRepository).findByName("NonExistent");
    }

    @Test
    void saveCategory_shouldCreateNewCategory() {
        // Given
        Category newCategory = new Category();
        newCategory.setName("Home & Garden");
        newCategory.setDescription("Home improvement and garden supplies");

        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setId(4L);
            return saved;
        });

        // When
        Category result = categoryService.saveCategory(newCategory);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(4L);
        assertThat(result.getName()).isEqualTo("Home & Garden");
        verify(categoryRepository).save(newCategory);
    }

    @Test
    void updateCategory_shouldUpdateExistingCategory() {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronicsCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(electronicsCategory);

        electronicsCategory.setDescription("Updated electronics description");

        // When
        Category result = categoryService.updateCategory(1L, electronicsCategory);

        // Then
        assertThat(result.getDescription()).isEqualTo("Updated electronics description");
        verify(categoryRepository).findById(1L);
        verify(categoryRepository).save(electronicsCategory);
    }

    @Test
    void deleteCategory_shouldDeleteCategory_whenExists() {
        // Given
        when(categoryRepository.existsById(1L)).thenReturn(true);
        doNothing().when(categoryRepository).deleteById(1L);

        // When
        categoryService.deleteCategory(1L);

        // Then
        verify(categoryRepository).existsById(1L);
        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void deleteCategory_shouldThrowException_whenCategoryNotFound() {
        // Given
        when(categoryRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> categoryService.deleteCategory(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found");
        verify(categoryRepository).existsById(999L);
        verify(categoryRepository, never()).deleteById(anyLong());
    }

    @Test
    void getCategoryProductCount_shouldReturnCorrectCount() {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronicsCategory));
        when(categoryRepository.countProductsByCategory(1L)).thenReturn(42L);

        // When
        long count = categoryService.getCategoryProductCount(1L);

        // Then
        assertThat(count).isEqualTo(42L);
        verify(categoryRepository).countProductsByCategory(1L);
    }

    @Test
    void findCategoriesWithProducts_shouldReturnOnlyCategoriesWithProducts() {
        // Given
        List<Category> categoriesWithProducts = Arrays.asList(electronicsCategory, clothingCategory);
        when(categoryRepository.findCategoriesWithProducts()).thenReturn(categoriesWithProducts);

        // When
        List<Category> result = categoryService.findCategoriesWithProducts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains(electronicsCategory, clothingCategory);
        assertThat(result).doesNotContain(booksCategory);
        verify(categoryRepository).findCategoriesWithProducts();
    }
}
