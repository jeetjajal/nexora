package com.nexora.category.repository;

import com.nexora.category.entity.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {
    List<Subcategory> findByCategoryIdOrderByDisplayOrderAsc(Long categoryId);
    List<Subcategory> findByCategoryIdAndActiveTrueOrderByDisplayOrderAsc(Long categoryId);
    List<Subcategory> findByActiveTrueOrderByDisplayOrderAsc();
    Optional<Subcategory> findBySlug(String slug);
    Optional<Subcategory> findByNameAndCategoryId(String name, Long categoryId);
    boolean existsByCategoryIdAndSlug(Long categoryId, String slug);
}
