package com.hoamai.loyalty_crm.store.service;

import com.hoamai.loyalty_crm.store.dto.CreateStoreRequest;
import com.hoamai.loyalty_crm.store.dto.StoreResponse;

import java.util.List;
import java.util.UUID;

public interface StoreService {

    StoreResponse createStore(CreateStoreRequest request);

    StoreResponse getStoreById(UUID id);

    List<StoreResponse> getAllStores();
}
