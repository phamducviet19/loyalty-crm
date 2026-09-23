package com.hoamai.loyalty_crm.loyalty.service;

import com.hoamai.loyalty_crm.common.exception.ResourceNotFoundException;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoyaltyServiceImpl implements LoyaltyService {

    private static final BigDecimal VND_PER_POINT = new BigDecimal("10000");

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public PointTransaction earnPointsForTransaction(Transaction transaction) {
        Customer customer = transaction.getCustomer();

        LoyaltyAccount loyaltyAccount = loyaltyAccountRepository.findByCustomerId(customer.getId())
                .orElseGet(() -> {
                    LoyaltyAccount account = LoyaltyAccount.builder()
                            .customer(customer)
                            .pointBalance(0L)
                            .build();
                    return loyaltyAccountRepository.save(account);
                });

        LoyaltyTier currentTier = LoyaltyTier.fromPoints(loyaltyAccount.getPointBalance());

        // Base points: 1 point per 10,000 VND
        BigDecimal basePointsDecimal = transaction.getTotalAmount().divideToIntegralValue(VND_PER_POINT);
        long basePoints = basePointsDecimal.longValue();

        if (basePoints <= 0) {
            return null;
        }

        // Apply tier multiplier
        long calculatedPoints = Math.round(basePoints * currentTier.getPointMultiplier());

        // Update balance
        loyaltyAccount.setPointBalance(loyaltyAccount.getPointBalance() + calculatedPoints);
        loyaltyAccountRepository.save(loyaltyAccount);

        // Record point transaction
        PointTransaction pointTransaction = PointTransaction.builder()
                .customer(customer)
                .transaction(transaction)
                .type(PointTransactionType.EARN)
                .points(calculatedPoints)
                .description("Earn points for transaction " + transaction.getTransactionCode())
                .build();

        return pointTransactionRepository.save(pointTransaction);
    }

    @Override
    @Transactional
    public PointTransactionResponse adjustPoints(AdjustPointRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        LoyaltyAccount loyaltyAccount = loyaltyAccountRepository.findByCustomerId(customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Loyalty account not found for customer id: " + request.getCustomerId()));

        long updatedBalance = loyaltyAccount.getPointBalance() + request.getPoints();
        if (updatedBalance < 0) {
            throw new IllegalArgumentException("Insufficient point balance. Current: " + loyaltyAccount.getPointBalance() + ", Adjustment: " + request.getPoints());
        }

        loyaltyAccount.setPointBalance(updatedBalance);
        loyaltyAccountRepository.save(loyaltyAccount);

        PointTransaction pointTransaction = PointTransaction.builder()
                .customer(customer)
                .type(request.getType())
                .points(request.getPoints())
                .description(request.getDescription() != null ? request.getDescription() : "Manual point adjustment")
                .build();

        PointTransaction savedTx = pointTransactionRepository.save(pointTransaction);
        return mapToPointTransactionResponse(savedTx);
    }

    @Override
    public LoyaltyAccountDetailResponse getLoyaltyAccountDetail(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        LoyaltyAccount loyaltyAccount = loyaltyAccountRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Loyalty account not found for customer id: " + customerId));

        long currentPoints = loyaltyAccount.getPointBalance();
        LoyaltyTier currentTier = LoyaltyTier.fromPoints(currentPoints);
        LoyaltyTier nextTier = currentTier.getNextTier();
        Long pointsNeeded = currentTier.getPointsNeededForNextTier(currentPoints);

        return LoyaltyAccountDetailResponse.builder()
                .id(loyaltyAccount.getId())
                .customerId(customer.getId())
                .customerCode(customer.getCustomerCode())
                .customerName(customer.getFullName())
                .pointBalance(currentPoints)
                .tier(currentTier)
                .tierName(currentTier.getDisplayName())
                .nextTier(nextTier != currentTier ? nextTier : null)
                .pointsNeededForNextTier(pointsNeeded)
                .pointMultiplier(currentTier.getPointMultiplier())
                .createdAt(loyaltyAccount.getCreatedAt())
                .updatedAt(loyaltyAccount.getUpdatedAt())
                .build();
    }

    @Override
    public Page<PointTransactionResponse> getPointHistory(UUID customerId, PointTransactionType type, Pageable pageable) {
        if (customerId != null && !customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }

        return pointTransactionRepository.findPointTransactions(customerId, type, pageable)
                .map(this::mapToPointTransactionResponse);
    }

    private PointTransactionResponse mapToPointTransactionResponse(PointTransaction pt) {
        return PointTransactionResponse.builder()
                .id(pt.getId())
                .customerId(pt.getCustomer().getId())
                .customerName(pt.getCustomer().getFullName())
                .transactionId(pt.getTransaction() != null ? pt.getTransaction().getId() : null)
                .type(pt.getType())
                .points(pt.getPoints())
                .description(pt.getDescription())
                .createdAt(pt.getCreatedAt())
                .build();
    }
}
