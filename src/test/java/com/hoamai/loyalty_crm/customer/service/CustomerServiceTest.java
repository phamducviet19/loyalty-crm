package com.hoamai.loyalty_crm.customer.service;

import com.hoamai.loyalty_crm.common.exception.DuplicateResourceException;
import com.hoamai.loyalty_crm.common.exception.ResourceNotFoundException;
import com.hoamai.loyalty_crm.customer.dto.*;
import com.hoamai.loyalty_crm.customer.entity.Customer;
import com.hoamai.loyalty_crm.customer.repository.CustomerRepository;
import com.hoamai.loyalty_crm.loyalty.entity.LoyaltyAccount;
import com.hoamai.loyalty_crm.loyalty.repository.LoyaltyAccountRepository;
import com.hoamai.loyalty_crm.loyalty.repository.PointTransactionRepository;
import com.hoamai.loyalty_crm.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private LoyaltyAccountRepository loyaltyAccountRepository;

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private UUID customerId;
    private Customer sampleCustomer;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        sampleCustomer = Customer.builder()
                .id(customerId)
                .customerCode("CUST-123456")
                .fullName("Nguyen Van A")
                .phone("0912345678")
                .email("nva@example.com")
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .gender("MALE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void registerCustomer_success() {
        RegisterCustomerRequest request = RegisterCustomerRequest.builder()
                .fullName("Nguyen Van A")
                .phone("0912345678")
                .email("nva@example.com")
                .build();

        when(customerRepository.existsByPhone(request.getPhone())).thenReturn(false);
        when(customerRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

        CustomerResponse response = customerService.registerCustomer(request);

        assertNotNull(response);
        assertEquals("0912345678", response.getPhone());
        assertEquals("Nguyen Van A", response.getFullName());
        verify(loyaltyAccountRepository, times(1)).save(any(LoyaltyAccount.class));
    }

    @Test
    void registerCustomer_duplicatePhone_throwsException() {
        RegisterCustomerRequest request = RegisterCustomerRequest.builder()
                .fullName("Nguyen Van A")
                .phone("0912345678")
                .build();

        when(customerRepository.existsByPhone(request.getPhone())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> customerService.registerCustomer(request));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void identifyCustomer_byPhone_success() {
        LoyaltyAccount loyaltyAccount = LoyaltyAccount.builder()
                .id(UUID.randomUUID())
                .customer(sampleCustomer)
                .pointBalance(500L)
                .build();

        when(customerRepository.findByPhoneOrCustomerCode("0912345678")).thenReturn(Optional.of(sampleCustomer));
        when(loyaltyAccountRepository.findByCustomerId(customerId)).thenReturn(Optional.of(loyaltyAccount));

        CustomerIdentifyResponse response = customerService.identifyCustomer("0912345678");

        assertNotNull(response);
        assertEquals(customerId, response.getId());
        assertEquals(500L, response.getPointBalance());
    }

    @Test
    void updateCustomer_success() {
        UpdateCustomerRequest updateRequest = UpdateCustomerRequest.builder()
                .fullName("Nguyen Van B")
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

        CustomerResponse response = customerService.updateCustomer(customerId, updateRequest);

        assertNotNull(response);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void getCustomer360_success() {
        LoyaltyAccount loyaltyAccount = LoyaltyAccount.builder()
                .id(UUID.randomUUID())
                .customer(sampleCustomer)
                .pointBalance(1200L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));
        when(loyaltyAccountRepository.findByCustomerId(customerId)).thenReturn(Optional.of(loyaltyAccount));
        when(transactionRepository.sumTotalAmountByCustomerId(customerId)).thenReturn(BigDecimal.valueOf(2500000));
        when(transactionRepository.countByCustomerId(customerId)).thenReturn(5L);
        when(pointTransactionRepository.findByCustomerIdOrderByCreatedAtDesc(eq(customerId), any(Pageable.class)))
                .thenReturn(Collections.emptyList());
        when(transactionRepository.findByCustomerIdOrderByTransactionDateDesc(eq(customerId), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        Customer360Response response = customerService.getCustomer360(customerId);

        assertNotNull(response);
        assertEquals("Nguyen Van A", response.getProfile().getFullName());
        assertEquals(1200L, response.getLoyaltyAccount().getPointBalance());
        assertEquals(BigDecimal.valueOf(2500000), response.getMetrics().getTotalSpendAmount());
        assertEquals(5L, response.getMetrics().getTotalTransactionCount());
    }
}
