package com.hoamai.loyalty_crm.store.service;

import com.hoamai.loyalty_crm.common.exception.DuplicateResourceException;
import com.hoamai.loyalty_crm.common.exception.ResourceNotFoundException;
import com.hoamai.loyalty_crm.store.dto.CreateStoreRequest;
import com.hoamai.loyalty_crm.store.dto.StoreResponse;
import com.hoamai.loyalty_crm.store.entity.Store;
import com.hoamai.loyalty_crm.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;

    @Override
    @Transactional
    public StoreResponse createStore(CreateStoreRequest request) {
        if (storeRepository.existsByStoreCode(request.getStoreCode())) {
            throw new DuplicateResourceException("Store code already exists: " + request.getStoreCode());
        }

        Store store = Store.builder()
                .storeCode(request.getStoreCode())
                .storeName(request.getStoreName())
                .address(request.getAddress())
                .build();

        Store savedStore = storeRepository.save(store);
        return mapToResponse(savedStore);
    }

    @Override
    public StoreResponse getStoreById(UUID id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + id));
        return mapToResponse(store);
    }

    @Override
    public List<StoreResponse> getAllStores() {
        return storeRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private StoreResponse mapToResponse(Store store) {
        return StoreResponse.builder()
                .id(store.getId())
                .storeCode(store.getStoreCode())
                .storeName(store.getStoreName())
                .address(store.getAddress())
                .createdAt(store.getCreatedAt())
                .build();
    }
}
