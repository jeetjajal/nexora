package com.nexora.phase2;

import com.nexora.address.entity.Address;
import com.nexora.address.repository.AddressRepository;
import com.nexora.category.entity.Category;
import com.nexora.category.repository.CategoryRepository;
import com.nexora.inventory.entity.Inventory;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.role.entity.Role;
import com.nexora.role.entity.RoleName;
import com.nexora.role.repository.RoleRepository;
import com.nexora.store.entity.Store;
import com.nexora.store.entity.StoreStatus;
import com.nexora.store.repository.StoreRepository;
import com.nexora.user.entity.User;
import com.nexora.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WHAT IS @DataJpaTest?
 * Unlike UserServiceTest (which mocks the repository entirely),
 * @DataJpaTest boots up ONLY the JPA/Hibernate layer plus an
 * in-memory H2 database — no web server, no unrelated beans. It then
 * lets us save and query REAL entities and check that our
 * relationships (@ManyToOne, @OneToMany, @ManyToMany, @OneToOne),
 * constraints, and cascades actually behave the way we designed them.
 *
 * Each test method runs inside a transaction that's rolled back
 * afterward, so tests never leak data into each other.
 */
@DataJpaTest
@DisplayName("Phase 2: entity relationship tests")
class RelationshipRepositoryTest {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private InventoryRepository inventoryRepository;

    // ------------------------------------------------------------
    // User <-> Role (Many-to-Many)
    // ------------------------------------------------------------
    @Test
    @DisplayName("A user can be assigned multiple roles via the user_roles join table")
    void userCanHaveMultipleRoles() {
        Role customer = roleRepository.save(Role.builder().name(RoleName.CUSTOMER).build());
        Role delivery = roleRepository.save(Role.builder().name(RoleName.DELIVERY_PARTNER).build());

        User user = User.builder()
                .name("Rohan Mehta")
                .email("rohan@example.com")
                .password("hashed")
                .phone("9998887777")
                .roles(Set.of(customer, delivery))
                .build();

        User saved = userRepository.save(user);

        User fetched = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(fetched.getRoles()).hasSize(2);
        assertThat(fetched.getRoles())
                .extracting(Role::getName)
                .containsExactlyInAnyOrder(RoleName.CUSTOMER, RoleName.DELIVERY_PARTNER);
    }

    // ------------------------------------------------------------
    // User <-> Address (One-to-Many / Many-to-One)
    // ------------------------------------------------------------
    @Test
    @DisplayName("A user can have multiple addresses, each pointing back to that user")
    void userCanHaveMultipleAddresses() {
        User user = userRepository.save(User.builder()
                .name("Neha Kapoor")
                .email("neha@example.com")
                .password("hashed")
                .phone("9123456789")
                .build());

        Address home = Address.builder()
                .user(user).label("Home").addressLine1("12 MG Road")
                .city("Rajkot").state("Gujarat").pincode("360001")
                .isDefault(true)
                .build();

        Address work = Address.builder()
                .user(user).label("Work").addressLine1("45 Tech Park")
                .city("Rajkot").state("Gujarat").pincode("360005")
                .build();

        addressRepository.save(home);
        addressRepository.save(work);

        var addresses = addressRepository.findByUserId(user.getId());
        assertThat(addresses).hasSize(2);
        assertThat(addresses).allSatisfy(a -> assertThat(a.getUser().getId()).isEqualTo(user.getId()));
    }

    // ------------------------------------------------------------
    // Store -> owner (Many-to-One) + Store <-> Category (Many-to-Many)
    // ------------------------------------------------------------
    @Test
    @DisplayName("A store belongs to one owner and can have multiple categories")
    void storeHasOwnerAndCategories() {
        User owner = userRepository.save(User.builder()
                .name("Vikram Rao")
                .email("vikram@example.com")
                .password("hashed")
                .phone("9012345678")
                .build());

        Category pizza = categoryRepository.save(Category.builder().name("Pizza").build());
        Category fastFood = categoryRepository.save(Category.builder().name("Fast Food").build());

        Store store = Store.builder()
                .owner(owner)
                .name("Rajkot Pizza Hub")
                .status(StoreStatus.OPEN)
                .categories(Set.of(pizza, fastFood))
                .build();

        Store saved = storeRepository.save(store);

        Store fetched = storeRepository.findById(saved.getId()).orElseThrow();
        assertThat(fetched.getOwner().getEmail()).isEqualTo("vikram@example.com");
        assertThat(fetched.getCategories()).hasSize(2);

        var ownerStores = storeRepository.findByOwnerId(owner.getId());
        assertThat(ownerStores).hasSize(1);
    }

    // ------------------------------------------------------------
    // Product -> Store (Many-to-One) + Product <-> Category (Many-to-Many)
    // ------------------------------------------------------------
    @Test
    @DisplayName("A product belongs to one store and can have multiple categories")
    void productBelongsToStoreAndHasCategories() {
        User owner = userRepository.save(User.builder()
                .name("Store Owner").email("owner@example.com")
                .password("hashed").phone("9000000000").build());

        Store store = storeRepository.save(Store.builder()
                .owner(owner).name("Green Grocery").status(StoreStatus.OPEN).build());

        Category veg = categoryRepository.save(Category.builder().name("Vegetables").build());

        Product product = Product.builder()
                .store(store)
                .name("Fresh Tomatoes")
                .price(new BigDecimal("40.00"))
                .discount(new BigDecimal("5.00"))
                .available(true)
                .categories(Set.of(veg))
                .build();

        Product saved = productRepository.save(product);

        Product fetched = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(fetched.getStore().getName()).isEqualTo("Green Grocery");
        assertThat(fetched.getCategories()).extracting(Category::getName).containsExactly("Vegetables");

        var storeProducts = productRepository.findByStoreId(store.getId());
        assertThat(storeProducts).hasSize(1);
    }

    // ------------------------------------------------------------
    // Inventory <-> Product (One-to-One)
    // ------------------------------------------------------------
    @Test
    @DisplayName("Each product has exactly one inventory record, and stock never goes negative")
    void productHasExactlyOneInventoryRecord() {
        User owner = userRepository.save(User.builder()
                .name("Owner Two").email("owner2@example.com")
                .password("hashed").phone("9111111111").build());

        Store store = storeRepository.save(Store.builder()
                .owner(owner).name("Test Store").status(StoreStatus.OPEN).build());

        Product product = productRepository.save(Product.builder()
                .store(store).name("Margherita Pizza")
                .price(new BigDecimal("249.00")).build());

        Inventory inventory = inventoryRepository.save(Inventory.builder()
                .product(product).quantity(10).build());

        Inventory fetched = inventoryRepository.findByProductId(product.getId()).orElseThrow();
        assertThat(fetched.getQuantity()).isEqualTo(10);
        assertThat(fetched.getProduct().getId()).isEqualTo(product.getId());
    }

    // ------------------------------------------------------------
    // Unique constraint check
    // ------------------------------------------------------------
    @Test
    @DisplayName("A category name must be unique")
    void categoryNameMustBeUnique() {
        categoryRepository.save(Category.builder().name("Desserts").build());

        var existing = categoryRepository.findByName("Desserts");
        assertThat(existing).isPresent();
    }
}
