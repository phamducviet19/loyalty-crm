package com.hoamai.loyalty_crm.store.service;

import com.hoamai.loyalty_crm.common.exception.DuplicateResourceException;
import com.hoamai.loyalty_crm.store.dto.CreateStoreRequest;
import com.hoamai.loyalty_crm.store.dto.StoreResponse;
import com.hoamai.loyalty_crm.store.entity.Store;
import com.hoamai.loyalty_crm.store.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private StoreServiceImpl storeService;

    private Store sampleStore;

    @BeforeEach
    void setUp() {
        sampleStore = Store.builder()
                .id(UUID.randomUUID())
                .storeCode("STORE-12345678")
                .storeName("Hoa Mai Central")
                .address("123 Main St")
                .build();
    }

    @Test
    void createStore_withProvidedCode_success() {
        CreateStoreRequest request = CreateStoreRequest.builder()
                .storeCode("STORE-001")
                .storeName("Hoa Mai Central")
                .address("123 Main St")
                .build();

        when(storeRepository.existsByStoreCode("STORE-001")).thenReturn(false);
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StoreResponse response = storeService.createStore(request);

        assertNotNull(response);
        assertEquals("STORE-001", response.getStoreCode());
        assertEquals("Hoa Mai Central", response.getStoreName());
    }

    @Test
    void createStore_withNullCode_autoGeneratesCode() {
        CreateStoreRequest request = CreateStoreRequest.builder()
                .storeCode(null)
                .storeName("Hoa Mai Branch 2")
                .address("456 Side St")
                .build();

        when(storeRepository.existsByStoreCode(anyString())).thenReturn(false);
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StoreResponse response = storeService.createStore(request);

        assertNotNull(response);
        assertNotNull(response.getStoreCode());
        assertTrue(response.getStoreCode().startsWith("STORE-"));
        assertEquals("Hoa Mai Branch 2", response.getStoreName());

        ArgumentCaptor<Store> captor = ArgumentCaptor.forClass(Store.class);
        verify(storeRepository).save(captor.capture());
        assertTrue(captor.getValue().getStoreCode().startsWith("STORE-"));
    }

    @Test
    void createStore_duplicateCode_throwsException() {
        CreateStoreRequest request = CreateStoreRequest.builder()
                .storeCode("STORE-001")
                .storeName("Hoa Mai Central")
                .address("123 Main St")
                .build();

        when(storeRepository.existsByStoreCode("STORE-001")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> storeService.createStore(request));
        verify(storeRepository, never()).save(any());
    }
}
