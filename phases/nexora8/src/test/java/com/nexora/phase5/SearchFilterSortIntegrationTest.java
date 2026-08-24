package com.nexora.phase5;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.auth.security.JwtService;
import com.nexora.category.entity.Category;
import com.nexora.category.repository.CategoryRepository;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.product.repository.ProductRepository;
import com.nexora.role.entity.Role;
import com.nexora.role.entity.RoleName;
import com.nexora.role.repository.RoleRepository;
import com.nexora.store.entity.Store;
import com.nexora.store.entity.StoreStatus;
import com.nexora.store.repository.StoreRepository;
import com.nexora.user.entity.User;
import com.nexora.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SearchFilterSortIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User ownerOne;
    private User customer;

    private String ownerToken;
    private String customerToken;

    // ============================================================
    // SETUP
    // ============================================================

    @BeforeEach
    void setUp() {

        inventoryRepository.deleteAll();
        productRepository.deleteAll();
        storeRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        Role customerRole =
                roleRepository.findByName(RoleName.CUSTOMER)
                        .orElseGet(() ->
                                roleRepository.save(
                                        Role.builder()
                                                .name(RoleName.CUSTOMER)
                                                .build()
                                )
                        );

        Role ownerRole =
                roleRepository.findByName(RoleName.STORE_OWNER)
                        .orElseGet(() ->
                                roleRepository.save(
                                        Role.builder()
                                                .name(RoleName.STORE_OWNER)
                                                .build()
                                )
                        );

        ownerOne =
                userRepository.save(
                        User.builder()
                                .name("Owner One")
                                .email(
                                        "owner-"
                                                + UUID.randomUUID()
                                                + "@test.com"
                                )
                                .password(
                                        passwordEncoder.encode(
                                                "Password@123"
                                        )
                                )
                                .phone("9999999999")
                                .roles(
                                        new HashSet<>(
                                                Set.of(ownerRole)
                                        )
                                )
                                .build()
                );

        customer =
                userRepository.save(
                        User.builder()
                                .name("Customer")
                                .email(
                                        "customer-"
                                                + UUID.randomUUID()
                                                + "@test.com"
                                )
                                .password(
                                        passwordEncoder.encode(
                                                "Password@123"
                                        )
                                )
                                .phone("8888888888")
                                .roles(
                                        new HashSet<>(
                                                Set.of(customerRole)
                                        )
                                )
                                .build()
                );

        ownerToken =
                jwtService.generateToken(
                        new com.nexora.auth.security.UserPrincipal(
                                ownerOne
                        )
                );

        customerToken =
                jwtService.generateToken(
                        new com.nexora.auth.security.UserPrincipal(
                                customer
                        )
                );
    }

    // ============================================================
    // STORE SEARCH
    // ============================================================

    @Test
    @DisplayName("Search stores by name substring")
    @Transactional
    void searchStoresByName() throws Exception {

        String marker =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8);

        seedStore(
                ownerOne,
                "Rajkot Pizza Hub " + marker
        );

        seedStore(
                ownerOne,
                "Green Grocery " + marker
        );

        mockMvc.perform(
                        get("/api/v1/stores")
                                .header(
                                        "Authorization",
                                        "Bearer " + customerToken
                                )
                                .param(
                                        "name",
                                        "pizza"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.data.content.length()"
                        ).value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.data.content[0].name"
                        ).value(
                                "Rajkot Pizza Hub " + marker
                        )
                );
    }

    // ============================================================
    // HELPER
    // ============================================================

    private Store seedStore(
            User owner,
            String name) {

        Store store =
                Store.builder()
                        .owner(owner)
                        .name(name)
                        .description("Test store")
                        .imageUrl(
                                "https://example.com/store.jpg"
                        )
                        .openingTime(
                                LocalTime.of(10, 0)
                        )
                        .closingTime(
                                LocalTime.of(23, 0)
                        )
                        .status(StoreStatus.OPEN)
                        .categories(
                                new HashSet<>()
                        )
                        .build();

        return storeRepository.saveAndFlush(store);
    }
}