package com.nexora.phase6;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.exception.ForbiddenOperationException;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.inventory.dto.InventoryResponse;
import com.nexora.inventory.dto.LowStockThresholdRequest;
import com.nexora.inventory.dto.StockAdjustmentRequest;
import com.nexora.inventory.entity.Inventory;
import com.nexora.inventory.exception.InsufficientStockException;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.inventory.service.InventoryService;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.role.entity.Role;
import com.nexora.role.entity.RoleName;
import com.nexora.store.entity.Store;
import com.nexora.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService unit tests (Phase 6)")
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductRepository productRepository;

    private InventoryService inventoryService;

    private User ownerUser;
    private User otherOwnerUser;
    private User adminUser;
    private Store store;
    private Product product;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryRepository, productRepository);

        Role storeOwnerRole = Role.builder()
                .id(1L)
                .name(RoleName.STORE_OWNER)
                .build();

        Role adminRole = Role.builder()
                .id(2L)
                .name(RoleName.ADMIN)
                .build();

        ownerUser = User.builder()
                .id(10L)
                .name("Owner")
                .email("owner@example.com")
                .password("hashed")
                .status("ACTIVE")
                .roles(Set.of(storeOwnerRole))
                .build();

        otherOwnerUser = User.builder()
                .id(11L)
                .name("Other Owner")
                .email("other@example.com")
                .password("hashed")
                .status("ACTIVE")
                .roles(Set.of(storeOwnerRole))
                .build();

        adminUser = User.builder()
                .id(99L)
                .name("Admin")
                .email("admin@example.com")
                .password("hashed")
                .status("ACTIVE")
                .roles(Set.of(adminRole))
                .build();

        store = Store.builder()
                .id(100L)
                .owner(ownerUser)
                .name("Test Store")
                .build();

        product = Product.builder()
                .id(200L)
                .store(store)
                .name("Test Product")
                .build();
    }

    private Inventory inventoryWith(int quantity, int threshold) {
        return Inventory.builder()
                .id(1L)
                .product(product)
                .quantity(quantity)
                .lowStockThreshold(threshold)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ==============================================================
    // Increase stock
    // ==============================================================

    @Test
    @DisplayName("Owner can increase stock for their own product")
    void ownerCanIncreaseStock() {
        when(productRepository.findById(200L))
                .thenReturn(Optional.of(product));

        when(inventoryRepository.findByProductId(200L))
                .thenReturn(Optional.of(inventoryWith(5, 5)))
                .thenReturn(Optional.of(inventoryWith(25, 5)));

        InventoryResponse response = inventoryService.increaseStock(
                200L,
                new StockAdjustmentRequest(20),
                new UserPrincipal(ownerUser));

        assertThat(response.getQuantity()).isEqualTo(25);

        verify(inventoryRepository)
                .increaseStock(
                        eq(200L),
                        eq(20),
                        any(LocalDateTime.class)
                );
    }

    @Test
    @DisplayName("A different STORE_OWNER cannot increase stock for someone else's product")
    void otherOwnerCannotIncreaseStock() {
        when(productRepository.findById(200L))
                .thenReturn(Optional.of(product));

        assertThatThrownBy(() ->
                inventoryService.increaseStock(
                        200L,
                        new StockAdjustmentRequest(20),
                        new UserPrincipal(otherOwnerUser)))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(inventoryRepository, never())
                .increaseStock(anyLong(), anyInt(), any());
    }

    @Test
    @DisplayName("ADMIN can increase stock for any product")
    void adminCanIncreaseAnyStock() {
        when(productRepository.findById(200L))
                .thenReturn(Optional.of(product));

        when(inventoryRepository.findByProductId(200L))
                .thenReturn(Optional.of(inventoryWith(5, 5)))
                .thenReturn(Optional.of(inventoryWith(15, 5)));

        InventoryResponse response = inventoryService.increaseStock(
                200L,
                new StockAdjustmentRequest(10),
                new UserPrincipal(adminUser));

        assertThat(response.getQuantity()).isEqualTo(15);
    }

    // ==============================================================
    // Reduce stock
    // ==============================================================

    @Test
    @DisplayName("Reducing stock with sufficient quantity succeeds")
    void reduceStockSucceedsWithSufficientQuantity() {
        when(productRepository.findById(200L))
                .thenReturn(Optional.of(product));

        when(inventoryRepository.findByProductId(200L))
                .thenReturn(Optional.of(inventoryWith(10, 5)))
                .thenReturn(Optional.of(inventoryWith(7, 5)));

        when(inventoryRepository.reduceStockIfAvailable(
                eq(200L),
                eq(3),
                any(LocalDateTime.class)))
                .thenReturn(1);

        InventoryResponse response = inventoryService.reduceStock(
                200L,
                new StockAdjustmentRequest(3),
                new UserPrincipal(ownerUser));

        assertThat(response.getQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("Reducing stock beyond what's available throws InsufficientStockException")
    void reduceStockFailsWithInsufficientQuantity() {
        when(productRepository.findById(200L))
                .thenReturn(Optional.of(product));

        when(inventoryRepository.findByProductId(200L))
                .thenReturn(Optional.of(inventoryWith(2, 5)))
                .thenReturn(Optional.of(inventoryWith(2, 5)));

        when(inventoryRepository.reduceStockIfAvailable(
                eq(200L),
                eq(5),
                any(LocalDateTime.class)))
                .thenReturn(0);

        assertThatThrownBy(() ->
                inventoryService.reduceStock(
                        200L,
                        new StockAdjustmentRequest(5),
                        new UserPrincipal(ownerUser)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("requested 5")
                .hasMessageContaining("only 2 available");
    }

    @Test
    @DisplayName("Reducing stock to exactly zero succeeds (boundary case)")
    void reduceStockToExactlyZeroSucceeds() {
        when(productRepository.findById(200L))
                .thenReturn(Optional.of(product));

        when(inventoryRepository.findByProductId(200L))
                .thenReturn(Optional.of(inventoryWith(5, 5)))
                .thenReturn(Optional.of(inventoryWith(0, 5)));

        when(inventoryRepository.reduceStockIfAvailable(
                eq(200L),
                eq(5),
                any(LocalDateTime.class)))
                .thenReturn(1);

        InventoryResponse response = inventoryService.reduceStock(
                200L,
                new StockAdjustmentRequest(5),
                new UserPrincipal(ownerUser));

        assertThat(response.getQuantity()).isEqualTo(0);
        assertThat(response.getStatus()).isEqualTo("OUT_OF_STOCK");
    }

    @Test
    @DisplayName("A different STORE_OWNER cannot reduce stock for someone else's product")
    void otherOwnerCannotReduceStock() {
        when(productRepository.findById(200L))
                .thenReturn(Optional.of(product));

        assertThatThrownBy(() ->
                inventoryService.reduceStock(
                        200L,
                        new StockAdjustmentRequest(1),
                        new UserPrincipal(otherOwnerUser)))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(inventoryRepository, never())
                .reduceStockIfAvailable(anyLong(), anyInt(), any());
    }

    @Test
    @DisplayName("Reducing stock for a nonexistent product throws ResourceNotFoundException")
    void reduceStockForUnknownProductThrows404() {
        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                inventoryService.reduceStock(
                        999L,
                        new StockAdjustmentRequest(1),
                        new UserPrincipal(ownerUser)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==============================================================
    // Low-stock threshold + derived status
    // ==============================================================

    @Test
    @DisplayName("Owner can update the low-stock threshold for their own product")
    void ownerCanUpdateThreshold() {
        Inventory inventory = inventoryWith(20, 5);

        when(productRepository.findById(200L))
                .thenReturn(Optional.of(product));

        when(inventoryRepository.findByProductId(200L))
                .thenReturn(Optional.of(inventory));

        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InventoryResponse response =
                inventoryService.updateLowStockThreshold(
                        200L,
                        new LowStockThresholdRequest(15),
                        new UserPrincipal(ownerUser));

        assertThat(response.getLowStockThreshold()).isEqualTo(15);

        // quantity (20) is now <= new threshold (15)? No,
        // 20 > 15, so still IN_STOCK
        assertThat(response.getStatus()).isEqualTo("IN_STOCK");
    }

    @Test
    @DisplayName("Status is LOW_STOCK when quantity is at or below the threshold, but above zero")
    void statusReflectsLowStock() {

        // FIX:
        // No productRepository.findById() stubbing is needed here.
        // getInventoryForProduct() only needs the inventory repository.
        when(inventoryRepository.findByProductId(200L))
                .thenReturn(Optional.of(inventoryWith(3, 5)));

        InventoryResponse response =
                inventoryService.getInventoryForProduct(200L);

        assertThat(response.getStatus()).isEqualTo("LOW_STOCK");
    }

    @Test
    @DisplayName("Status is IN_STOCK when quantity is comfortably above the threshold")
    void statusReflectsInStock() {

        // FIX:
        // Removed unnecessary productRepository.findById() stubbing.
        when(inventoryRepository.findByProductId(200L))
                .thenReturn(Optional.of(inventoryWith(50, 5)));

        InventoryResponse response =
                inventoryService.getInventoryForProduct(200L);

        assertThat(response.getStatus()).isEqualTo("IN_STOCK");
    }

    @Test
    @DisplayName("Status is OUT_OF_STOCK when quantity is zero")
    void statusReflectsOutOfStock() {

        // FIX:
        // Removed unnecessary productRepository.findById() stubbing.
        when(inventoryRepository.findByProductId(200L))
                .thenReturn(Optional.of(inventoryWith(0, 5)));

        InventoryResponse response =
                inventoryService.getInventoryForProduct(200L);

        assertThat(response.getStatus()).isEqualTo("OUT_OF_STOCK");
    }
}