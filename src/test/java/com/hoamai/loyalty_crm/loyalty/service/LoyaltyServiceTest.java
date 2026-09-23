package com.hoamai.loyalty_crm.loyalty.service;

import com.hoamai.loyalty_crm.customer.entity.Customer;
import com.hoamai.loyalty_crm.customer.repository.CustomerRepository;
import com.hoamai.loyalty_crm.loyalty.dto.AdjustPointRequest;
import com.hoamai.loyalty_crm.loyalty.dto.LoyaltyAccountDetailResponse;
import com.hoamai.loyalty_crm.loyalty.dto.PointTransactionResponse;
import com.hoamai.loyalty_crm.loyalty.entity.LoyaltyAccount;
import com.hoamai.loyalty_crm.loyalty.entity.LoyaltyTier;
import com.hoamai.loyalty_crm.loyalty.entity.PointTransaction;
import com.hoamai.loyalty_crm.loyalty.entity.PointTransactionType;
import com.hoamai.loyalty_crm.loyalty.repository.LoyaltyAccountRepository;
import com.hoamai.loyalty_crm.loyalty.repository.PointTransactionRepository;
import com.hoamai.loyalty_crm.transaction.entity.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoyaltyServiceTest {

    @Mock
    private LoyaltyAccountRepository loyaltyAccountRepository;

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private LoyaltyServiceImpl loyaltyService;

    private Customer customer;
    private LoyaltyAccount loyaltyAccount;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(UUID.randomUUID())
                .fullName("Test Customer")
                .phone("0987654321")
                .customerCode("CUST-999")
                .build();

        loyaltyAccount = LoyaltyAccount.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .pointBalance(800L) // Bronze tier
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void earnPointsForTransaction_bronzeTier_success() {
        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID())
                .transactionCode("TX-100")
                .customer(customer)
                .totalAmount(new BigDecimal("500000")) // 500,000 VND -> 50 base points (multiplier 1.0)
                .build();

        when(loyaltyAccountRepository.findByCustomerId(customer.getId())).thenReturn(Optional.of(loyaltyAccount));
        when(pointTransactionRepository.save(any(PointTransaction.class))).thenAnswer(i -> i.getArgument(0));

        PointTransaction pointTx = loyaltyService.earnPointsForTransaction(tx);

        assertNotNull(pointTx);
        assertEquals(50L, pointTx.getPoints());
        assertEquals(PointTransactionType.EARN, pointTx.getType());
        assertEquals(850L, loyaltyAccount.getPointBalance());
    }

    @Test
    void earnPointsForTransaction_goldTierMultiplier_success() {
        loyaltyAccount.setPointBalance(6000L); // Gold tier multiplier 1.25x

        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID())
                .transactionCode("TX-101")
                .customer(customer)
                .totalAmount(new BigDecimal("1000000")) // 1,000,000 VND -> 100 base points * 1.25 = 125 points
                .build();

        when(loyaltyAccountRepository.findByCustomerId(customer.getId())).thenReturn(Optional.of(loyaltyAccount));
        when(pointTransactionRepository.save(any(PointTransaction.class))).thenAnswer(i -> i.getArgument(0));

        PointTransaction pointTx = loyaltyService.earnPointsForTransaction(tx);

        assertNotNull(pointTx);
        assertEquals(125L, pointTx.getPoints());
        assertEquals(6125L, loyaltyAccount.getPointBalance());
    }

    @Test
    void getLoyaltyAccountDetail_segmentationTierCheck() {
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(loyaltyAccountRepository.findByCustomerId(customer.getId())).thenReturn(Optional.of(loyaltyAccount));

        LoyaltyAccountDetailResponse response = loyaltyService.getLoyaltyAccountDetail(customer.getId());

        assertNotNull(response);
        assertEquals(LoyaltyTier.BRONZE, response.getTier());
        assertEquals("Bronze", response.getTierName());
        assertEquals(LoyaltyTier.SILVER, response.getNextTier());
        assertEquals(200L, response.getPointsNeededForNextTier()); // 1000 - 800
    }

    @Test
    void adjustPoints_success() {
        AdjustPointRequest request = AdjustPointRequest.builder()
                .customerId(customer.getId())
                .points(200L)
                .type(PointTransactionType.ADJUSTMENT)
                .description("Bonus points")
                .build();

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(loyaltyAccountRepository.findByCustomerId(customer.getId())).thenReturn(Optional.of(loyaltyAccount));
        when(pointTransactionRepository.save(any(PointTransaction.class))).thenAnswer(i -> i.getArgument(0));

        PointTransactionResponse response = loyaltyService.adjustPoints(request);

        assertNotNull(response);
        assertEquals(200L, response.getPoints());
        assertEquals(1000L, loyaltyAccount.getPointBalance());
    }
}
