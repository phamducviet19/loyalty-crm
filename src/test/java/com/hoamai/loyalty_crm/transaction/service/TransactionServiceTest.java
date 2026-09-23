package com.hoamai.loyalty_crm.transaction.service;

import com.hoamai.loyalty_crm.customer.entity.Customer;
import com.hoamai.loyalty_crm.customer.repository.CustomerRepository;
import com.hoamai.loyalty_crm.loyalty.entity.PointTransaction;
import com.hoamai.loyalty_crm.loyalty.service.LoyaltyService;
import com.hoamai.loyalty_crm.store.entity.Store;
import com.hoamai.loyalty_crm.store.repository.StoreRepository;
import com.hoamai.loyalty_crm.transaction.dto.CreateTransactionItemRequest;
import com.hoamai.loyalty_crm.transaction.dto.CreateTransactionRequest;
import com.hoamai.loyalty_crm.transaction.dto.TransactionResponse;
import com.hoamai.loyalty_crm.transaction.entity.Transaction;
import com.hoamai.loyalty_crm.transaction.entity.TransactionStatus;
import com.hoamai.loyalty_crm.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private LoyaltyService loyaltyService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Customer customer;
    private Store store;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(UUID.randomUUID())
                .customerCode("CUST-001")
                .fullName("Customer One")
                .build();

        store = Store.builder()
                .id(UUID.randomUUID())
                .storeCode("STORE-001")
                .storeName("Main Store")
                .build();
    }

    @Test
    void createTransaction_success() {
        CreateTransactionItemRequest item = CreateTransactionItemRequest.builder()
                .productCode("PROD-1")
                .productName("Product 1")
                .quantity(2)
                .unitPrice(new BigDecimal("100000"))
                .build();

        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .transactionCode("POS-TX-001")
                .customerId(customer.getId())
                .storeId(store.getId())
                .items(List.of(item))
                .build();

        when(transactionRepository.findByTransactionCode("POS-TX-001")).thenReturn(Optional.empty());
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> {
            Transaction tx = i.getArgument(0);
            tx.setId(UUID.randomUUID());
            return tx;
        });

        PointTransaction pointTx = PointTransaction.builder().points(20L).build();
        when(loyaltyService.earnPointsForTransaction(any(Transaction.class))).thenReturn(pointTx);

        TransactionResponse response = transactionService.createTransaction(request);

        assertNotNull(response);
        assertEquals("POS-TX-001", response.getTransactionCode());
        assertEquals(new BigDecimal("200000"), response.getTotalAmount());
        assertEquals(20L, response.getPointsEarned());
        assertFalse(response.getIsDuplicateRequest());
        verify(loyaltyService, times(1)).earnPointsForTransaction(any(Transaction.class));
    }

    @Test
    void createTransaction_idempotencyDuplicate_returnsExistingWithoutReEarning() {
        Transaction existingTx = Transaction.builder()
                .id(UUID.randomUUID())
                .transactionCode("POS-TX-001")
                .customer(customer)
                .store(store)
                .totalAmount(new BigDecimal("200000"))
                .status(TransactionStatus.SUCCESS)
                .items(Collections.emptyList())
                .build();

        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .transactionCode("POS-TX-001")
                .customerId(customer.getId())
                .storeId(store.getId())
                .build();

        when(transactionRepository.findByTransactionCode("POS-TX-001")).thenReturn(Optional.of(existingTx));

        TransactionResponse response = transactionService.createTransaction(request);

        assertNotNull(response);
        assertEquals("POS-TX-001", response.getTransactionCode());
        assertTrue(response.getIsDuplicateRequest());
        verify(transactionRepository, never()).save(any());
        verify(loyaltyService, never()).earnPointsForTransaction(any());
    }
}
