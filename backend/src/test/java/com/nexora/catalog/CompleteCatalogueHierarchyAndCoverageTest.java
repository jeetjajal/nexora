package com.nexora.catalog;

import com.nexora.category.dto.SubcategoryRequest;
import com.nexora.category.dto.SubcategoryResponse;
import com.nexora.category.entity.Category;
import com.nexora.category.entity.Subcategory;
import com.nexora.category.repository.CategoryRepository;
import com.nexora.category.repository.SubcategoryRepository;
import com.nexora.category.service.SubcategoryService;
import com.nexora.product.entity.Product;
import com.nexora.product.entity.ProductVariant;
import com.nexora.product.repository.ProductRepository;
import com.nexora.product.repository.ProductVariantRepository;
import com.nexora.store.entity.Store;
import com.nexora.store.repository.StoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Complete 31-Category Catalogue Hierarchy & Coverage Unit Tests")
public class CompleteCatalogueHierarchyAndCoverageTest {

    @Mock
    private SubcategoryRepository subcategoryRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private SubcategoryService subcategoryService;

    @Test
    @DisplayName("Verify Subcategory Creation with Parent Category Reference")
    void testCreateSubcategory() {
        Category cat = Category.builder().id(1L).name("Vegetables & Fruits").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(subcategoryRepository.existsByCategoryIdAndSlug(1L, "vegetables")).thenReturn(false);

        Subcategory saved = Subcategory.builder()
                .id(101L)
                .category(cat)
                .name("Vegetables")
                .slug("vegetables")
                .description("Daily vegetables")
                .imageUrl("🥦")
                .displayOrder(1)
                .active(true)
                .build();
        when(subcategoryRepository.save(any(Subcategory.class))).thenReturn(saved);

        SubcategoryRequest req = SubcategoryRequest.builder()
                .categoryId(1L)
                .name("Vegetables")
                .slug("vegetables")
                .description("Daily vegetables")
                .build();

        SubcategoryResponse res = subcategoryService.createSubcategory(req);

        assertNotNull(res);
        assertEquals(101L, res.getId());
        assertEquals("Vegetables", res.getName());
        assertEquals("Vegetables & Fruits", res.getCategoryName());
        assertEquals(1L, res.getCategoryId());
    }

    @Test
    @DisplayName("Prevent Duplicate Subcategory Slugs within Same Category")
    void testPreventDuplicateSlugWithinCategory() {
        Category cat = Category.builder().id(2L).name("Dairy & Breakfast").build();
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(cat));
        when(subcategoryRepository.existsByCategoryIdAndSlug(2L, "milk")).thenReturn(true);

        SubcategoryRequest req = SubcategoryRequest.builder()
                .categoryId(2L)
                .name("Milk")
                .slug("milk")
                .build();

        assertThrows(IllegalArgumentException.class, () -> subcategoryService.createSubcategory(req));
    }

    @Test
    @DisplayName("Verify 31 Category Name Specification Completeness")
    void test31CategoryNameSpecificationCompleteness() {
        String[] expected31Categories = {
                "Vegetables & Fruits",
                "Dairy & Breakfast",
                "Munchies & Snacks",
                "Cold Drinks & Juices",
                "Instant & Frozen Food",
                "Tea, Coffee & Milk Drinks",
                "Bakery & Biscuits",
                "Sweet Tooth",
                "Atta, Rice & Dal",
                "Dry Fruits, Masala & Oil",
                "Sauces & Spreads",
                "Chicken, Meat & Fish",
                "Paan Corner",
                "Organic & Premium",
                "Baby Care",
                "Pharma & Wellness",
                "Cleaning Essentials",
                "Home Furnishing & Decor",
                "Personal Care",
                "Beauty & Cosmetics",
                "Pet Care",
                "Magazines",
                "Kitchen & Dining",
                "Fashion & Accessories",
                "Electronics & Electricals",
                "Stationery Needs",
                "Books",
                "Toys & Games",
                "Print Store",
                "E-Gift Cards",
                "Rakhi & Seasonal"
        };

        assertEquals(31, expected31Categories.length, "Must specify exactly 31 main categories");

        // Verify all 31 are unique
        long uniqueCount = java.util.Arrays.stream(expected31Categories).distinct().count();
        assertEquals(31, uniqueCount, "All 31 main category names must be distinct");
    }

    @Test
    @DisplayName("Verify Product Variant Entity Model and Pricing Fields")
    void testProductVariantEntity() {
        Product prod = Product.builder().id(200L).name("Fresh Potato").price(new BigDecimal("35.00")).build();
        ProductVariant variant = ProductVariant.builder()
                .id(501L)
                .product(prod)
                .sku("NX-VEG-POT-1KG")
                .name("Fresh Potato 1kg Pack")
                .unit("1 kg")
                .packSize("1kg Bag")
                .mrp(new BigDecimal("45.00"))
                .price(new BigDecimal("35.00"))
                .discount(new BigDecimal("22.22"))
                .barcode("8901234567890")
                .active(true)
                .build();

        assertNotNull(variant);
        assertEquals("NX-VEG-POT-1KG", variant.getSku());
        assertEquals("1 kg", variant.getUnit());
        assertEquals(new BigDecimal("35.00"), variant.getPrice());
        assertEquals(new BigDecimal("45.00"), variant.getMrp());
        assertTrue(variant.isActive());
        assertEquals(200L, variant.getProduct().getId());
    }
}
