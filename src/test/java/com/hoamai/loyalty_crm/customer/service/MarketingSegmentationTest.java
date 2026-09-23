package com.hoamai.loyalty_crm.customer.service;

import com.hoamai.loyalty_crm.customer.dto.CustomerSegmentResponse;
import com.hoamai.loyalty_crm.customer.entity.Customer;
import com.hoamai.loyalty_crm.customer.repository.CustomerRepository;
import com.hoamai.loyalty_crm.loyalty.entity.LoyaltyAccount;
import com.hoamai.loyalty_crm.loyalty.entity.LoyaltyTier;
import com.hoamai.loyalty_crm.loyalty.repository.LoyaltyAccountRepository;
import com.hoamai.loyalty_crm.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketingSegmentationTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private LoyaltyAccountRepository loyaltyAccountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private Customer customer;
    private LoyaltyAccount loyaltyAccount;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(UUID.randomUUID())
                .customerCode("CUST-GOLD")
                .fullName("Gold Customer")
                .phone("0900000001")
                .build();

        loyaltyAccount = LoyaltyAccount.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .pointBalance(6000L) // Gold Tier
                .build();
    }

    @Test
    void getSegmentedCustomers_byGoldTier_success() {
        when(customerRepository.findCustomersByPointsRange(eq(5000L), eq(19999L), any()))
                .thenReturn(new PageImpl<>(List.of(customer)));
        when(loyaltyAccountRepository.findByCustomerId(customer.getId()))
                .thenReturn(Optional.of(loyaltyAccount));
        when(transactionRepository.sumTotalAmountByCustomerId(customer.getId()))
                .thenReturn(new BigDecimal("15000000"));
        when(transactionRepository.countByCustomerId(customer.getId()))
                .thenReturn(12L);

        Page<CustomerSegmentResponse> page = customerService.getSegmentedCustomers(
                LoyaltyTier.GOLD, null, null, PageRequest.of(0, 10));

        assertNotNull(page);
        assertEquals(1, page.getContent().size());
        CustomerSegmentResponse resp = page.getContent().get(0);
        assertEquals(LoyaltyTier.GOLD, resp.getTier());
        assertEquals("Gold", resp.getTierName());
        assertEquals(6000L, resp.getPointBalance());
        assertEquals(new BigDecimal("15000000"), resp.getTotalSpend());
    }
}
