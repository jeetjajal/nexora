package com.nexora.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.Set;

/**
 * Used for BOTH create and update — Phase 4 doesn't need two separate
 * shapes since a store update replaces the same set of editable
 * fields a creation would set. `categoryNames` references Category
 * rows that must already exist (Category Management is ADMIN-only —
 * a store owner can attach existing categories to their store but
 * can't invent new ones on the fly).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StoreRequest {

    @NotBlank(message = "Store name is required")
    @Size(min = 2, max = 150, message = "Store name must be between 2 and 150 characters")
    private String name;

    @Size(max = 1000, message = "Description must be under 1000 characters")
    private String description;

    private String imageUrl;

    private LocalTime openingTime;

    private LocalTime closingTime;

    /**
     * Category names (must already exist — see CategoryController).
     * Optional: a store can be created with zero categories and have
     * them attached later via update.
     */
    private Set<String> categoryNames;
}
