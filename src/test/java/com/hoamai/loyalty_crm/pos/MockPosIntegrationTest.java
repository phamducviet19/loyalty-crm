package com.hoamai.loyalty_crm.pos;

import com.hoamai.loyalty_crm.common.dto.ApiResponse;
import com.hoamai.loyalty_crm.customer.entity.Customer;
import com.hoamai.loyalty_crm.customer.repository.CustomerRepository;
import com.hoamai.loyalty_crm.loyalty.repository.LoyaltyAccountRepository;
import com.hoamai.loyalty_crm.pos.controller.MockPosController;
import com.hoamai.loyalty_crm.pos.dto.MockPosCheckoutRequest;
import com.hoamai.loyalty_crm.pos.dto.MockPosCheckoutResponse;
import com.hoamai.loyalty_crm.store.entity.Store;
import com.hoamai.loyalty_crm.store.repository.StoreRepository;
import com.hoamai.loyalty_crm.transaction.dto.CreateTransactionItemRequest;
import com.hoamai.loyalty_crm.transaction.dto.TransactionResponse;
import com.hoamai.loyalty_crm.transaction.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockPosIntegrationTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private LoyaltyAccountRepository loyaltyAccountRepository;

    @InjectMocks
    private MockPosController mockPosController;

    private Store store;
    private Customer customer;

    @BeforeEach
    void setUp() {
        store = Store.builder()
                .id(UUID.randomUUID())
                .storeCode("STORE-01")
                .storeName("Hoa Mai Store 1")
                .build();

        customer = Customer.builder()
                .id(UUID.randomUUID())
                .customerCode("CUST-100")
                .fullName("POS Customer")
                .phone("0912345678")
                .build();
    }

    @Test
    void mockPosCheckout_success() {
        MockPosCheckoutRequest request = MockPosCheckoutRequest.builder()
                .posDeviceId("POS-DEV-1")
                .storeCode("STORE-01")
                .customerPhoneOrCode("0912345678")
                .posTransactionId("POS-TX-999")
                .items(List.of(CreateTransactionItemRequest.builder()
                        .productCode("P1")
                        .productName("Item 1")
                        .quantity(1)
                        .unitPrice(new BigDecimal("100000"))
                        .build()))
                .build();

        TransactionResponse txResponse = TransactionResponse.builder()
                .transactionCode("POS-TX-999")
                .totalAmount(new BigDecimal("100000"))
                .pointsEarned(10L)
                .isDuplicateRequest(false)
                .items(Collections.emptyList())
                .build();

        when(storeRepository.findByStoreCode("STORE-01")).thenReturn(Optional.of(store));
        when(customerRepository.findByPhoneOrCustomerCode("0912345678")).thenReturn(Optional.of(customer));
        when(transactionService.createTransaction(any())).thenReturn(txResponse);

        ResponseEntity<ApiResponse<MockPosCheckoutResponse>> responseEntity = mockPosController.mockPosCheckout(request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertEquals("POS-TX-999", responseEntity.getBody().getData().getTransactionCode());
        assertEquals(10L, responseEntity.getBody().getData().getPointsEarned());
        assertFalse(responseEntity.getBody().getData().getIsDuplicatePosTransaction());
    }
}
