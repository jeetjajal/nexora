package com.nexora.address.service;

import com.nexora.address.dto.AddressRequest;
import com.nexora.address.dto.AddressResponse;
import com.nexora.address.entity.Address;
import com.nexora.address.mapper.AddressMapper;
import com.nexora.address.repository.AddressRepository;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.exception.ForbiddenOperationException;
import com.nexora.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PHASE 8 PREREQUISITE: Phase 2 created the Address entity, repository,
 * DTO, and mapper as groundwork, but never a service or controller —
 * there was nothing yet that needed one. Checkout (this phase) is the
 * first real consumer: a customer must be able to save at least one
 * address before they can tell checkout where to deliver. This fills
 * exactly that gap, following the same ownership pattern Cart (Phase 7)
 * established: every operation resolves "the caller's own" addresses
 * from the JWT — there's no admin override here, since addresses are
 * personal data with no legitimate cross-user management need the way
 * ADMIN oversight of stores/products had.
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    @Transactional
    public AddressResponse createAddress(UserPrincipal caller, AddressRequest request) {
        Address address = Address.builder()
                .user(caller.getUser())
                .label(request.getLabel())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .country(request.getCountry() != null ? request.getCountry() : "India")
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .isDefault(request.isDefault())
                .build();

        Address saved = addressRepository.save(address);
        return AddressMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses(UserPrincipal caller) {
        return addressRepository.findByUserId(caller.getId()).stream()
                .map(AddressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AddressResponse getAddressById(UserPrincipal caller, Long addressId) {
        Address address = findAddressOrThrow(addressId);
        assertOwnedByCaller(address, caller);
        return AddressMapper.toResponse(address);
    }

    @Transactional
    public AddressResponse updateAddress(UserPrincipal caller, Long addressId, AddressRequest request) {
        Address address = findAddressOrThrow(addressId);
        assertOwnedByCaller(address, caller);

        address.setLabel(request.getLabel());
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setCountry(request.getCountry() != null ? request.getCountry() : "India");
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setDefault(request.isDefault());

        Address saved = addressRepository.save(address);
        return AddressMapper.toResponse(saved);
    }

    @Transactional
    public void deleteAddress(UserPrincipal caller, Long addressId) {
        Address address = findAddressOrThrow(addressId);
        assertOwnedByCaller(address, caller);
        addressRepository.delete(address);
    }

    // ------------------------------------------------------------
    // Internal helpers — findOwnedAddressOrThrow is public so
    // OrderService (Phase 8, a different package) can reuse the same
    // ownership-checked lookup during checkout, instead of
    // duplicating this exact check.
    // ------------------------------------------------------------

    public Address findOwnedAddressOrThrow(UserPrincipal caller, Long addressId) {
        Address address = findAddressOrThrow(addressId);
        assertOwnedByCaller(address, caller);
        return address;
    }

    private Address findAddressOrThrow(Long addressId) {
        return addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));
    }

    private void assertOwnedByCaller(Address address, UserPrincipal caller) {
        if (!address.getUser().getId().equals(caller.getId())) {
            throw new ForbiddenOperationException("This address does not belong to you.");
        }
    }
}
