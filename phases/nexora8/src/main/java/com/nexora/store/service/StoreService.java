package com.nexora.store.service;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.category.entity.Category;
import com.nexora.category.repository.CategoryRepository;
import com.nexora.exception.ForbiddenOperationException;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.role.entity.RoleName;
import com.nexora.store.dto.StoreRequest;
import com.nexora.store.dto.StoreResponse;
import com.nexora.store.dto.StoreStatusRequest;
import com.nexora.store.entity.Store;
import com.nexora.store.entity.StoreStatus;
import com.nexora.store.mapper.StoreMapper;
import com.nexora.store.repository.StoreRepository;
import com.nexora.store.spec.StoreSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;

    // ============================================================
    // CREATE STORE
    // ============================================================

    @Transactional
    public StoreResponse createStore(
            StoreRequest request,
            UserPrincipal owner) {

        Set<Category> categories =
                resolveCategories(request.getCategoryNames());

        Store store = Store.builder()
                .owner(owner.getUser())
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .openingTime(request.getOpeningTime())
                .closingTime(request.getClosingTime())
                .categories(categories)
                .build();

        Store saved = storeRepository.save(store);

        return StoreMapper.toResponse(saved);
    }

    // ============================================================
    // GET ALL STORES
    // ============================================================

    @Transactional(readOnly = true)
    public Page<StoreResponse> getAllStores(
            Pageable pageable) {

        return searchStores(
                null,
                null,
                null,
                pageable
        );
    }

    // ============================================================
    // SEARCH / FILTER / SORT
    // ============================================================

    @Transactional(readOnly = true)
    public Page<StoreResponse> searchStores(
            String name,
            String categoryName,
            StoreStatus status,
            Pageable pageable) {

        Specification<Store> specification =
                Specification.allOf(
                        StoreSpecifications.nameContains(name),
                        StoreSpecifications.hasCategoryName(
                                categoryName
                        ),
                        StoreSpecifications.hasStatus(status)
                );

        return storeRepository
                .findAll(specification, pageable)
                .map(StoreMapper::toResponse);
    }

    // ============================================================
    // GET STORE BY ID
    // ============================================================

    @Transactional(readOnly = true)
    public StoreResponse getStoreById(Long id) {

        Store store = findStoreOrThrow(id);

        return StoreMapper.toResponse(store);
    }

    // ============================================================
    // GET CURRENT USER'S STORES
    // ============================================================

    @Transactional(readOnly = true)
    public List<StoreResponse> getStoresOwnedByCurrentUser(
            UserPrincipal principal) {

        return storeRepository
                .findByOwnerId(principal.getId())
                .stream()
                .map(StoreMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // UPDATE STORE
    // ============================================================

    @Transactional
    public StoreResponse updateStore(
            Long id,
            StoreRequest request,
            UserPrincipal caller) {

        Store store = findStoreOrThrow(id);

        assertCallerOwnsStoreOrIsAdmin(
                store,
                caller
        );

        store.setName(request.getName());
        store.setDescription(request.getDescription());
        store.setImageUrl(request.getImageUrl());
        store.setOpeningTime(request.getOpeningTime());
        store.setClosingTime(request.getClosingTime());

        store.setCategories(
                resolveCategories(
                        request.getCategoryNames()
                )
        );

        Store saved = storeRepository.save(store);

        return StoreMapper.toResponse(saved);
    }

    // ============================================================
    // UPDATE STORE STATUS
    // ============================================================

    @Transactional
    public StoreResponse updateStoreStatus(
            Long id,
            StoreStatusRequest request,
            UserPrincipal caller) {

        Store store = findStoreOrThrow(id);

        assertCallerOwnsStoreOrIsAdmin(
                store,
                caller
        );

        store.setStatus(request.getStatus());

        Store saved = storeRepository.save(store);

        return StoreMapper.toResponse(saved);
    }

    // ============================================================
    // INTERNAL HELPERS
    // ============================================================

    private Store findStoreOrThrow(Long id) {

        return storeRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Store not found with id: " + id
                        )
                );
    }

    /**
     * ADMIN can modify any store.
     *
     * STORE_OWNER can modify only their own store.
     */
    private void assertCallerOwnsStoreOrIsAdmin(
            Store store,
            UserPrincipal caller) {

        boolean isAdmin =
                caller.getUser()
                        .getRoles()
                        .stream()
                        .anyMatch(
                                role ->
                                        role.getName()
                                                == RoleName.ADMIN
                        );

        if (isAdmin) {
            return;
        }

        if (!store.getOwner()
                .getId()
                .equals(caller.getId())) {

            throw new ForbiddenOperationException(
                    "You do not have permission to modify a store you do not own."
            );
        }
    }

    /**
     * Resolve category names to Category entities.
     */
    private Set<Category> resolveCategories(
            Set<String> categoryNames) {

        if (categoryNames == null ||
                categoryNames.isEmpty()) {

            return new HashSet<>();
        }

        Set<Category> resolved =
                new HashSet<>();

        for (String name : categoryNames) {

            Category category =
                    categoryRepository
                            .findByName(name)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Category not found: "
                                                    + name
                                                    + " (an ADMIN must create it first)"
                                    )
                            );

            resolved.add(category);
        }

        return resolved;
    }
}